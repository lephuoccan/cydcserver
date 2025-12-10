package cloud.cydc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class DeviceInfo {
    private final long id;
    private final String name;
    private final String boardType;
    private final String token;
    private final String vendor;
    private final String connectionType;
    private final String status;
    private final long disconnectTime;
    private final long connectTime;
    private final long firstConnectTime;
    private final long dataReceivedAt;
    private final String lastLoggedIP;
    private final Map<String, Object> hardwareInfo;
    private final boolean isUserIcon;

    @JsonCreator
    public DeviceInfo(
        @JsonProperty("id") long id,
        @JsonProperty("name") String name,
        @JsonProperty("boardType") String boardType,
        @JsonProperty("token") String token,
        @JsonProperty("vendor") String vendor,
        @JsonProperty("connectionType") String connectionType,
        @JsonProperty("status") String status,
        @JsonProperty("disconnectTime") long disconnectTime,
        @JsonProperty("connectTime") long connectTime,
        @JsonProperty("firstConnectTime") long firstConnectTime,
        @JsonProperty("dataReceivedAt") long dataReceivedAt,
        @JsonProperty("lastLoggedIP") String lastLoggedIP,
        @JsonProperty("hardwareInfo") Map<String, Object> hardwareInfo,
        @JsonProperty("userIcon") boolean isUserIcon) {
        this.id = id;
        this.name = name;
        this.boardType = boardType;
        this.token = token;
        this.vendor = vendor;
        this.connectionType = connectionType;
        this.status = status;
        this.disconnectTime = disconnectTime;
        this.connectTime = connectTime;
        this.firstConnectTime = firstConnectTime;
        this.dataReceivedAt = dataReceivedAt;
        this.lastLoggedIP = lastLoggedIP;
        this.hardwareInfo = hardwareInfo == null ? Map.of() : hardwareInfo;
        this.isUserIcon = isUserIcon;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public String getBoardType() { return boardType; }
    public String getToken() { return token; }
    public String getVendor() { return vendor; }
    public String getConnectionType() { return connectionType; }
    public String getStatus() { return status; }
    public long getDisconnectTime() { return disconnectTime; }
    public long getConnectTime() { return connectTime; }
    public long getFirstConnectTime() { return firstConnectTime; }
    public long getDataReceivedAt() { return dataReceivedAt; }
    public String getLastLoggedIP() { return lastLoggedIP; }
    public Map<String, Object> getHardwareInfo() { return hardwareInfo; }
    @JsonProperty("userIcon")
    public boolean isUserIcon() { return isUserIcon; }
}
