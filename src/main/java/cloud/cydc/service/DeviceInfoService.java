package cloud.cydc.service;

import cloud.cydc.cache.RedisClientManager;
import cloud.cydc.db.DeviceInfoDao;
import cloud.cydc.model.DeviceInfo;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;

public class DeviceInfoService {
    private final DeviceInfoDao dao;
    private final ObjectMapper mapper = new ObjectMapper();

    public DeviceInfoService(DeviceInfoDao dao) {
        this.dao = dao;
    }

    public void createOrUpdate(String userId, long dashId, DeviceInfo d) {
        try {
            System.out.println("[DeviceInfoService] createOrUpdate - devId: " + d.getId() + ", token: " + d.getToken());
            String json = mapper.writeValueAsString(d);
            System.out.println("[DeviceInfoService] JSON: " + json);
            dao.upsert(userId, dashId, d, json);
            RedisClientManager.sync().set("device:" + userId + ":" + dashId + ":" + d.getId(), json);
        } catch (Exception e) {
            System.out.println("[DeviceInfoService] Exception: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public String findJsonById(String userId, long dashId, long devId) {
        try {
            String cached = RedisClientManager.sync().get("device:" + userId + ":" + dashId + ":" + devId);
            if (cached != null) return cached;
        } catch (Exception e) {
            // ignore
        }
        return dao.findJsonById(userId, dashId, devId);
    }

    public String generateNewToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public boolean delete(String userId, long dashId, long devId) {
        RedisClientManager.sync().del("device:" + userId + ":" + dashId + ":" + devId);
        // also delete all pins for this device
        for (int i = 0; i < 128; i++) {
            RedisClientManager.sync().del("pin:" + devId + ":V" + i);
        }
        return dao.deleteById(userId, dashId, devId);
    }

    public String getToken(long devId) {
        return dao.findTokenByDevId(devId);
    }

    public long getNextDeviceId(String userId, long dashId) {
        return dao.getNextDeviceId(userId, dashId);
    }

    public String findTokenByDeviceId(String userId, long dashId, long devId) {
        System.out.println("[DeviceInfoService] findTokenByDeviceId - userId: " + userId + ", dashId: " + dashId + ", devId: " + devId);
        String json = findJsonById(userId, dashId, devId);
        System.out.println("[DeviceInfoService] Found JSON: " + (json != null ? json.substring(0, Math.min(100, json.length())) : "null"));
        if (json == null) return null;
        try {
            DeviceInfo d = mapper.readValue(json, DeviceInfo.class);
            System.out.println("[DeviceInfoService] Parsed token: " + d.getToken());
            return d.getToken();
        } catch (Exception e) {
            System.out.println("[DeviceInfoService] Exception parsing JSON: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
