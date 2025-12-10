package cloud.cydc.db;

import cloud.cydc.model.Device;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DeviceDao {
    private final DataSource ds;

    public DeviceDao(DataSource ds) {
        this.ds = ds;
    }

    public void upsert(Device d) {
        try (Connection c = ds.getConnection()) {
            PreparedStatement ps = c.prepareStatement("INSERT INTO devices (id, name, meta) VALUES (?, ?, ?) ON CONFLICT (id) DO UPDATE SET name=EXCLUDED.name, meta=EXCLUDED.meta");
            ps.setString(1, d.getId());
            ps.setString(2, d.getName());
            ps.setString(3, d.getMeta());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Device findById(String id) {
        try (Connection c = ds.getConnection()) {
            PreparedStatement ps = c.prepareStatement("SELECT id, name, meta FROM devices WHERE id = ?");
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Device d = new Device(rs.getString(1), rs.getString(2), rs.getString(3));
                rs.close();
                ps.close();
                return d;
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
