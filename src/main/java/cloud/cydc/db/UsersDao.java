package cloud.cydc.db;

import cloud.cydc.model.User;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UsersDao {
    private final DataSource ds;

    public UsersDao(DataSource ds) {
        this.ds = ds;
    }

    public boolean existsById(String id) {
        String sql = "SELECT 1 FROM users WHERE id = ? LIMIT 1";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            boolean exists = rs.next();
            rs.close();
            return exists;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void upsert(User u, String profileJson) {
        String sql = "INSERT INTO users (id, name, email, appname, region, ip, pass, lastmodifiedts, lastloggedip, " +
                            "lastloggedat, profile, isfacebookuser, issuperadmin, energy) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?) " +
                            "ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, email = EXCLUDED.email, " +
                            "appname = EXCLUDED.appname, region = EXCLUDED.region, ip = EXCLUDED.ip, " +
                            "pass = EXCLUDED.pass, lastmodifiedts = EXCLUDED.lastmodifiedts, " +
                            "lastloggedip = EXCLUDED.lastloggedip, lastloggedat = EXCLUDED.lastloggedat, " +
                            "profile = EXCLUDED.profile, isfacebookuser = EXCLUDED.isfacebookuser, " +
                            "issuperadmin = EXCLUDED.issuperadmin, energy = EXCLUDED.energy";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u.getId());
            ps.setString(2, u.getName());
            ps.setString(3, u.getEmail());
            ps.setString(4, u.getAppName());
            ps.setString(5, u.getRegion());
            ps.setString(6, u.getIp());
            ps.setString(7, u.getPass());
            ps.setLong(8, u.getLastModifiedTs());
            ps.setString(9, u.getLastLoggedIP());
            ps.setLong(10, u.getLastLoggedAt());
            ps.setString(11, profileJson == null ? "{}" : profileJson);
            ps.setBoolean(12, u.isFacebookUser());
            ps.setBoolean(13, u.isSuperAdmin());
            ps.setLong(14, u.getEnergy());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String findJsonById(String id) {
        String sql = "SELECT row_to_json(u) FROM (SELECT id, name, email, appname as \"appName\", region, ip, pass, lastmodifiedts as \"lastModifiedTs\", lastloggedip as \"lastLoggedIP\", lastloggedat as \"lastLoggedAt\", profile, isfacebookuser as \"isFacebookUser\", issuperadmin as \"isSuperAdmin\", energy FROM users WHERE id = ?) u";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
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

    public boolean deleteById(String id) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            int r = ps.executeUpdate();
            return r > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
