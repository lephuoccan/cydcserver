package cloud.cydc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class User {
    private final String name;
    private final String email;
    private final String appName;
    private final String region;
    private final String ip;
    private final String pass;
    private final long lastModifiedTs;
    private final String lastLoggedIP;
    private final long lastLoggedAt;
    private final Map<String, Object> profile;
    private final boolean isFacebookUser;
    private final boolean isSuperAdmin;
    private final long energy;
    private final String id;

    @JsonCreator
    public User(@JsonProperty("name") String name,
                @JsonProperty("email") String email,
                @JsonProperty("appName") String appName,
                @JsonProperty("region") String region,
                @JsonProperty("ip") String ip,
                @JsonProperty("pass") String pass,
                @JsonProperty("lastModifiedTs") long lastModifiedTs,
                @JsonProperty("lastLoggedIP") String lastLoggedIP,
                @JsonProperty("lastLoggedAt") long lastLoggedAt,
                @JsonProperty("profile") Map<String, Object> profile,
                @JsonProperty("isFacebookUser") boolean isFacebookUser,
                @JsonProperty("isSuperAdmin") boolean isSuperAdmin,
                @JsonProperty("energy") long energy,
                @JsonProperty("id") String id) {
        this.name = name;
        this.email = email;
        this.appName = appName;
        this.region = region;
        this.ip = ip;
        this.pass = pass;
        this.lastModifiedTs = lastModifiedTs;
        this.lastLoggedIP = lastLoggedIP;
        this.lastLoggedAt = lastLoggedAt;
        this.profile = profile == null ? Map.of() : profile;
        this.isFacebookUser = isFacebookUser;
        this.isSuperAdmin = isSuperAdmin;
        this.energy = energy;
        this.id = id;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getAppName() { return appName; }
    public String getRegion() { return region; }
    public String getIp() { return ip; }
    public String getPass() { return pass; }
    public long getLastModifiedTs() { return lastModifiedTs; }
    public String getLastLoggedIP() { return lastLoggedIP; }
    public long getLastLoggedAt() { return lastLoggedAt; }
    public Map<String, Object> getProfile() { return profile; }
    public boolean isFacebookUser() { return isFacebookUser; }
    public boolean isSuperAdmin() { return isSuperAdmin; }
    public long getEnergy() { return energy; }
    public String getId() { return id; }
}
