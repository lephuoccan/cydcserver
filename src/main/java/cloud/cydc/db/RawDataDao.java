package cloud.cydc.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RawDataDao {
    private static final Logger log = LoggerFactory.getLogger(RawDataDao.class);
    private final DataSource ds;
    private final ObjectMapper mapper = new ObjectMapper();

    public RawDataDao(DataSource ds) {
        this.ds = ds;
    }

    /**
     * Insert raw data point: userId, dashId, devId, pin, value, timestamp.
     */
    public void insert(String userId, long dashId, long devId, String pin, String value, long ts) {
        String sql = "INSERT INTO raw_data (userid, dashid, devid, pin, value, ts) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setLong(2, dashId);
            ps.setLong(3, devId);
            ps.setString(4, pin);
            ps.setString(5, value);
            ps.setLong(6, ts);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error inserting raw data", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Query raw data for a device and pin within a time range.
     * Returns JSON array of [value, timestamp, pin] objects.
     */
    public String queryData(String userId, long dashId, long devId, String pin, long startTs, long endTs) {
        String sql = "SELECT value, ts FROM raw_data WHERE userid = ? AND dashid = ? AND devid = ? AND pin = ? AND ts >= ? AND ts <= ? ORDER BY ts DESC";
        List<Map<String, Object>> data = new ArrayList<>();

        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setLong(2, dashId);
            ps.setLong(3, devId);
            ps.setString(4, pin);
            ps.setLong(5, startTs);
            ps.setLong(6, endTs);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("value", rs.getString("value"));
                row.put("ts", rs.getLong("ts"));
                row.put("pin", pin);
                data.add(row);
            }
            rs.close();
        } catch (SQLException e) {
            log.error("Error querying raw data", e);
            throw new RuntimeException(e);
        }

        try {
            return mapper.writeValueAsString(data);
        } catch (Exception e) {
            log.error("Error serializing raw data", e);
            return "[]";
        }
    }

    /**
     * Delete raw data older than a given timestamp (for cleanup).
     */
    public int deleteOlderThan(long ts) {
        String sql = "DELETE FROM raw_data WHERE ts < ?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, ts);
            return ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error deleting old raw data", e);
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Delete old raw data but keep the latest record for each device/pin combination.
     * This ensures we always have at least one record per device/pin.
     */
    public int deleteOlderThanKeepLatest(long ts) {
        // Delete old records except the most recent one for each devid/pin combination
        String sql = "DELETE FROM raw_data " +
                     "WHERE ts < ? " +
                     "AND id NOT IN (" +
                     "  SELECT MAX(id) FROM raw_data GROUP BY devid, pin" +
                     ")";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, ts);
            return ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error deleting old raw data with keep latest", e);
            throw new RuntimeException(e);
        }
    }
}
