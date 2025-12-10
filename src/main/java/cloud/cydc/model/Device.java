package cloud.cydc.model;

public class Device {
    private final String id;
    private final String name;
    private final String meta;

    public Device(String id, String name, String meta) {
        this.id = id;
        this.name = name;
        this.meta = meta;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getMeta() {
        return meta;
    }
}
