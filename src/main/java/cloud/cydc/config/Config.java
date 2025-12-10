package cloud.cydc.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Simple configuration helper. Reads from environment variables with defaults.
 */
public final class Config {
    private final Properties props = new Properties();

    public Config() {
        // load defaults from application.properties on classpath (if present)
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            // ignore, will use env defaults
        }
        // defaults
        props.setProperty("server.port", System.getenv().getOrDefault("CYDC_SERVER_PORT", props.getProperty("server.port", "8080")));
        props.setProperty("server.http.port", System.getenv().getOrDefault("CYDC_SERVER_HTTP_PORT", props.getProperty("server.http.port", "8081")));
        props.setProperty("db.url", System.getenv().getOrDefault("CYDC_DB_URL", props.getProperty("db.url", "jdbc:postgresql://localhost:5432/cydc")));
        props.setProperty("db.user", System.getenv().getOrDefault("CYDC_DB_USER", props.getProperty("db.user", "postgres")));
        props.setProperty("db.pass", System.getenv().getOrDefault("CYDC_DB_PASS", props.getProperty("db.pass", "postgres")));
        props.setProperty("redis.uri", System.getenv().getOrDefault("CYDC_REDIS_URI", props.getProperty("redis.uri", "redis://localhost:6379")));
    }

    public String get(String key, String fallback) {
        return props.getProperty(key, fallback);
    }

    public String get(String key) {
        return props.getProperty(key);
    }
}
