# CYDConnect - Quick Reference Guide

## Server Endpoints

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/register` | Create new user |
| POST | `/api/login` | Authenticate user |
| DELETE | `/api/user/{userId}` | Delete user |
| POST | `/api/user/{userId}/dashboard` | Create dashboard |
| GET | `/api/user/{userId}/dashboard/{dashId}` | Get dashboard |
| DELETE | `/api/user/{userId}/dashboard/{dashId}` | Delete dashboard |
| POST | `/api/user/{userId}/dashboard/{dashId}/device` | Create device |
| GET | `/api/user/{userId}/dashboard/{dashId}/device/{devId}` | Get device |
| DELETE | `/api/user/{userId}/dashboard/{dashId}/device/{devId}` | Delete device |
| PUT | `/api/user/{userId}/dashboard/{dashId}/device/{devId}/token` | Refresh token |
| PUT | `/api/pin/{deviceId}/V{pinNum}` | Write pin value |
| GET | `/api/pin/{deviceId}/V{pinNum}` | Read pin value |
| GET | `/api/pin/{deviceId}` | Read all pins |
| GET | `/api/health` | Health check |
| GET | `/api/admin/stats` | Server stats |

---

## Common curl Commands

### Register User
```bash
curl -X POST http://localhost:8081/api/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","pass":"password123","appName":"Blynk"}'
```

### Login
```bash
curl -X POST http://localhost:8081/api/login \
  -H "Content-Type: application/json" \
  -d '{"id":"user@example.com-Blynk","pass":"password123"}'
```

### Create Dashboard
```bash
curl -X POST http://localhost:8081/api/user/user@example.com-Blynk/dashboard \
  -H "Content-Type: application/json" \
  -d '{"id":1,"name":"MyDash","isActive":true,"devices":[],"widgets":[]}'
```

### Create Device
```bash
curl -X POST http://localhost:8081/api/user/user@example.com-Blynk/dashboard/1/device \
  -H "Content-Type: application/json" \
  -d '{"id":0,"name":"ESP32","boardType":"ESP32","vendor":"Test","connectionType":"WIFI","status":"ONLINE"}'
```

### Write Pin
```bash
TOKEN="user@example.com-Blynk-1-0-abc123xyz"
curl -X PUT http://localhost:8081/api/pin/0/V0 \
  -H "Content-Type: application/json" \
  -d "{\"value\":\"255\",\"token\":\"$TOKEN\"}"
```

### Read Pin
```bash
TOKEN="user@example.com-Blynk-1-0-abc123xyz"
curl -X GET "http://localhost:8081/api/pin/0/V0?token=$TOKEN"
```

### Read All Pins
```bash
TOKEN="user@example.com-Blynk-1-0-abc123xyz"
curl -X GET "http://localhost:8081/api/pin/0?token=$TOKEN"
```

---

## User ID Format

Token structure:
```
{email}-{appName}-{dashboardId}-{deviceId}-{randomToken}
```

Example:
```
user@example.com-Blynk-1-0-abc123xyz
```

---

## Key Configuration Values

```properties
# Ports
server.port=8080                           # Device protocol
server.http.port=8081                      # REST API
server.websocket.port=9001                 # WebSocket

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/blynk
spring.datasource.username=postgres
spring.datasource.password=password

# Redis
spring.redis.host=localhost
spring.redis.port=6379

# Security
security.bcrypt.strength=12
```

---

## Virtual Pin Ranges

- **V0 - V127**: 128 virtual pins per device
- **Data Type**: String or numeric
- **Max Size**: 4KB per pin
- **Storage**: Redis (fast) + PostgreSQL (persistent)

---

## Build Commands

```bash
# Build
mvn clean package -DskipTests

# Run
java -jar target/cydcserver-1.0.0.jar

# Build with profile
mvn clean package -P prod

# Run tests
mvn test

# Clean artifacts
mvn clean
```

---

## Error Codes

| Code | Meaning |
|------|---------|
| 200 | OK |
| 201 | Created |
| 400 | Bad Request |
| 401 | Unauthorized |
| 404 | Not Found |
| 409 | Conflict (duplicate) |
| 500 | Server Error |

---

## Database Tables

**users**: `id, email, password_hash, app_name, profile, is_super_admin, created_at`

**dashboards**: `user_id, dashboard_id, name, data, created_at`

**device_info**: `user_id, dashboard_id, device_id, token, data, created_at`

---

## Environment Variables (Optional)

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/blynk
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=password
export SPRING_REDIS_HOST=redis
export SPRING_REDIS_PORT=6379
export SERVER_PORT=8080
export SERVER_HTTP_PORT=8081
export SERVER_WEBSOCKET_PORT=9001
```

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Connection refused on 8081 | Check if server is running, verify port |
| Database connection error | Verify PostgreSQL is running, check credentials |
| Redis connection error | Verify Redis is running on localhost:6379 |
| Invalid token error | Token may be expired, regenerate via PUT token endpoint |
| User not found (401) | Verify user ID format: `email-appName` |

---

## File Locations

| File | Purpose |
|------|---------|
| `API.md` | Complete API documentation |
| `IMPLEMENTATION_SUMMARY.md` | Architecture and design overview |
| `QUICK_REFERENCE.md` | This file |
| `pom.xml` | Maven build configuration |
| `src/main/java/cloud/cydc/` | Source code |
| `target/cydcserver-1.0.0.jar` | Built application |

---

## Token Authentication Methods

1. **JSON Body**
   ```json
   {"value":"255","token":"user@example.com-Blynk-1-0-abc123xyz"}
   ```

2. **Query Parameter**
   ```
   ?token=user@example.com-Blynk-1-0-abc123xyz
   ```

3. **Authorization Header**
   ```
   Authorization: Bearer user@example.com-Blynk-1-0-abc123xyz
   ```

---

## Quick Start

```bash
# 1. Start PostgreSQL and Redis
docker run -d -p 5432:5432 -e POSTGRES_PASSWORD=password postgres
docker run -d -p 6379:6379 redis

# 2. Build
mvn clean package -DskipTests

# 3. Run
java -jar target/cydcserver-1.0.0.jar

# 4. Test health
curl http://localhost:8081/api/health

# 5. Register user
curl -X POST http://localhost:8081/api/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","pass":"password123"}'
```

---

**Version**: 1.0.0  
**Last Updated**: 2024  
**Status**: Active
