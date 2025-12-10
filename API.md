# CYDConnect Server - API Documentation

Comprehensive IoT server with dashboard, device, and virtual pin management inspired by Blynk Legacy.

## Overview

CYDConnect is a Blynk-compatible IoT server built with:
- **Netty** - Asynchronous networking for device connections
- **PostgreSQL** - Persistent data storage (users, dashboards, devices)
- **Redis** - In-memory caching for virtual pin values
- **Spring Boot** - REST API framework
- **Bcrypt** - Secure password hashing

### Server Components
- **Port 8080**: Hardware TCP protocol (Netty device connections)
- **Port 8081**: HTTP REST API (application/json)
- **Port 9001**: WebSocket server (real-time app updates)

---

## Authentication

### Token Format
Device tokens are generated as: `{userId}-{dashboardId}-{deviceId}-{randomToken}`

Example: `user@example.com-Blynk-1-123-abc123xyz`

### Request Methods
Tokens can be provided in:
1. JSON body: `{"token":"...", "value":"..."}`
2. Query parameter: `?token=...`
3. Authorization header: `Authorization: Bearer token`

---

## API Endpoints

### User Management

#### POST `/api/register`
Register a new user account.

**Request:**
```json
{
  "email": "user@example.com",
  "pass": "password123",
  "appName": "Blynk"
}
```

**Response (201 Created):**
```json
{
  "status": "ok",
  "userId": "user@example.com-Blynk"
}
```

**Response (409 Conflict):**
```json
{
  "error": "User already exists"
}
```

**Notes:**
- `email` and `pass` are required
- `appName` is optional (defaults to "Blynk")
- User ID is auto-generated as `email-appName`
- Passwords are hashed using bcrypt (12 rounds)
- `isSuperAdmin` is always `false` for new users

---

#### POST `/api/login`
Authenticate user and retrieve profile.

**Request:**
```json
{
  "id": "user@example.com-Blynk",
  "pass": "password123"
}
```

**Response (200 OK):**
```json
{
  "id": "user@example.com-Blynk",
  "name": "user@example.com",
  "email": "user@example.com",
  "appname": "Blynk",
  "region": "local",
  "ip": "127.0.0.1",
  "issuperadmin": false,
  "energy": 1000000000
}
```

**Response (401 Unauthorized):**
```json
{
  "error": "invalid credentials"
}
```

---

#### DELETE `/api/user/{userId}`
Delete a user account and all associated data (dashboards, devices, pins).

**Response (200 OK):**
```json
{
  "status": "deleted"
}
```

---

### Dashboard Management

#### POST `/api/user/{userId}/dashboard`
Create or update a dashboard.

**Request:**
```json
{
  "id": 1365449706,
  "name": "IOT Dashboard",
  "isActive": true,
  "devices": [],
  "widgets": []
}
```

**Response (201 Created):**
```json
{
  "status": "ok"
}
```

---

#### GET `/api/user/{userId}/dashboard/{dashId}`
Retrieve dashboard details including name and widget configuration.

**Response (200 OK):**
```json
{
  "id": 1365449706,
  "name": "IOT Dashboard",
  "isActive": true,
  "devices": [],
  "widgets": []
}
```

---

#### DELETE `/api/user/{userId}/dashboard/{dashId}`
Delete a dashboard and all associated devices.

**Response (200 OK):**
```json
{
  "status": "deleted"
}
```

---

### Device Management

#### POST `/api/user/{userId}/dashboard/{dashId}/device`
Create or update a device.

**Request:**
```json
{
  "id": 0,
  "name": "ESP32",
  "boardType": "ESP32",
  "vendor": "New Device",
  "connectionType": "WIFI",
  "status": "ONLINE"
}
```

**Response (201 Created):**
```json
{
  "status": "ok"
}
```

**Notes:**
- Device token is auto-generated if not provided
- Token format: `{userId}-{dashId}-{deviceId}-{randomToken}`

---

#### GET `/api/user/{userId}/dashboard/{dashId}/device/{devId}`
Retrieve device details including token and status.

**Response (200 OK):**
```json
{
  "id": 0,
  "name": "ESP32",
  "boardType": "ESP32",
  "token": "user@example.com-Blynk-1-0-abc123xyz",
  "vendor": "New Device",
  "connectionType": "WIFI",
  "status": "ONLINE"
}
```

---

#### DELETE `/api/user/{userId}/dashboard/{dashId}/device/{devId}`
Delete a device and all associated virtual pin values.

**Response (200 OK):**
```json
{
  "status": "deleted"
}
```

---

#### PUT `/api/user/{userId}/dashboard/{dashId}/device/{devId}/token`
Refresh/regenerate device token for security.

**Response (200 OK):**
```json
{
  "token": "user@example.com-Blynk-1-0-newtoken123abc"
}
```

---

### Virtual Pin Operations (V0-V127)

Virtual pins allow devices to store and retrieve arbitrary data. Each device supports 128 pins (V0-V127).

#### PUT `/api/pin/{deviceId}/V{pinNum}`
Write a value to a virtual pin.

**Request:**
```json
{
  "value": "255",
  "token": "user@example.com-Blynk-1-123-abc123xyz"
}
```

**Response (200 OK):**
```json
{
  "status": "ok"
}
```

**Error Response (401 Unauthorized):**
```json
{
  "error": "invalid or missing token"
}
```

**Storage:**
- Values are stored in Redis (fast access)
- Persisted to PostgreSQL (historical records if enabled)
- Supports both string and numeric values

---

#### GET `/api/pin/{deviceId}/V{pinNum}`
Read a value from a virtual pin.

**Query Parameters:**
- `token` (required) - Device token for authentication

**Response (200 OK):**
```json
{
  "pin": "V0",
  "value": "255"
}
```

**Response (404 Not Found):**
```json
{
  "error": "pin not found"
}
```

---

#### GET `/api/pin/{deviceId}`
Retrieve all virtual pin values (V0-V127) for a device as JSON.

**Query Parameters:**
- `token` (required) - Device token for authentication

**Response (200 OK):**
```json
{
  "V0": "255",
  "V1": "128",
  "V2": "0",
  "V3": "hello"
}
```

---

### Server Health & Metrics

#### GET `/api/health`
Simple health check endpoint.

**Response (200 OK):**
```json
{
  "status": "ok"
}
```

---

#### GET `/api/admin/stats` (Admin Only)
Retrieve server statistics and system metrics.

**Response (200 OK):**
```json
{
  "uptime": "3600000",
  "memory_used_mb": 256,
  "memory_max_mb": 1024,
  "java_version": "21.0.9",
  "timestamp": 1702262400000
}
```

---

## WebSocket API

### Connection
Connect to `ws://localhost:9001/ws` (or `wss://` for SSL)

### Subscribe to Device Updates
```json
{
  "cmd": "subscribe",
  "userId": "user@example.com-Blynk",
  "deviceId": "123"
}
```

### Unsubscribe from Device Updates
```json
{
  "cmd": "unsubscribe",
  "userId": "user@example.com-Blynk",
  "deviceId": "123"
}
```

### Heartbeat
Server sends periodic ping frames to keep connection alive.

### Pin Update Notification (Server → Client)
```json
{
  "type": "pin_update",
  "deviceId": "123",
  "pin": "V0",
  "value": "255",
  "timestamp": 1702262400000
}
```

---

## Complete Workflow Examples

### 1. Register User and Create Device
```bash
# Register user
curl -X POST http://localhost:8081/api/register \
  -H "Content-Type: application/json" \
  -d '{
    "email":"user@example.com",
    "pass":"password123",
    "appName":"Blynk"
  }'
# Response: {"status":"ok","userId":"user@example.com-Blynk"}

# Login
curl -X POST http://localhost:8081/api/login \
  -H "Content-Type: application/json" \
  -d '{
    "id":"user@example.com-Blynk",
    "pass":"password123"
  }'

# Create dashboard
curl -X POST http://localhost:8081/api/user/user@example.com-Blynk/dashboard \
  -H "Content-Type: application/json" \
  -d '{
    "id":1,
    "name":"MyDash",
    "isActive":true,
    "devices":[],
    "widgets":[]
  }'

# Create device
curl -X POST http://localhost:8081/api/user/user@example.com-Blynk/dashboard/1/device \
  -H "Content-Type: application/json" \
  -d '{
    "id":0,
    "name":"ESP32",
    "boardType":"ESP32",
    "vendor":"Test",
    "connectionType":"WIFI",
    "status":"ONLINE"
  }'

# Get device to retrieve token
curl -X GET http://localhost:8081/api/user/user@example.com-Blynk/dashboard/1/device/0
```

### 2. Device Operations with Tokens
```bash
TOKEN="user@example.com-Blynk-1-0-abc123xyz"

# Set virtual pin V0 to 255
curl -X PUT http://localhost:8081/api/pin/0/V0 \
  -H "Content-Type: application/json" \
  -d "{\"value\":\"255\",\"token\":\"$TOKEN\"}"

# Read pin V0
curl -X GET "http://localhost:8081/api/pin/0/V0?token=$TOKEN"

# Read all pins
curl -X GET "http://localhost:8081/api/pin/0?token=$TOKEN"

# Refresh device token
curl -X PUT http://localhost:8081/api/user/user@example.com-Blynk/dashboard/1/device/0/token
```

---

## Database Schema

### Users Table
```
users:
  - id (TEXT, PRIMARY KEY) - e.g., "user@example.com-Blynk"
  - email (TEXT UNIQUE)
  - password_hash (TEXT) - bcrypt hash
  - app_name (TEXT) - Application name
  - profile (JSONB) - User metadata
  - is_super_admin (BOOLEAN)
  - created_at (TIMESTAMP)
```

### Dashboards Table
```
dashboards:
  - user_id (TEXT, FK)
  - dashboard_id (BIGINT)
  - name (TEXT)
  - data (JSONB) - Widget configuration
  - created_at (TIMESTAMP)
  - PRIMARY KEY (user_id, dashboard_id)
```

### Device Info Table
```
device_info:
  - user_id (TEXT, FK)
  - dashboard_id (BIGINT, FK)
  - device_id (BIGINT)
  - token (TEXT UNIQUE) - Device authentication token
  - data (JSONB) - Device metadata
  - created_at (TIMESTAMP)
  - PRIMARY KEY (user_id, dashboard_id, device_id)
```

### Redis Cache
```
Pin values cached in Redis:
Key: pin:{deviceId}:V{pinNum}
Value: String or numeric value
TTL: No expiration (manual refresh on device update)
```

---

## Error Handling

All error responses follow this format:
```json
{
  "error": "Description of the error"
}
```

Common HTTP Status Codes:
- **200 OK** - Successful operation
- **201 Created** - Resource created
- **400 Bad Request** - Invalid input or malformed JSON
- **401 Unauthorized** - Invalid or missing authentication
- **404 Not Found** - Resource does not exist
- **409 Conflict** - Resource already exists (e.g., duplicate user)
- **500 Internal Server Error** - Server-side error

---

## Configuration

Edit `application.properties` to customize server behavior:

```properties
# Server ports
server.port=8080
server.http.port=8081
server.websocket.port=9001

# Database
db.url=jdbc:postgresql://localhost:5432/blynk
db.user=postgres
db.pass=password

# Redis cache
redis.uri=redis://localhost:6379

# Optional features
enable.raw.data.store=false
enable.websocket=true
bcrypt.strength=12
```

---

## Performance & Limits

- **Virtual Pins**: 128 per device (V0-V127)
- **Devices**: Unlimited per dashboard
- **Dashboards**: Unlimited per user
- **Token Length**: ~64 characters (secure random)
- **Pin Value**: String up to 4KB
- **Concurrent Connections**: Limited by system resources

---

## Security Considerations

1. **Passwords**: Always hashed with bcrypt (12 rounds)
2. **Tokens**: Cryptographically secure random generation
3. **HTTPS**: Recommended for production (use reverse proxy)
4. **Authentication**: Token-based for device API
5. **CORS**: Configure as needed for cross-origin requests
6. **Rate Limiting**: Not yet implemented, plan for production

---

## Implementation Status

✅ Implemented:
- User registration and authentication
- Dashboard management
- Device management
- Virtual pin read/write (V0-V127)
- Token generation and refresh
- PostgreSQL persistence
- Redis caching
- Bcrypt password hashing
- Health check endpoint

🚀 Roadmap:
- WebSocket real-time updates
- Raw data historical storage
- Server metrics endpoint
- Rate limiting
- API documentation (Swagger)
- Integration tests
- Docker support

---

## Build & Run

```bash
# Build
mvn -DskipTests package

# Run
java -jar target/cydcserver-1.0.0.jar

# Run with specific profile
java -jar target/cydcserver-1.0.0.jar --spring.profiles.active=prod
```

Server starts on:
- **Port 8080**: Device protocol (Netty)
- **Port 8081**: REST API
- **Port 9001**: WebSocket (when enabled)
