#!/bin/bash
# Test script for registration with simplified input

BASE_URL="http://localhost:8081"

echo "=== Test 1: Register user with email and password only ==="
curl -X POST $BASE_URL/api/register \
  -H "Content-Type: application/json" \
  -d '{
    "email":"john@example.com",
    "pass":"mypassword123"
  }'
echo -e "\n"

echo "=== Test 2: Register user with custom appName ==="
curl -X POST $BASE_URL/api/register \
  -H "Content-Type: application/json" \
  -d '{
    "email":"alice@example.com",
    "pass":"pass456",
    "appName":"MyApp"
  }'
echo -e "\n"

echo "=== Test 3: Try to register duplicate (should fail) ==="
curl -X POST $BASE_URL/api/register \
  -H "Content-Type: application/json" \
  -d '{
    "email":"john@example.com",
    "pass":"different123"
  }'
echo -e "\n"

echo "=== Test 4: Try to register without email (should fail) ==="
curl -X POST $BASE_URL/api/register \
  -H "Content-Type: application/json" \
  -d '{
    "pass":"mypassword"
  }'
echo -e "\n"

echo "=== Test 5: Login with newly registered user ==="
curl -X POST $BASE_URL/api/login \
  -H "Content-Type: application/json" \
  -d '{
    "id":"john@example.com-Blynk",
    "pass":"mypassword123"
  }'
echo -e "\n"
