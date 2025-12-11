package cloud.cydc.service;

import cloud.cydc.blynk.BlynkProtocolHandler;
import cloud.cydc.cache.RedisClientManager;
import cloud.cydc.websocket.WebSocketFrameHandler;
import io.lettuce.core.api.sync.RedisCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

public class VirtualPinService {
    private static final Logger log = LoggerFactory.getLogger(VirtualPinService.class);
    private static final String PIN_KEY_PREFIX = "pin:";
    private static final String DEVICE_ACTIVITY_PREFIX = "device:activity:";
    private static final int DEVICE_INACTIVE_THRESHOLD_SEC = 15; // 5 minutes
    private static final int DEVICE_CLEANUP_THRESHOLD_SEC = 60; // 30 minutes
    private static final int SYNC_BATCH_SIZE = 100;
    private static final long DATA_RETENTION_DAYS = 90; // 3 months = 90 days
    private static final long DATA_RETENTION_MS = DATA_RETENTION_DAYS * 24 * 60 * 60 * 1000L;
    
    private final RawDataService rawDataService;
    private final ScheduledExecutorService scheduler;
    private final ConcurrentHashMap<Long, Queue<PinUpdate>> pendingWrites;
    
    private static class PinUpdate {
        final String userId;
        final long dashId;
        final long devId;
        final int pinNum;
        final String value;
        final long timestamp;
        
        PinUpdate(String userId, long dashId, long devId, int pinNum, String value) {
            this.userId = userId;
            this.dashId = dashId;
            this.devId = devId;
            this.pinNum = pinNum;
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public VirtualPinService(RawDataService rawDataService) {
        this.rawDataService = rawDataService;
        this.pendingWrites = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(2);
        
        // Định kỳ sync dữ liệu từ Redis vào DB (mỗi 30s)
        scheduler.scheduleAtFixedRate(this::syncPendingWritesToDB, 30, 30, TimeUnit.SECONDS);
        
        // Định kỳ cleanup các device không hoạt động (mỗi 5 phút)
        scheduler.scheduleAtFixedRate(this::cleanupInactiveDevices, 300, 300, TimeUnit.SECONDS);
        
        // Định kỳ xóa dữ liệu pin cũ hơn 3 tháng (mỗi ngày lúc 2:00 AM)
        scheduler.scheduleAtFixedRate(this::cleanupOldPinData, calculateInitialDelay(), 24 * 60 * 60, TimeUnit.SECONDS);
        
        log.info("VirtualPinService initialized with auto-sync, cleanup, and 90-day data retention");
    }

    public VirtualPinService() {
        this(null); // For backward compatibility
    }
    
    /**
     * Cập nhật activity timestamp của device
     */
    private void touchDeviceActivity(long deviceId) {
        String key = DEVICE_ACTIVITY_PREFIX + deviceId;
        RedisCommands<String, String> sync = RedisClientManager.sync();
        sync.set(key, String.valueOf(System.currentTimeMillis()));
        sync.expire(key, DEVICE_CLEANUP_THRESHOLD_SEC);
    }
    
    /**
     * Kiểm tra device có active không (trong 5 phút gần đây)
     */
    private boolean isDeviceActive(long deviceId) {
        String key = DEVICE_ACTIVITY_PREFIX + deviceId;
        String lastActivityStr = RedisClientManager.sync().get(key);
        if (lastActivityStr == null) return false;
        
        long lastActivity = Long.parseLong(lastActivityStr);
        long now = System.currentTimeMillis();
        return (now - lastActivity) < (DEVICE_INACTIVE_THRESHOLD_SEC * 1000);
    }

    public void setPinValue(long deviceId, int pinNum, String value) {
        String key = PIN_KEY_PREFIX + deviceId + ":V" + pinNum;
        RedisCommands<String, String> sync = RedisClientManager.sync();
        sync.set(key, value);
        
        // Set TTL dựa trên activity: active = 24h, inactive = 30 phút
        int ttl = isDeviceActive(deviceId) ? 86400 : DEVICE_CLEANUP_THRESHOLD_SEC;
        sync.expire(key, ttl);
        
        touchDeviceActivity(deviceId);
    }

    public void setPinValueWithBroadcast(String userId, String deviceId, int pinNum, String value) {
        long devId = Long.parseLong(deviceId);
        setPinValue(devId, pinNum, value);
        
        // Broadcast pin update to all subscribed WebSocket clients
        WebSocketFrameHandler.broadcastPinUpdate(userId, deviceId, "V" + pinNum, value);
        
        // Push to connected ESP32 via Blynk protocol
        BlynkProtocolHandler.sendHardwareCommand(devId, pinNum, value);
        
        // Queue để sync vào DB sau (batch write)
        if (rawDataService != null && rawDataService.isEnabled()) {
            PinUpdate update = new PinUpdate(userId, 0, devId, pinNum, value);
            pendingWrites.computeIfAbsent(devId, k -> new ConcurrentLinkedQueue<>()).offer(update);
            log.debug("Queued pin update for DB sync: device={}, pin=V{}, value={}", devId, pinNum, value);
        }
        
        touchDeviceActivity(devId);
    }

    public void setPinValueWithBroadcastAndRawData(String userId, long dashId, long devId, int pinNum, String value) {
        // Ghi vào Redis ngay lập tức
        setPinValue(devId, pinNum, value);
        
        // Broadcast đến WebSocket clients
        WebSocketFrameHandler.broadcastPinUpdate(userId, String.valueOf(devId), "V" + pinNum, value);
        
        // Queue để sync vào DB sau (batch write)
        if (rawDataService != null && rawDataService.isEnabled()) {
            PinUpdate update = new PinUpdate(userId, dashId, devId, pinNum, value);
            pendingWrites.computeIfAbsent(devId, k -> new ConcurrentLinkedQueue<>()).offer(update);
        }
        
        touchDeviceActivity(devId);
    }
    
    /**
     * Sync batch các pin updates vào database
     */
    private void syncPendingWritesToDB() {
        if (rawDataService == null || !rawDataService.isEnabled()) return;
        
        try {
            int totalSynced = 0;
            List<Long> emptyDevices = new ArrayList<>();
            
            log.info("Starting pin data sync, pending devices: {}", pendingWrites.size());
            
            for (Map.Entry<Long, Queue<PinUpdate>> entry : pendingWrites.entrySet()) {
                Queue<PinUpdate> queue = entry.getValue();
                if (queue.isEmpty()) {
                    emptyDevices.add(entry.getKey());
                    continue;
                }
                
                log.info("Syncing device {} with {} pending updates", entry.getKey(), queue.size());
                
                // Batch sync tối đa SYNC_BATCH_SIZE records mỗi device
                int count = 0;
                PinUpdate update;
                while ((update = queue.poll()) != null && count < SYNC_BATCH_SIZE) {
                    try {
                        rawDataService.storeRawData(
                            update.userId, 
                            update.dashId, 
                            update.devId, 
                            "V" + update.pinNum, 
                            update.value
                        );
                        totalSynced++;
                        count++;
                        log.debug("Stored to DB: device={}, pin=V{}, value={}", update.devId, update.pinNum, update.value);
                    } catch (Exception e) {
                        log.error("Failed to sync pin data for device {}: {}", update.devId, e.getMessage(), e);
                    }
                }
            }
            
            // Cleanup empty queues
            emptyDevices.forEach(pendingWrites::remove);
            
            if (totalSynced > 0) {
                log.info("Successfully synced {} pin updates to database", totalSynced);
            }
        } catch (Exception e) {
            log.error("Error during pin data sync: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Cleanup dữ liệu của các device không hoạt động
     */
    private void cleanupInactiveDevices() {
        try {
            RedisCommands<String, String> sync = RedisClientManager.sync();
            List<String> activityKeys = sync.keys(DEVICE_ACTIVITY_PREFIX + "*");
            
            int cleaned = 0;
            long now = System.currentTimeMillis();
            
            for (String activityKey : activityKeys) {
                String lastActivityStr = sync.get(activityKey);
                if (lastActivityStr == null) continue;
                
                long lastActivity = Long.parseLong(lastActivityStr);
                long inactiveDuration = (now - lastActivity) / 1000; // seconds
                
                // Nếu device không hoạt động quá 30 phút, xóa tất cả pin data
                if (inactiveDuration > DEVICE_CLEANUP_THRESHOLD_SEC) {
                    String deviceIdStr = activityKey.substring(DEVICE_ACTIVITY_PREFIX.length());
                    long deviceId = Long.parseLong(deviceIdStr);
                    
                    // Xóa tất cả pin keys của device này
                    List<String> pinKeys = sync.keys(PIN_KEY_PREFIX + deviceId + ":*");
                    if (!pinKeys.isEmpty()) {
                        sync.del(pinKeys.toArray(new String[0]));
                        cleaned += pinKeys.size();
                    }
                    
                    // Xóa activity key
                    sync.del(activityKey);
                    
                    // Xóa pending writes queue
                    pendingWrites.remove(deviceId);
                    
                    log.info("Cleaned up {} pin keys for inactive device {}", pinKeys.size(), deviceId);
                }
            }
            
            if (cleaned > 0) {
                log.info("Total cleaned {} keys from inactive devices", cleaned);
            }
        } catch (Exception e) {
            log.error("Error during device cleanup: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Calculate initial delay to run cleanup at 2:00 AM
     * @return seconds until next 2:00 AM
     */
    private long calculateInitialDelay() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime nextRun = now.withHour(2).withMinute(0).withSecond(0).withNano(0);
        
        // If already past 2:00 AM today, schedule for tomorrow
        if (now.compareTo(nextRun) > 0) {
            nextRun = nextRun.plusDays(1);
        }
        
        long delay = java.time.Duration.between(now, nextRun).getSeconds();
        log.info("Old pin data cleanup scheduled to run at 2:00 AM (initial delay: {} seconds / {} hours)", 
                 delay, delay / 3600);
        return delay;
    }
    
    /**
     * Delete pin data older than 3 months from database
     */
    private void cleanupOldPinData() {
        if (rawDataService == null || !rawDataService.isEnabled()) {
            return;
        }
        
        try {
            long cutoffTime = System.currentTimeMillis() - DATA_RETENTION_MS;
            log.info("Starting cleanup of pin data older than {} days (before timestamp: {})", 
                     DATA_RETENTION_DAYS, cutoffTime);
            
            int deleted = rawDataService.deleteOldData(cutoffTime);
            
            if (deleted > 0) {
                log.info("Deleted {} old pin data records (older than {} days)", deleted, DATA_RETENTION_DAYS);
            } else {
                log.debug("No old pin data to delete");
            }
        } catch (Exception e) {
            log.error("Error during old pin data cleanup", e);
        }
    }

    public String getPinValue(long deviceId, int pinNum) {
        String key = PIN_KEY_PREFIX + deviceId + ":V" + pinNum;
        touchDeviceActivity(deviceId);
        return RedisClientManager.sync().get(key);
    }

    public void deletePinValue(long deviceId, int pinNum) {
        String key = PIN_KEY_PREFIX + deviceId + ":V" + pinNum;
        RedisClientManager.sync().del(key);
    }
    
    /**
     * Xóa tất cả pin data của device (khi device bị xóa)
     */
    public void deleteAllPins(long deviceId) {
        RedisCommands<String, String> sync = RedisClientManager.sync();
        List<String> keys = sync.keys(PIN_KEY_PREFIX + deviceId + ":*");
        if (!keys.isEmpty()) {
            sync.del(keys.toArray(new String[0]));
        }
        sync.del(DEVICE_ACTIVITY_PREFIX + deviceId);
        pendingWrites.remove(deviceId);
        log.info("Deleted all pin data for device {}", deviceId);
    }

    public String getAllPinsJson(long deviceId) {
        StringBuilder sb = new StringBuilder("{");
        touchDeviceActivity(deviceId);
        
        for (int i = 0; i < 128; i++) {
            String val = getPinValue(deviceId, i);
            if (val != null) {
                if (sb.length() > 1) sb.append(",");
                sb.append("\"V").append(i).append("\":\"").append(val).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }
    
    /**
     * Force sync ngay lập tức (để gọi khi shutdown)
     */
    public void forceSync() {
        log.info("Force syncing all pending pin updates...");
        syncPendingWritesToDB();
    }
    
    /**
     * Shutdown service và sync dữ liệu còn lại
     */
    public void shutdown() {
        log.info("Shutting down VirtualPinService...");
        forceSync();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
        log.info("VirtualPinService shutdown complete");
    }
    
    /**
     * Lấy thống kê
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("pendingDevices", pendingWrites.size());
        
        int totalPending = 0;
        for (Queue<PinUpdate> queue : pendingWrites.values()) {
            totalPending += queue.size();
        }
        stats.put("totalPendingWrites", totalPending);
        
        try {
            RedisCommands<String, String> sync = RedisClientManager.sync();
            List<String> activityKeys = sync.keys(DEVICE_ACTIVITY_PREFIX + "*");
            stats.put("activeDevices", activityKeys.size());
            
            List<String> pinKeys = sync.keys(PIN_KEY_PREFIX + "*");
            stats.put("totalPinKeys", pinKeys.size());
        } catch (Exception e) {
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }
}
