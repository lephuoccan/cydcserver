package cloud.cydc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class Dashboard {
    private final long id;
    private final long parentId;
    private final boolean isPreview;
    private final String name;
    private final long createdAt;
    private final long updatedAt;
    private final List<Map<String, Object>> widgets;
    private final List<Map<String, Object>> devices;
    private final String theme;
    private final boolean keepScreenOn;
    private final boolean isAppConnectedOn;
    private final boolean isNotificationsOff;
    private final boolean isShared;
    private final boolean isActive;
    private final boolean widgetBackgroundOn;
    private final long color;
    private final boolean isDefaultColor;
    private final String sharedToken;

    @JsonCreator
    public Dashboard(
        @JsonProperty("id") long id,
        @JsonProperty("parentId") long parentId,
        @JsonProperty("isPreview") boolean isPreview,
        @JsonProperty("name") String name,
        @JsonProperty("createdAt") long createdAt,
        @JsonProperty("updatedAt") long updatedAt,
        @JsonProperty("widgets") List<Map<String, Object>> widgets,
        @JsonProperty("devices") List<Map<String, Object>> devices,
        @JsonProperty("theme") String theme,
        @JsonProperty("keepScreenOn") boolean keepScreenOn,
        @JsonProperty("isAppConnectedOn") boolean isAppConnectedOn,
        @JsonProperty("isNotificationsOff") boolean isNotificationsOff,
        @JsonProperty("isShared") boolean isShared,
        @JsonProperty("isActive") boolean isActive,
        @JsonProperty("widgetBackgroundOn") boolean widgetBackgroundOn,
        @JsonProperty("color") long color,
        @JsonProperty("isDefaultColor") boolean isDefaultColor,
        @JsonProperty("sharedToken") String sharedToken) {
        this.id = id;
        this.parentId = parentId;
        this.isPreview = isPreview;
        this.name = name;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.widgets = widgets == null ? List.of() : widgets;
        this.devices = devices == null ? List.of() : devices;
        this.theme = theme;
        this.keepScreenOn = keepScreenOn;
        this.isAppConnectedOn = isAppConnectedOn;
        this.isNotificationsOff = isNotificationsOff;
        this.isShared = isShared;
        this.isActive = isActive;
        this.widgetBackgroundOn = widgetBackgroundOn;
        this.color = color;
        this.isDefaultColor = isDefaultColor;
        this.sharedToken = sharedToken;
    }

    public long getId() { return id; }
    public long getParentId() { return parentId; }
    public boolean isPreview() { return isPreview; }
    public String getName() { return name; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public List<Map<String, Object>> getWidgets() { return widgets; }
    public List<Map<String, Object>> getDevices() { return devices; }
    public String getTheme() { return theme; }
    public boolean isKeepScreenOn() { return keepScreenOn; }
    public boolean isAppConnectedOn() { return isAppConnectedOn; }
    public boolean isNotificationsOff() { return isNotificationsOff; }
    public boolean isShared() { return isShared; }
    public boolean isActive() { return isActive; }
    public boolean isWidgetBackgroundOn() { return widgetBackgroundOn; }
    public long getColor() { return color; }
    public boolean isDefaultColor() { return isDefaultColor; }
    public String getSharedToken() { return sharedToken; }
}
