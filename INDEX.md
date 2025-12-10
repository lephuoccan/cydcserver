# CYDConnect Documentation Index

Welcome to the CYDConnect IoT Server documentation. This page provides a complete guide to all available resources.

## 📖 Main Documentation Files

### 1. [README.md](README.md) - Start Here!
**Main project overview and quick start guide**
- Project overview and features
- Quick start instructions
- Technology stack overview
- Configuration basics
- Troubleshooting guide

**Read this first to understand the project.**

---

### 2. [API.md](API.md) - Complete API Reference
**Comprehensive API documentation with all endpoints**
- Authentication mechanisms
- User management endpoints
- Dashboard CRUD operations
- Device management
- Virtual pin operations (V0-V127)
- WebSocket API
- Error handling and status codes
- Complete curl examples
- Database schema details

**Use this when building integrations or working with the API.**

---

### 3. [QUICK_REFERENCE.md](QUICK_REFERENCE.md) - Developer Cheat Sheet
**Quick lookup for common commands and configurations**
- Endpoint reference table
- Common curl commands
- Token format and authentication methods
- Configuration key values
- Build commands
- Error codes
- Environment variables
- Quick start checklist

**Use this for quick lookups and common tasks.**

---

### 4. [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md) - Architecture Deep Dive
**Detailed implementation details and architecture overview**
- Technology stack details
- Complete database schema with SQL
- Core features breakdown
- API workflow examples
- Authentication & security details
- Data storage & caching strategy
- Performance characteristics
- Configuration reference
- Implementation checklist
- Next steps and roadmap

**Use this to understand how the system is built.**

---

### 5. [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) - Production Deployment
**Complete deployment instructions for all environments**
- Prerequisites and requirements
- Local development setup
- Docker containerization
- Docker Compose setup
- Production deployment steps
- Nginx reverse proxy configuration
- Systemd service setup
- Database initialization
- Backup and recovery procedures
- Monitoring and maintenance
- Performance tuning

**Use this to deploy the server in any environment.**

---

### 6. [REGISTRATION_CHANGES.md](REGISTRATION_CHANGES.md) - Recent Updates
**Documentation of recent registration feature updates**
- Summary of changes made
- New registration endpoint behavior
- Testing procedures
- Example requests and responses

**Use this to stay updated on recent changes.**

---

## 🎯 Quick Navigation by Use Case

### I'm new to this project
1. Read [README.md](README.md)
2. Follow the Quick Start section
3. Run the server locally
4. Test with [QUICK_REFERENCE.md](QUICK_REFERENCE.md) curl commands

### I need to integrate with the API
1. Start with [API.md](API.md) for endpoint details
2. Use [QUICK_REFERENCE.md](QUICK_REFERENCE.md) for common commands
3. Check [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md) for authentication details

### I need to deploy to production
1. Read [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)
2. Choose your deployment method (Docker, Linux service, etc.)
3. Follow the step-by-step instructions
4. Use the monitoring section for health checks

### I want to understand the architecture
1. Read [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md)
2. Review the database schema section
3. Check the API workflow examples
4. Examine the source code in `src/main/java/cloud/cydc/`

### I need quick answers
1. Use [QUICK_REFERENCE.md](QUICK_REFERENCE.md) for common commands
2. Check the troubleshooting section in [README.md](README.md)
3. Look up specific endpoints in [API.md](API.md)

---

## 📡 Server Ports

| Port | Service | Purpose |
|------|---------|---------|
| 8080 | Netty TCP | Device protocol (hardware connections) |
| 8081 | HTTP REST | Application API (all REST endpoints) |
| 9001 | WebSocket | Real-time updates (WebSocket connections) |

---

## 🔑 Core Concepts

### Users
- Email-based identification
- Bcrypt-hashed passwords
- User ID format: `email-appName`
- Profile stored as JSONB

### Dashboards
- User dashboards organize devices
- Dashboard ID is a long integer
- Widget and device configuration via JSONB
- Cascade delete removes all devices

### Devices
- Devices belong to a specific dashboard
- Each device has a unique token for authentication
- Token format: `{userId}-{dashId}-{devId}-{randomToken}`
- Board type, vendor, connection type, and status tracking

### Virtual Pins
- 128 pins per device (V0-V127)
- Store arbitrary string or numeric data
- Cached in Redis for performance
- Persisted in PostgreSQL for durability
- Used for real-time data exchange

---

## 🚀 Common Tasks

### Register a User
```bash
curl -X POST http://localhost:8081/api/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","pass":"password123","appName":"Blynk"}'
```
See [API.md - User Management](API.md#user-management)

### Create a Device
```bash
curl -X POST http://localhost:8081/api/user/{userId}/dashboard/{dashId}/device \
  -H "Content-Type: application/json" \
  -d '{"id":0,"name":"ESP32","boardType":"ESP32","connectionType":"WIFI"}'
```
See [API.md - Device Management](API.md#device-management)

### Write to a Virtual Pin
```bash
curl -X PUT http://localhost:8081/api/pin/{deviceId}/V0 \
  -H "Content-Type: application/json" \
  -d '{"value":"255","token":"<device_token>"}'
```
See [API.md - Virtual Pin Operations](API.md#virtual-pin-operations-v0-v127)

### Read from a Virtual Pin
```bash
curl -X GET "http://localhost:8081/api/pin/{deviceId}/V0?token=<device_token>"
```
See [QUICK_REFERENCE.md](QUICK_REFERENCE.md#quick-start)

---

## 🛠️ Build & Run

### Build
```bash
mvn clean package -DskipTests
```

### Run Locally
```bash
java -jar target/cydcserver-1.0.0.jar
```

### Run with Docker
```bash
docker-compose up -d
```

For detailed instructions, see:
- [DEPLOYMENT_GUIDE.md - Docker Deployment](DEPLOYMENT_GUIDE.md#docker-deployment)
- [README.md - Quick Start](README.md#-quick-start)

---

## 📊 Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                   Client Applications                    │
└────────────┬──────────────────────────────────┬──────────┘
             │                                  │
             ▼                                  ▼
       ┌──────────────┐                   ┌──────────────┐
       │  REST API    │                   │  WebSocket   │
       │  (8081)      │                   │  (9001)      │
       └────┬─────────┘                   └──────┬───────┘
            │                                   │
            └───────────┬─────────────────────┬─┘
                        │                     │
                   ┌────▼────────────────────▼────┐
                   │    Spring Boot Application    │
                   │  (Business Logic, Services)   │
                   └────┬──────────────────────┬───┘
                        │                      │
           ┌────────────┴─┐              ┌────┴──────────┐
           ▼              ▼              ▼               ▼
      ┌─────────┐    ┌────────┐    ┌─────────┐    ┌──────────┐
      │  Netty  │    │Database│    │  Cache  │    │ Disk I/O │
      │  (8080) │    │(Postgre)   │ (Redis) │    │  Storage │
      └─────────┘    └────────┘    └─────────┘    └──────────┘
```

For complete architecture details, see [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md#architecture)

---

## 🔐 Security

- **Passwords**: Bcrypt hashing (12 rounds)
- **Tokens**: Cryptographically secure random generation
- **Authentication**: Token-based for device API
- **Authorization**: User-based access control
- **Data Storage**: JSONB for flexible schemas

For security details, see:
- [IMPLEMENTATION_SUMMARY.md - Security](IMPLEMENTATION_SUMMARY.md#authentication--security)
- [DEPLOYMENT_GUIDE.md - Production Setup](DEPLOYMENT_GUIDE.md#production-deployment)

---

## 📈 Performance

- **Redis Operations**: <1ms (cached pins)
- **Database Queries**: 1-5ms
- **Concurrent Support**: Thousands (async Netty)
- **Limits**: 128 pins/device, 4KB max pin value

See [IMPLEMENTATION_SUMMARY.md - Performance](IMPLEMENTATION_SUMMARY.md#performance-characteristics) for details.

---

## 🚀 Next Steps

1. **Get Started**: Follow [README.md](README.md#-quick-start)
2. **Learn the API**: Read [API.md](API.md)
3. **Deploy**: Use [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)
4. **Integrate**: Build with [QUICK_REFERENCE.md](QUICK_REFERENCE.md)
5. **Understand**: Deep dive with [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md)

---

## 🐛 Troubleshooting

For common issues, see:
- [README.md - Troubleshooting](README.md#-troubleshooting)
- [DEPLOYMENT_GUIDE.md - Troubleshooting](DEPLOYMENT_GUIDE.md#troubleshooting-production-issues)

---

## 📚 Additional Resources

### Project Files
- **Source Code**: `src/main/java/cloud/cydc/`
- **Tests**: `src/test/java/cloud/cydc/`
- **Build Config**: `pom.xml`
- **Executable**: `target/cydcserver-1.0.0.jar`

### External References
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Redis Documentation](https://redis.io/documentation)
- [Netty Documentation](https://netty.io/wiki/)
- [Blynk Legacy GitHub](https://github.com/blynkkk/blynk-server)

---

## 📞 Support & Feedback

Need help?
1. Check the [README.md](README.md#-troubleshooting)
2. Review [QUICK_REFERENCE.md](QUICK_REFERENCE.md)
3. Search [API.md](API.md) for your endpoint
4. Consult [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md)

---

## 📅 Version Information

- **Current Version**: 1.0.0
- **Status**: 🟢 Active Development
- **Java Version**: 21+
- **Build Tool**: Maven 3.9+
- **Last Updated**: 2024

---

## 📋 Documentation Checklist

- ✅ README.md - Project overview and quick start
- ✅ API.md - Complete API reference
- ✅ QUICK_REFERENCE.md - Cheat sheet and quick commands
- ✅ IMPLEMENTATION_SUMMARY.md - Architecture and design
- ✅ DEPLOYMENT_GUIDE.md - Production deployment
- ✅ REGISTRATION_CHANGES.md - Recent updates
- ✅ INDEX.md - This file

---

**For the best experience, start with [README.md](README.md) and then refer to other docs as needed!**
