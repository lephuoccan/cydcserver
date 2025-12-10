package cloud.cydc.websocket;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketSession {
    // userId -> Set of subscribed deviceIds
    private final Map<String, Set<String>> subscriptions = new ConcurrentHashMap<>();

    public void subscribe(String userId, String deviceId) {
        subscriptions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(deviceId);
    }

    public void unsubscribe(String userId, String deviceId) {
        Set<String> devices = subscriptions.get(userId);
        if (devices != null) {
            devices.remove(deviceId);
            if (devices.isEmpty()) {
                subscriptions.remove(userId);
            }
        }
    }

    public boolean isSubscribed(String userId, String deviceId) {
        Set<String> devices = subscriptions.get(userId);
        return devices != null && devices.contains(deviceId);
    }

    public Set<String> getSubscribedDevices(String userId) {
        Set<String> devices = subscriptions.get(userId);
        return devices != null ? new HashSet<>(devices) : Collections.emptySet();
    }

    public boolean hasSubscriptions(String userId) {
        return subscriptions.containsKey(userId) && !subscriptions.get(userId).isEmpty();
    }

    public void clear() {
        subscriptions.clear();
    }
}
