package cloud.cydc.service;

import cloud.cydc.cache.RedisClientManager;
import cloud.cydc.db.DashboardDao;
import cloud.cydc.model.Dashboard;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class DashboardService {
    private final DashboardDao dao;
    private final ObjectMapper mapper = new ObjectMapper();

    public DashboardService(DashboardDao dao) {
        this.dao = dao;
    }

    public void createOrUpdate(String userId, Dashboard d) {
        try {
            String json = mapper.writeValueAsString(d);
            dao.upsert(userId, d, json);
            RedisClientManager.sync().set("dashboard:" + userId + ":" + d.getId(), json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String findJsonById(String userId, long dashId) {
        try {
            String cached = RedisClientManager.sync().get("dashboard:" + userId + ":" + dashId);
            if (cached != null) return cached;
        } catch (Exception e) {
            // ignore
        }
        return dao.findJsonById(userId, dashId);
    }

    public List<String> findAllJson(String userId) {
        return dao.findAllJsonByUserId(userId);
    }

    public boolean delete(String userId, long dashId) {
        RedisClientManager.sync().del("dashboard:" + userId + ":" + dashId);
        return dao.deleteById(userId, dashId);
    }
}
