package cloud.cydc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class VirtualPin {
    private final int pin;
    private final String value;
    private final long timestamp;

    @JsonCreator
    public VirtualPin(@JsonProperty("pin") int pin,
                      @JsonProperty("value") String value,
                      @JsonProperty("timestamp") long timestamp) {
        this.pin = pin;
        this.value = value == null ? "0" : value;
        this.timestamp = timestamp;
    }

    public int getPin() { return pin; }
    public String getValue() { return value; }
    public long getTimestamp() { return timestamp; }
}
