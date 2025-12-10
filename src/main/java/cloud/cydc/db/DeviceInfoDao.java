package cloud.cydc.db;

import cloud.cydc.model.DeviceInfo;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DeviceInfoDao {
    private final DataSource ds;

    public DeviceInfoDao(DataSource ds) {
        this.ds = ds;
    }

    public void upsert(String userId, long dashId, DeviceInfo d, String dataJson) {
        String sql = "INSERT INTO device_info (userid, dashid, devid, token, data) VALUES (?, ?, ?, ?, ?::jsonb) ON CONFLICT (userid, dashid, devid) DO UPDATE SET token = EXCLUDED.token, data = EXCLUDED.data";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setLong(2, dashId);
            ps.setLong(3, d.getId());
            ps.setString(4, d.getToken());
            ps.setString(5, dataJson);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String findJsonById(String userId, long dashId, long devId) {
        String sql = "SELECT data FROM device_info WHERE userid = ? AND dashid = ? AND devid = ?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setLong(2, dashId);
            ps.setLong(3, devId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String json = rs.getString(1);
                rs.close();
                return json;
            }
            rs.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public String findTokenByDevId(long devId) {
        String sql = "SELECT token FROM device_info WHERE devid = ?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, devId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String token = rs.getString(1);
                rs.close();
                return token;
            }
            rs.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public boolean deleteById(String userId, long dashId, long devId) {
        String sql = "DELETE FROM device_info WHERE userid = ? AND dashid = ? AND devid = ?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setLong(2, dashId);
            ps.setLong(3, devId);
            int r = ps.executeUpdate();
            return r > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
