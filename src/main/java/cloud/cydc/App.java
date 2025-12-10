package cloud.cydc;

import cloud.cydc.cache.RedisClientManager;
import cloud.cydc.config.Config;
import cloud.cydc.db.DashboardDao;
import cloud.cydc.db.DeviceDao;
import cloud.cydc.db.DeviceInfoDao;
import cloud.cydc.db.PostgresDataSource;
import cloud.cydc.db.RawDataDao;
import cloud.cydc.db.UsersDao;
import cloud.cydc.netty.NettyServer;
import cloud.cydc.service.DashboardService;
import cloud.cydc.service.DeviceInfoService;
import cloud.cydc.service.DeviceService;
import cloud.cydc.service.RawDataService;
import cloud.cydc.service.UsersService;
import cloud.cydc.service.VirtualPinService;
import cloud.cydc.http.HttpServer;
import cloud.cydc.websocket.WebSocketServer;
import cloud.cydc.websocket.WebSocketFrameHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

public class App 
{
    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main( String[] args ) throws Exception
    {
        Thread.setDefaultUncaughtExceptionHandler((t, e) ->
            log.error("Uncaught exception on thread {}", t.getName(), e));
        CountDownLatch shutdownLatch = new CountDownLatch(1);
        Config cfg = new Config();

        // init database
        PostgresDataSource.init(cfg);
        DeviceDao dao = new DeviceDao(PostgresDataSource.getDataSource());

        // init redis
        RedisClientManager.init(cfg);

        // services
        DeviceService deviceService = new DeviceService(dao);

        // Initialize database schema with Blynk-compatible tables and raw data support
        initializeDatabase(cfg);

        int port = Integer.parseInt(cfg.get("server.port", "8080"));
        NettyServer server = new NettyServer(port, deviceService);
        server.start();

        // start HTTP API
        int httpPort = Integer.parseInt(cfg.get("server.http.port", "8081"));
        var usersDao = new UsersDao(PostgresDataSource.getDataSource());
        var usersService = new UsersService(usersDao);
        var dashboardDao = new DashboardDao(PostgresDataSource.getDataSource());
        var dashboardService = new DashboardService(dashboardDao);
        var deviceInfoDao = new DeviceInfoDao(PostgresDataSource.getDataSource());
        var deviceInfoService = new DeviceInfoService(deviceInfoDao);
        
        // Initialize raw data service
        boolean enableRawData = Boolean.parseBoolean(cfg.get("enable.raw.data.store", "false"));
        RawDataService rawDataService = null;
        if (enableRawData) {
            RawDataDao rawDataDao = new RawDataDao(PostgresDataSource.getDataSource());
            rawDataService = new RawDataService(rawDataDao, true);
            log.info("Raw data storage enabled");
        }
        
        var pinService = new VirtualPinService(rawDataService);
        var httpServer = new HttpServer(httpPort, usersService, dashboardService, deviceInfoService, pinService);
        httpServer.start();

        // start WebSocket server for app clients (with heartbeat and pin updates)
        int wsPort = Integer.parseInt(cfg.get("server.websocket.port", "9001"));
        WebSocketFrameHandler wsFrameHandler = new WebSocketFrameHandler();
        WebSocketServer wsServer = new WebSocketServer(wsPort, null, wsFrameHandler); // null = no SSL for now
        wsServer.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown hook triggered, stopping servers...");
            try {
                // Force sync pin data trước khi shutdown
                pinService.shutdown();
                
                server.stop();
                httpServer.stop();
                wsServer.stop();
                RedisClientManager.close();
                PostgresDataSource.close();
            } catch (Exception e) {
                log.error("Error during shutdown", e);
            } finally {
                shutdownLatch.countDown();
            }
        }, "cydc-shutdown"));

        System.out.println("CYDConnect Server running on port " + port + " (http:" + httpPort + ", websocket:" + wsPort + ")");
        log.info("Servers started (tcp:{} http:{} ws:{})", port, httpPort, wsPort);
        shutdownLatch.await();
    }

    /**
     * Initialize database with Blynk-compatible schema.
     * Creates all necessary tables for users, dashboards, devices, and raw data.
     */
    private static void initializeDatabase(Config cfg) {
        try (var c = PostgresDataSource.getDataSource().getConnection();
             var s = c.createStatement()) {
            
            // Create users table (Blynk compatible)
            s.execute("CREATE TABLE IF NOT EXISTS users (" +
                "id TEXT PRIMARY KEY, name TEXT, email TEXT NOT NULL UNIQUE, appname TEXT, " +
                "region TEXT, ip TEXT, pass TEXT NOT NULL, lastmodifiedts BIGINT DEFAULT 0, " +
                "lastloggedip TEXT, lastloggedat BIGINT DEFAULT 0, profile JSONB DEFAULT '{}'::jsonb, " +
                "isfacebookuser BOOLEAN DEFAULT false, issuperadmin BOOLEAN DEFAULT false, " +
                "energy BIGINT DEFAULT 0, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // Create dashboards table
            s.execute("CREATE TABLE IF NOT EXISTS dashboards (" +
                "userid TEXT NOT NULL, dashid BIGINT NOT NULL, data JSONB NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (userid, dashid), " +
                "FOREIGN KEY (userid) REFERENCES users(id) ON DELETE CASCADE)");

            // Create device_info table (Blynk compatible)
            s.execute("CREATE TABLE IF NOT EXISTS device_info (" +
                "userid TEXT NOT NULL, dashid BIGINT NOT NULL, devid BIGINT NOT NULL, " +
                "token TEXT NOT NULL, data JSONB NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (userid, dashid, devid), " +
                "FOREIGN KEY (userid) REFERENCES users(id) ON DELETE CASCADE)");

            // Create raw_data table for storing virtual pin writes (optional)
            s.execute("CREATE TABLE IF NOT EXISTS raw_data (" +
                "id SERIAL PRIMARY KEY, userid TEXT NOT NULL, dashid BIGINT NOT NULL, " +
                "devid BIGINT NOT NULL, pin TEXT NOT NULL, value TEXT NOT NULL, " +
                "ts BIGINT NOT NULL DEFAULT " + System.currentTimeMillis() + ", " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (userid) REFERENCES users(id) ON DELETE CASCADE)");

            // Create indexes for raw_data
            s.execute("CREATE INDEX IF NOT EXISTS idx_raw_data_userid_dashid_devid " +
                "ON raw_data(userid, dashid, devid)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_raw_data_ts ON raw_data(ts)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_raw_data_pin ON raw_data(pin)");

            // Create simple devices table
            s.execute("CREATE TABLE IF NOT EXISTS devices (" +
                "id TEXT PRIMARY KEY, name TEXT, meta TEXT)");

            // Create sessions table for tracking connections
            s.execute("CREATE TABLE IF NOT EXISTS sessions (" +
                "id SERIAL PRIMARY KEY, userid TEXT NOT NULL, dashid BIGINT NOT NULL, " +
                "devid BIGINT NOT NULL, session_token TEXT UNIQUE NOT NULL, " +
                "session_type TEXT NOT NULL, ip_address TEXT, " +
                "connected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "last_ping_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (userid) REFERENCES users(id) ON DELETE CASCADE)");

            s.execute("CREATE INDEX IF NOT EXISTS idx_sessions_userid ON sessions(userid)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_sessions_session_token ON sessions(session_token)");

            log.info("Database schema initialized successfully");
        } catch (Exception e) {
            log.error("Error initializing database schema", e);
            throw new RuntimeException(e);
        }
    }
}
