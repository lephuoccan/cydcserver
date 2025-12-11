# Blynk Protocol Debug Notes

## Issue: ESP32 disconnects immediately after LOGIN

### Timeline:
1. ESP32 sends LOGIN (cmd=29) with token
2. Server validates token → SUCCESS
3. Server sends RESPONSE (cmd=0, msgId=1)
4. ESP32 disconnects **5-60ms later**

### What we've tried:

#### 1. Response with status code 200
```java
// Status 200 = 0x00C8 big-endian
byte[] body = {0x00, 0xC8};
RESPONSE[msgId=1, body=2 bytes]
```
**Result**: Disconnect

#### 2. Empty RESPONSE body
```java
byte[] body = {};
RESPONSE[msgId=1, body=0 bytes]
```
**Result**: Testing now...

#### 3. Next to try: NO RESPONSE at all
Some Blynk implementations don't send RESPONSE for LOGIN.
Just keep connection alive and wait for HARDWARE commands.

### Blynk Library v0.6.1 Expected Behavior

From Blynk docs and source:
- LOGIN command should receive RESPONSE
- But some custom servers just accept and wait
- Heartbeat (PING) usually sent by client every 10-45 seconds

### Possible Solutions:

1. **Don't send RESPONSE for LOGIN**
   - Just authenticate and keep connection
   - Wait for client to send HARDWARE or PING

2. **Send different status code**
   - Try 0x00 instead of 200
   - Or specific device/dashboard info

3. **Check if client expects PING immediately**
   - Server might need to send PING first
   - To establish keepalive

4. **Protocol version mismatch**
   - v0.6.1 might have different expectations
   - Need to check official Blynk server behavior

### Next Steps:
- [ ] Test empty RESPONSE
- [ ] Test no RESPONSE at all
- [ ] Test immediate PING after LOGIN
- [ ] Check ESP32 serial for error messages
- [ ] Compare with official Blynk cloud protocol
