package cloud.cydc.cache;

import cloud.cydc.config.Config;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

public final class RedisClientManager {
    private static RedisClient client;
    private static StatefulRedisConnection<String, String> conn;

    public static void init(Config cfg) {
        String uri = cfg.get("redis.uri", "redis://localhost:6379");
        client = RedisClient.create(uri);
        conn = client.connect();
    }

    public static RedisCommands<String, String> sync() {
        return conn.sync();
    }

    public static void close() {
        if (conn != null) conn.close();
        if (client != null) client.shutdown();
    }
}
