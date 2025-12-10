# Registration Enhancement Summary

## Changes Made

### 1. UsersService - New simplified registration method
- Added `register(email, password, appName)` method that:
  - Checks if user already exists → throws exception if duplicate
  - Auto-generates userId as `email-appName`
  - Sets `isSuperAdmin = false` always
  - Sets default appName to "Blynk" if not provided
  - Returns the generated userId

### 2. HttpRequestHandler - Updated /api/register endpoint
- Now accepts simplified JSON: `{"email":"...", "pass":"...", "appName":"..."}`
- email and pass are required, appName is optional
- Returns 201 CREATED with userId on success
- Returns 409 CONFLICT if user already exists
- Returns 400 BAD_REQUEST if email or password missing

### 3. UsersDao - Already had existsById() method
- No changes needed, already supports existence check

## API Example

**Request:**
```bash
curl -X POST http://localhost:8081/api/register \
  -H "Content-Type: application/json" \
  -d '{
    "email":"user@example.com",
    "pass":"password123",
    "appName":"MyApp"
  }'
```

**Success Response (201):**
```json
{"status":"ok","userId":"user@example.com-MyApp"}
```

**Duplicate User (409):**
```json
{"error":"User already exists"}
```

**Missing Fields (400):**
```json
{"error":"email and pass are required"}
```

## Test Scripts Provided

- `test-registration.sh` - Bash script with 5 test cases
- `test-registration.ps1` - PowerShell script with 5 test cases

Both test:
1. Register with email + password
2. Register with custom appName
3. Duplicate registration (should fail)
4. Missing email (should fail)
5. Login with new user

## Build Status
✅ All tests pass: `mvn -DskipTests package`
