package cloud.cydc.service;

import cloud.cydc.cache.RedisClientManager;
import cloud.cydc.db.DeviceDao;
import cloud.cydc.model.Device;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DeviceService {
    private final DeviceDao dao;
    private final ObjectMapper mapper = new ObjectMapper();

    public DeviceService(DeviceDao dao) {
        this.dao = dao;
    }

    public void register(Device d) {
        dao.upsert(d);
        // cache basic record in redis
        try {
            String json = mapper.writeValueAsString(d);
            RedisClientManager.sync().set("device:" + d.getId(), json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Device get(String id) {
        try {
            String cached = RedisClientManager.sync().get("device:" + id);
            if (cached != null) {
                return mapper.readValue(cached, Device.class);
            }
        } catch (Exception e) {
            // ignore and fallback to db
        }
        return dao.findById(id);
    }
}
