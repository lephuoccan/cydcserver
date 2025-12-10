package cloud.cydc.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import cloud.cydc.config.Config;

import javax.sql.DataSource;

public final class PostgresDataSource {
    private static HikariDataSource ds;

    public static void init(Config cfg) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(cfg.get("db.url", "jdbc:postgresql://localhost:5432/cydc"));
        config.setUsername(cfg.get("db.user", "postgres"));
        config.setPassword(cfg.get("db.pass", "postgres"));
        config.setMaximumPoolSize(10);
        ds = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return ds;
    }

    public static void close() {
        if (ds != null) ds.close();
    }
}
