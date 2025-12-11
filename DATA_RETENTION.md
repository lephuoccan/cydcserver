# Data Retention Policy

## Overview
Hệ thống tự động xóa dữ liệu pin cũ hơn **90 ngày (3 tháng)** để duy trì hiệu suất database.

## Configuration

### Retention Period
```java
// VirtualPinService.java
private static final long DATA_RETENTION_DAYS = 90; // 3 months
```

Để thay đổi thời gian lưu trữ:
- 30 ngày (1 tháng): `DATA_RETENTION_DAYS = 30`
- 180 ngày (6 tháng): `DATA_RETENTION_DAYS = 180`
- 365 ngày (1 năm): `DATA_RETENTION_DAYS = 365`

### Cleanup Schedule
Cleanup chạy **mỗi ngày lúc 2:00 AM** (giờ server).

Thời gian này được tính tự động khi khởi động:
```java
scheduler.scheduleAtFixedRate(this::cleanupOldPinData, 
    calculateInitialDelay(),  // Initial delay đến 2:00 AM tiếp theo
    24 * 60 * 60,            // Lặp lại mỗi 24 giờ
    TimeUnit.SECONDS);
```

## How It Works

### 1. Startup
```
[INFO] Old pin data cleanup scheduled to run at 2:00 AM 
       (initial delay: 34200 seconds / 9.5 hours)
[INFO] VirtualPinService initialized with auto-sync, cleanup, 
       and 90-day data retention
```

### 2. Daily Execution (2:00 AM)
```
[INFO] Starting cleanup of pin data older than 90 days 
       (before timestamp: 1757670000000)
[INFO] Deleted 1234 old pin data records (older than 90 days)
```

### 3. No Data to Delete
```
[DEBUG] No old pin data to delete
```

## Database Impact

### Query Executed
```sql
DELETE FROM raw_data WHERE ts < ?
-- ? = (current_time_ms - 90_days_in_ms)
```

### Example
- Current time: 2025-12-11 02:00:00 (1765446000000 ms)
- Cutoff time: 2025-09-12 02:00:00 (1757670000000 ms)
- Action: DELETE all records with `ts < 1757670000000`

## Monitoring

### Check Cleanup Logs
```bash
grep "cleanup of pin data" cydcserver.log
grep "Deleted.*old pin data" cydcserver.log
```

### Verify Data Retention
```sql
-- Count records by age
SELECT 
  CASE 
    WHEN ts > EXTRACT(EPOCH FROM NOW() - INTERVAL '30 days') * 1000 THEN '< 30 days'
    WHEN ts > EXTRACT(EPOCH FROM NOW() - INTERVAL '60 days') * 1000 THEN '30-60 days'
    WHEN ts > EXTRACT(EPOCH FROM NOW() - INTERVAL '90 days') * 1000 THEN '60-90 days'
    ELSE '> 90 days (should be 0)'
  END AS age_group,
  COUNT(*) AS record_count
FROM raw_data
GROUP BY age_group
ORDER BY age_group;
```

### Check Oldest Record
```sql
SELECT 
  pin, 
  value, 
  to_timestamp(ts/1000) AS timestamp,
  NOW() - to_timestamp(ts/1000) AS age
FROM raw_data 
ORDER BY ts ASC 
LIMIT 1;
```

## Manual Cleanup

### Force Cleanup Now (via code)
```java
VirtualPinService service = // get instance
service.cleanupOldPinData(); // Run immediately
```

### SQL Manual Cleanup
```sql
-- Delete data older than 90 days
DELETE FROM raw_data 
WHERE ts < EXTRACT(EPOCH FROM NOW() - INTERVAL '90 days') * 1000;

-- Delete data older than specific date
DELETE FROM raw_data 
WHERE ts < EXTRACT(EPOCH FROM '2025-09-01'::timestamp) * 1000;
```

## Performance Considerations

### Index Usage
Cleanup query sử dụng index trên cột `ts`:
```sql
CREATE INDEX idx_raw_data_ts ON raw_data(ts);
```

### Large Deletions
Nếu có nhiều dữ liệu cũ (>1 triệu records), có thể:

1. **Batch Delete** (modify code):
```java
int batchSize = 10000;
int totalDeleted = 0;
while (true) {
    int deleted = dao.deleteOlderThanBatch(cutoffTime, batchSize);
    totalDeleted += deleted;
    if (deleted < batchSize) break;
    Thread.sleep(1000); // Pause between batches
}
```

2. **Partitioning** (database level):
```sql
-- Create partitioned table (PostgreSQL 10+)
CREATE TABLE raw_data_partitioned (
  -- same columns
) PARTITION BY RANGE (ts);

-- Create monthly partitions
CREATE TABLE raw_data_2025_12 
  PARTITION OF raw_data_partitioned 
  FOR VALUES FROM (1733011200000) TO (1735689600000);

-- Drop old partitions instead of DELETE
DROP TABLE raw_data_2025_09;
```

## Disabling Cleanup

Để tắt auto-cleanup (không khuyến khích):

```java
// Comment out trong VirtualPinService constructor
// scheduler.scheduleAtFixedRate(this::cleanupOldPinData, 
//     calculateInitialDelay(), 24 * 60 * 60, TimeUnit.SECONDS);
```

Hoặc set retention rất lớn:
```java
private static final long DATA_RETENTION_DAYS = 3650; // 10 years
```

## Best Practices

1. **Monitor database size** weekly:
   ```sql
   SELECT pg_size_pretty(pg_total_relation_size('raw_data'));
   ```

2. **Review cleanup logs** monthly
3. **Adjust retention period** based on:
   - Available disk space
   - Query performance
   - Business requirements
   - Compliance regulations

4. **Backup before changes**:
   ```bash
   pg_dump -t raw_data > raw_data_backup.sql
   ```

## Troubleshooting

### Cleanup Not Running
Check logs for scheduler initialization:
```
grep "Old pin data cleanup scheduled" cydcserver.log
```

### Database Errors
Check RawDataService logs:
```
grep "Error deleting old pin data" cydcserver.log
```

### Wrong Timezone
Cleanup time sử dụng server timezone. Verify:
```java
System.out.println(java.time.ZoneId.systemDefault());
```

## Related Files
- `VirtualPinService.java` - Cleanup logic
- `RawDataService.java` - Service wrapper
- `RawDataDao.java` - Database operations
- `App.java` - Database schema and indexes
