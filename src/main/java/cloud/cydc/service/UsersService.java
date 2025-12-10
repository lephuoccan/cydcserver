package cloud.cydc.service;

import cloud.cydc.db.UsersDao;
import cloud.cydc.model.User;
import cloud.cydc.util.PasswordHasher;
import com.fasterxml.jackson.databind.ObjectMapper;

public class UsersService {
    private final UsersDao dao;
    private final ObjectMapper mapper = new ObjectMapper();

    public UsersService(UsersDao dao) {
        this.dao = dao;
    }

    /**
     * Register a new user with email, password, and optional appName.
     * Returns the generated user ID (email-appName format).
     * Password is hashed using bcrypt before storage.
     * Throws exception if user already exists.
     */
    public String register(String email, String password, String appName) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (appName == null || appName.trim().isEmpty()) {
            appName = "Blynk";
        }
        
        String userId = email + "-" + appName;
        
        if (dao.existsById(userId)) {
            throw new IllegalArgumentException("User already exists");
        }

        long now = System.currentTimeMillis();
        String hashedPassword = PasswordHasher.hashPassword(password);
        User u = new User(email, email, appName, "local", "127.0.0.1", hashedPassword, 
            now, "127.0.0.1", now, null, false, false, 1000000000L, userId);
        
        try {
            String profileJson = mapper.writeValueAsString(u.getProfile());
            dao.upsert(u, profileJson);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        return userId;
    }

    public void register(User u) {
        try {
            String profileJson = mapper.writeValueAsString(u.getProfile());
            dao.upsert(u, profileJson);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String findJsonById(String id) {
        return dao.findJsonById(id);
    }

    public boolean delete(String id) {
        return dao.deleteById(id);
    }

    public boolean checkLogin(String id, String plainTextPassword) {
        String json = dao.findJsonById(id);
        if (json == null) return false;
        try {
            User u = mapper.readValue(json, User.class);
            String storedHash = u.getPass();
            if (storedHash == null) return false;
            // Verify plain-text password against bcrypt hash
            return PasswordHasher.verifyPassword(plainTextPassword, storedHash);
        } catch (Exception e) {
            return false;
        }
    }
}
