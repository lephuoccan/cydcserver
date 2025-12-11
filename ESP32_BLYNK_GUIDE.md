# ESP32 Integration with Blynk Library v0.6.1

## Tổng quan

Server này hỗ trợ ESP32 devices sử dụng **Blynk Library v0.6.1** để giao tiếp qua binary protocol.

### Ports

- **8080**: TCP text-based protocol (legacy)
- **8081**: HTTP REST API
- **8442**: Blynk binary protocol (cho ESP32)
- **9001**: WebSocket

## Cài đặt Blynk Library v0.6.1 cho ESP32

### Arduino IDE

1. Vào **Sketch** → **Include Library** → **Manage Libraries**
2. Tìm "Blynk" by Volodymyr Shymanskyy
3. Chọn version **0.6.1**
4. Click **Install**

### PlatformIO

Thêm vào `platformio.ini`:

```ini
[env:esp32]
platform = espressif32
board = esp32dev
framework = arduino
lib_deps = 
    blynkkk/Blynk@^0.6.1
```

## Code mẫu cho ESP32

### Đơn giản nhất

```cpp
#define BLYNK_PRINT Serial

#include <WiFi.h>
#include <BlynkSimpleEsp32.h>

// WiFi credentials
char ssid[] = "YOUR_WIFI_SSID";
char pass[] = "YOUR_WIFI_PASSWORD";

// Blynk server and token
char server[] = "192.168.1.100";  // IP của server
int port = 8442;                   // Blynk protocol port
char auth[] = "YOUR_DEVICE_TOKEN"; // Token từ API /api/device

void setup() {
  Serial.begin(115200);
  
  // Connect to Blynk server
  Blynk.begin(auth, ssid, pass, server, port);
  
  Serial.println("Connected to server!");
}

void loop() {
  Blynk.run();
}
```

### Với Virtual Pin Write

```cpp
#define BLYNK_PRINT Serial
#include <WiFi.h>
#include <BlynkSimpleEsp32.h>

char ssid[] = "YOUR_WIFI";
char pass[] = "YOUR_PASS";
char server[] = "192.168.1.100";
int port = 8442;
char auth[] = "YOUR_TOKEN";

BlynkTimer timer;

void sendSensor() {
  // Đọc sensor (ví dụ: analog pin)
  int sensorValue = analogRead(34);
  
  // Gửi lên virtual pin V1
  Blynk.virtualWrite(V1, sensorValue);
  Serial.print("Sent V1: ");
  Serial.println(sensorValue);
  
  // Gửi nhiều giá trị
  Blynk.virtualWrite(V2, millis() / 1000);  // Uptime in seconds
  Blynk.virtualWrite(V3, WiFi.RSSI());      // WiFi signal strength
}

void setup() {
  Serial.begin(115200);
  
  Blynk.begin(auth, ssid, pass, server, port);
  
  // Gửi data mỗi 2 giây
  timer.setInterval(2000L, sendSensor);
}

void loop() {
  Blynk.run();
  timer.run();
}
```

### Với Virtual Pin Read (nhận lệnh từ server)

```cpp
#define BLYNK_PRINT Serial
#include <WiFi.h>
#include <BlynkSimpleEsp32.h>

char ssid[] = "YOUR_WIFI";
char pass[] = "YOUR_PASS";
char server[] = "192.168.1.100";
int port = 8442;
char auth[] = "YOUR_TOKEN";

const int LED_PIN = 2;  // Built-in LED

// Nhận giá trị từ virtual pin V0
BLYNK_WRITE(V0) {
  int value = param.asInt();  // 0 hoặc 1
  digitalWrite(LED_PIN, value);
  Serial.print("LED set to: ");
  Serial.println(value);
}

// Nhận giá trị từ virtual pin V4 (PWM)
BLYNK_WRITE(V4) {
  int pwm = param.asInt();  // 0-255
  analogWrite(LED_PIN, pwm);
  Serial.print("PWM set to: ");
  Serial.println(pwm);
}

void setup() {
  Serial.begin(115200);
  pinMode(LED_PIN, OUTPUT);
  
  Blynk.begin(auth, ssid, pass, server, port);
}

void loop() {
  Blynk.run();
}
```

### Complete Example: Bidirectional

```cpp
#define BLYNK_PRINT Serial
#include <WiFi.h>
#include <BlynkSimpleEsp32.h>

char ssid[] = "YOUR_WIFI";
char pass[] = "YOUR_PASS";
char server[] = "192.168.1.100";
int port = 8442;
char auth[] = "YOUR_TOKEN";

const int LED_PIN = 2;
const int SENSOR_PIN = 34;

BlynkTimer timer;

// Nhận lệnh bật/tắt LED từ server (V0)
BLYNK_WRITE(V0) {
  int ledState = param.asInt();
  digitalWrite(LED_PIN, ledState);
  Serial.printf("LED: %s\n", ledState ? "ON" : "OFF");
}

// Nhận PWM value từ server (V1)
BLYNK_WRITE(V1) {
  int pwm = param.asInt();
  analogWrite(LED_PIN, pwm);
  Serial.printf("PWM: %d\n", pwm);
}

// Gửi sensor data lên server
void sendSensorData() {
  int sensorValue = analogRead(SENSOR_PIN);
  float voltage = sensorValue * (3.3 / 4095.0);
  
  // Gửi lên V2 và V3
  Blynk.virtualWrite(V2, sensorValue);
  Blynk.virtualWrite(V3, voltage);
  
  Serial.printf("Sensor: %d (%.2fV)\n", sensorValue, voltage);
}

// Gửi uptime
void sendUptime() {
  long uptime = millis() / 1000;
  Blynk.virtualWrite(V4, uptime);
}

void setup() {
  Serial.begin(115200);
  pinMode(LED_PIN, OUTPUT);
  pinMode(SENSOR_PIN, INPUT);
  
  Serial.println("Connecting to Blynk server...");
  Blynk.begin(auth, ssid, pass, server, port);
  Serial.println("Connected!");
  
  // Send data every 2 seconds
  timer.setInterval(2000L, sendSensorData);
  timer.setInterval(5000L, sendUptime);
}

void loop() {
  Blynk.run();
  timer.run();
}
```

## Lấy Device Token

### 1. Đăng ký user

```bash
curl -X POST http://localhost:8081/api/register \
  -H "Content-Type: application/json" \
  -d '{"userId":"test@example.com","email":"test@example.com","password":"password123"}'
```

### 2. Tạo dashboard

```bash
curl -X POST http://localhost:8081/api/dashboard/test@example.com/0 \
  -H "Content-Type: application/json" \
  -d '{"name":"ESP32 Dashboard"}'
```

### 3. Tạo device và lấy token

```bash
curl -X POST http://localhost:8081/api/device/test@example.com/0 \
  -H "Content-Type: application/json" \
  -d '{"name":"ESP32 Device"}'
```

Response:
```json
{
  "success": true,
  "message": "Device created",
  "device": {
    "devId": 0,
    "name": "ESP32 Device",
    "token": "test@example.com-0-0-a1b2c3d4"
  }
}
```

Dùng `token` này trong ESP32 code.

## Kiểm tra pin values từ server

### Read một pin

```bash
curl http://localhost:8081/api/pin/0/1
```

Response:
```json
{
  "deviceId": 0,
  "pinNum": 1,
  "value": "1234"
}
```

### Write một pin (từ server hoặc app)

```bash
curl -X POST http://localhost:8081/api/pin/0/1 \
  -H "Content-Type: application/json" \
  -d '{"value":"5678"}'
```

### Read nhiều pins

```bash
curl http://localhost:8081/api/pins/0?start=0&count=10
```

## Virtual Pin Mapping

| Pin   | Mục đích ví dụ       | Type   |
|-------|---------------------|--------|
| V0    | LED control         | Output |
| V1    | PWM control         | Output |
| V2    | Sensor raw value    | Input  |
| V3    | Sensor voltage      | Input  |
| V4    | Uptime              | Input  |
| V5-V9 | Reserved            | -      |
| V10+  | Custom use          | Mixed  |

## Debugging

### Enable Blynk debug output

```cpp
#define BLYNK_PRINT Serial
#define BLYNK_DEBUG
```

### Check server logs

Server sẽ log mọi Blynk messages:

```
[Blynk] Received: Command=LOGIN, MessageId=1, Length=28
[Blynk] Login successful for token: test@example.com-0-0-a1b2c3d4
[Blynk] Received: Command=HARDWARE, MessageId=2, Length=8
[Blynk] Virtual pin write: V1 = 1234
```

## Blynk Protocol Details

### Binary Format

```
[Command (1 byte)][MessageId (2 bytes)][Length (2 bytes)][Body (Length bytes)]
```

### Commands

- `LOGIN (2)`: Authenticate với token
- `PING (6)`: Keepalive
- `HARDWARE (20)`: Virtual pin read/write
- `RESPONSE (0)`: Server response

### Hardware Command Format

**Write**: `vw\0V1\01234`
- `vw`: virtual write
- `V1`: pin V1
- `1234`: value

**Read**: `vr\0V1`
- `vr`: virtual read
- `V1`: pin to read

## Troubleshooting

### ESP32 không connect được

1. Check WiFi:
```cpp
Serial.println(WiFi.status());
```

2. Check token format: `userId-dashId-deviceId-random`

3. Ping server:
```bash
ping 192.168.1.100
```

4. Check port 8442 open:
```bash
telnet 192.168.1.100 8442
```

### Data không update

1. Check Redis connection
2. Check PostgreSQL
3. Tăng timer interval trong ESP32
4. Enable debug mode

### Connection drops

1. Thêm reconnect logic:
```cpp
void loop() {
  if (!Blynk.connected()) {
    Serial.println("Reconnecting...");
    Blynk.connect();
  }
  Blynk.run();
}
```

## Performance Tips

- Giới hạn virtualWrite frequency: max 10 writes/second
- Sử dụng BlynkTimer thay vì delay()
- Batch multiple writes nếu có thể
- Sử dụng BLYNK_WRITE callbacks cho inputs

## Tài liệu

- Blynk Library Docs: https://github.com/blynkkk/blynk-library/tree/v0.6.1
- ESP32 Arduino Core: https://docs.espressif.com/projects/arduino-esp32/
- Server API: http://localhost:8081/api

## Ví dụ Advanced

### OTA Updates
### Deep Sleep
### Multiple Devices
### Custom Protocol Extensions

(Sẽ update sau)
