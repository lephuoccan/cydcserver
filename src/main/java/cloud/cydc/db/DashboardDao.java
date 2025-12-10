package cloud.cydc.db;

import cloud.cydc.model.Dashboard;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DashboardDao {
    private final DataSource ds;
    private final ObjectMapper mapper = new ObjectMapper();

    public DashboardDao(DataSource ds) {
        this.ds = ds;
    }

    public void upsert(String userId, Dashboard d, String dataJson) {
        String sql = "INSERT INTO dashboards (userid, dashid, data) VALUES (?, ?, ?::jsonb) ON CONFLICT (userid, dashid) DO UPDATE SET data = EXCLUDED.data";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setLong(2, d.getId());
            ps.setString(3, dataJson);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String findJsonById(String userId, long dashId) {
        String sql = "SELECT data FROM dashboards WHERE userid = ? AND dashid = ?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setLong(2, dashId);
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

    public List<String> findAllJsonByUserId(String userId) {
        String sql = "SELECT data FROM dashboards WHERE userid = ?";
        List<String> results = new ArrayList<>();
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                results.add(rs.getString(1));
            }
            rs.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return results;
    }

    public boolean deleteById(String userId, long dashId) {
        String sql = "DELETE FROM dashboards WHERE userid = ? AND dashid = ?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setLong(2, dashId);
            int r = ps.executeUpdate();
            return r > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
