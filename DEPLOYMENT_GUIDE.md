# CYDConnect - Deployment Guide

## Prerequisites

- Java 21 JDK or higher
- Maven 3.9 or higher
- PostgreSQL 12+
- Redis 6+
- Git (optional)

## Local Development Setup

### 1. Install Dependencies

#### Windows (Using Chocolatey)
```powershell
choco install jdk21 maven postgresql redis
```

#### macOS (Using Homebrew)
```bash
brew install openjdk@21 maven postgresql redis
```

#### Linux (Ubuntu/Debian)
```bash
sudo apt-get install openjdk-21-jdk maven postgresql redis-server
```

### 2. Start Database Services

#### PostgreSQL
```bash
# Windows (if installed via Chocolatey)
postgresql-13 start

# macOS
brew services start postgresql

# Linux
sudo systemctl start postgresql

# Or with Docker
docker run -d \
  --name cydc-postgres \
  -e POSTGRES_PASSWORD=password \
  -e POSTGRES_DB=blynk \
  -p 5432:5432 \
  postgres:15
```

#### Redis
```bash
# Windows (if installed via Chocolatey)
redis-server.exe

# macOS
brew services start redis

# Linux
sudo systemctl start redis-server

# Or with Docker
docker run -d \
  --name cydc-redis \
  -p 6379:6379 \
  redis:7
```

### 3. Build the Server

```bash
cd c:\cydc\server\cydcserver
mvn clean package -DskipTests
```

### 4. Run Locally

```bash
java -jar target/cydcserver-1.0.0.jar
```

### 5. Verify Server

```bash
# Health check
curl http://localhost:8081/api/health

# Response should be: {"status":"ok"}
```

---

## Docker Deployment

### Build Docker Image

Create `Dockerfile` in project root:

```dockerfile
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Copy built JAR
COPY target/cydcserver-1.0.0.jar app.jar

# Expose ports
EXPOSE 8080 8081 9001

# Set environment variables
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/blynk
ENV SPRING_DATASOURCE_USERNAME=postgres
ENV SPRING_DATASOURCE_PASSWORD=password
ENV SPRING_REDIS_HOST=redis
ENV SPRING_REDIS_PORT=6379

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8081/api/health || exit 1

# Run
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Docker Compose Setup

Create `docker-compose.yml`:

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15
    container_name: cydc-postgres
    environment:
      POSTGRES_PASSWORD: password
      POSTGRES_DB: blynk
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7
    container_name: cydc-redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  app:
    build: .
    container_name: cydc-app
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/blynk
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: password
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: 6379
    ports:
      - "8080:8080"
      - "8081:8081"
      - "9001:9001"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/api/health"]
      interval: 30s
      timeout: 10s
      retries: 3

volumes:
  postgres_data:
  redis_data:

networks:
  default:
    name: cydc-network
```

### Launch with Docker Compose

```bash
# Build and start all services
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop services
docker-compose down

# Remove volumes (WARNING: deletes data)
docker-compose down -v
```

---

## Production Deployment

### Prerequisites
- Domain name
- SSL/TLS certificate
- Reverse proxy (Nginx or Apache)
- Dedicated database server (optional)
- Load balancer (optional)

### 1. Environment Configuration

Create `.env` file:
```bash
# Database
DB_HOST=db.example.com
DB_PORT=5432
DB_NAME=blynk_prod
DB_USER=blynk_user
DB_PASS=secure_password_here

# Redis
REDIS_HOST=redis.example.com
REDIS_PORT=6379
REDIS_PASS=redis_password

# Server
SERVER_PORT=8080
API_PORT=8081
WS_PORT=9001

# Spring Profile
SPRING_PROFILE=prod
```

### 2. Production application.properties

```properties
# Server Configuration
server.port=8080
server.http.port=8081
server.websocket.port=9001

# Database Configuration
spring.datasource.url=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASS}
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5

# Redis Configuration
spring.redis.host=${REDIS_HOST}
spring.redis.port=${REDIS_PORT}
spring.redis.password=${REDIS_PASS}
spring.redis.timeout=2000ms

# JPA Configuration
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.jdbc.fetch_size=100

# Bcrypt Configuration
security.bcrypt.strength=12

# Logging
logging.level.root=WARN
logging.level.cloud.cydc=INFO
logging.level.org.springframework.web=INFO

# Feature Flags
enable.raw.data.store=true
enable.websocket=true
enable.metrics=true
```

### 3. Nginx Reverse Proxy Configuration

```nginx
upstream cydc_api {
    server localhost:8081;
}

upstream cydc_websocket {
    server localhost:9001;
}

server {
    listen 80;
    server_name cydc.example.com;
    
    # Redirect to HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name cydc.example.com;

    # SSL Certificates
    ssl_certificate /etc/ssl/certs/cydc.example.com.crt;
    ssl_certificate_key /etc/ssl/private/cydc.example.com.key;
    
    # SSL Configuration
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # Logging
    access_log /var/log/nginx/cydc_access.log;
    error_log /var/log/nginx/cydc_error.log;

    # API Endpoints
    location /api/ {
        proxy_pass http://cydc_api;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # WebSocket Endpoint
    location /ws {
        proxy_pass http://cydc_websocket;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        
        # WebSocket timeouts (longer)
        proxy_connect_timeout 7d;
        proxy_send_timeout 7d;
        proxy_read_timeout 7d;
    }

    # Health check
    location /health {
        proxy_pass http://cydc_api/api/health;
        access_log off;
    }
}
```

### 4. Systemd Service File

Create `/etc/systemd/system/cydc-server.service`:

```ini
[Unit]
Description=CYDConnect IoT Server
After=network.target postgresql.service redis.service
Wants=postgresql.service redis.service

[Service]
Type=simple
User=cydc
WorkingDirectory=/opt/cydc
Environment="SPRING_PROFILES_ACTIVE=prod"
Environment="SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/blynk"
Environment="SPRING_DATASOURCE_USERNAME=postgres"
Environment="SPRING_DATASOURCE_PASSWORD=password"
Environment="SPRING_REDIS_HOST=redis"
EnvironmentFile=/etc/cydc/.env
ExecStart=/usr/bin/java -Xmx1024m -Xms512m -jar /opt/cydc/cydcserver-1.0.0.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

### 5. Enable and Start Service

```bash
sudo systemctl daemon-reload
sudo systemctl enable cydc-server
sudo systemctl start cydc-server
sudo systemctl status cydc-server

# View logs
sudo journalctl -u cydc-server -f
```

---

## Database Initialization

### PostgreSQL Schema Creation

```sql
-- Create database
CREATE DATABASE blynk;

-- Connect to blynk database
\c blynk

-- Create users table
CREATE TABLE users (
    id TEXT PRIMARY KEY,
    email TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    app_name TEXT,
    profile JSONB,
    is_super_admin BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create dashboards table
CREATE TABLE dashboards (
    user_id TEXT REFERENCES users(id) ON DELETE CASCADE,
    dashboard_id BIGINT,
    name TEXT,
    data JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, dashboard_id)
);

-- Create device_info table
CREATE TABLE device_info (
    user_id TEXT REFERENCES users(id) ON DELETE CASCADE,
    dashboard_id BIGINT REFERENCES dashboards(dashboard_id) ON DELETE CASCADE,
    device_id BIGINT,
    token TEXT UNIQUE NOT NULL,
    data JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, dashboard_id, device_id)
);

-- Create indexes for performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_dashboards_user_id ON dashboards(user_id);
CREATE INDEX idx_device_info_user_id ON device_info(user_id);
CREATE INDEX idx_device_info_token ON device_info(token);
```

---

## Monitoring & Maintenance

### Health Check
```bash
curl https://cydc.example.com/api/health
```

### Server Metrics
```bash
curl https://cydc.example.com/api/admin/stats
```

### Log Monitoring
```bash
# Real-time logs (Docker)
docker logs -f cydc-app

# Real-time logs (Systemd)
sudo journalctl -u cydc-server -f

# View specific date range
journalctl -u cydc-server --since "2024-01-01" --until "2024-01-02"
```

### Database Maintenance
```bash
# PostgreSQL backup
pg_dump -h localhost -U postgres blynk > backup.sql

# PostgreSQL restore
psql -h localhost -U postgres blynk < backup.sql

# Vacuum (cleanup)
vacuum full analyze;
```

### Redis Monitoring
```bash
# Redis CLI
redis-cli

# Check key count
dbsize

# Monitor operations (real-time)
monitor

# Save snapshot
save
```

---

## Backup & Recovery

### Automated Daily Backup

Create backup script `/opt/cydc/backup.sh`:

```bash
#!/bin/bash

BACKUP_DIR="/backups/cydc"
DATE=$(date +%Y%m%d_%H%M%S)

# PostgreSQL backup
pg_dump -h localhost -U postgres blynk | gzip > $BACKUP_DIR/db_$DATE.sql.gz

# Redis backup (if persistence enabled)
redis-cli --rdb $BACKUP_DIR/redis_$DATE.rdb

# Keep only last 7 days
find $BACKUP_DIR -name "db_*.sql.gz" -mtime +7 -delete
find $BACKUP_DIR -name "redis_*.rdb" -mtime +7 -delete

echo "Backup completed: $DATE"
```

Add to crontab (daily at 2 AM):
```bash
0 2 * * * /opt/cydc/backup.sh
```

---

## Troubleshooting Production Issues

| Issue | Resolution |
|-------|-----------|
| High memory usage | Increase JVM heap: `-Xmx2048m` |
| Slow API responses | Check database connection pool, add indexes |
| WebSocket disconnects | Increase proxy timeouts, check firewall |
| Redis connection errors | Verify Redis availability, check credentials |
| Database connection pool exhausted | Increase `maximum-pool-size` in properties |

---

## Performance Tuning

### JVM Garbage Collection
```bash
java -Xmx2048m -Xms1024m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -jar cydcserver-1.0.0.jar
```

### Database Connection Pool
```properties
spring.datasource.hikari.maximum-pool-size=30
spring.datasource.hikari.minimum-idle=10
spring.datasource.hikari.connection-timeout=30000
```

### Redis Connection Pool
```properties
spring.redis.jedis.pool.max-active=20
spring.redis.jedis.pool.max-idle=10
```

---

**Last Updated**: 2024  
**Version**: 1.0.0
