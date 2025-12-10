package cloud.cydc.service;

import cloud.cydc.db.RawDataDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RawDataService {
    private static final Logger log = LoggerFactory.getLogger(RawDataService.class);
    private final RawDataDao dao;
    private final boolean enabled;

    public RawDataService(RawDataDao dao, boolean enabled) {
        this.dao = dao;
        this.enabled = enabled;
    }

    /**
     * Store a raw data point for a virtual pin write.
     * Stores userId, dashId, devId, pin name, value, and timestamp.
     */
    public void storeRawData(String userId, long dashId, long devId, String pin, String value) {
        if (!enabled) return;

        try {
            long ts = System.currentTimeMillis();
            dao.insert(userId, dashId, devId, pin, value, ts);
            log.debug("Raw data stored: {}:{}: {} = {} at {}", dashId, devId, pin, value, ts);
        } catch (Exception e) {
            log.error("Error storing raw data", e);
        }
    }

    /**
     * Retrieve raw data for a device within a time range.
     * Returns JSON array of [value, timestamp, pin] tuples.
     */
    public String getRawData(String userId, long dashId, long devId, String pin, long startTs, long endTs) {
        if (!enabled) return "[]";

        try {
            return dao.queryData(userId, dashId, devId, pin, startTs, endTs);
        } catch (Exception e) {
            log.error("Error retrieving raw data", e);
            return "[]";
        }
    }

    public boolean isEnabled() {
        return enabled;
    }
}
