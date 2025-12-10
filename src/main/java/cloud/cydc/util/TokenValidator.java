package cloud.cydc.util;

import cloud.cydc.service.DeviceInfoService;

/**
 * Token validation utilities for device and hardware authentication.
 */
public class TokenValidator {
    private final DeviceInfoService deviceService;

    public TokenValidator(DeviceInfoService deviceService) {
        this.deviceService = deviceService;
    }

    /**
     * Validates a device token. Returns true if valid, false otherwise.
     * Token format: userId-dashId-deviceId-randomPart (all parts required)
     * Note: userId can contain hyphens, so we need to parse from the end
     */
    public boolean validateToken(String token) {
        if (token == null || token.isEmpty()) {
            System.out.println("[TokenValidator] Token is null or empty");
            return false;
        }

        try {
            // Parse from the end: find last 3 numeric segments
            String[] parts = token.split("-");
            System.out.println("[TokenValidator] Token parts count: " + parts.length + ", token: " + token);
            if (parts.length < 4) {
                System.out.println("[TokenValidator] Not enough parts (need 4+, got " + parts.length + ")");
                return false;
            }

            // Last part is the random token portion
            String randomPart = parts[parts.length - 1];
            // Second to last is deviceId
            long devId = Long.parseLong(parts[parts.length - 2]);
            // Third to last is dashId
            long dashId = Long.parseLong(parts[parts.length - 3]);
            // Everything before that is userId
            String userId = String.join("-", java.util.Arrays.copyOf(parts, parts.length - 3));

            // Reconstruct expected token format
            String expectedToken = userId + "-" + dashId + "-" + devId + "-" + randomPart;
            System.out.println("[TokenValidator] Parsed - userId: " + userId + ", dashId: " + dashId + ", devId: " + devId);
            System.out.println("[TokenValidator] Expected token: " + expectedToken);
            
            // Get stored token and compare entire token (not just random part)
            String storedToken = deviceService.findTokenByDeviceId(userId, dashId, devId);
            System.out.println("[TokenValidator] Stored token: " + storedToken);
            boolean result = storedToken != null && storedToken.equals(expectedToken);
            System.out.println("[TokenValidator] Validation result: " + result);
            return result;
        } catch (Exception e) {
            System.out.println("[TokenValidator] Exception: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Extracts userId, dashId, deviceId from token.
     * Returns array [userId, dashId, deviceId] or null if invalid.
     * Note: userId can contain hyphens, so we parse from the end
     */
    public String[] extractTokenInfo(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }

        try {
            String[] parts = token.split("-");
            if (parts.length < 4) {
                return null;
            }

            // Last part is the random token portion
            String randomPart = parts[parts.length - 1];
            // Second to last is deviceId
            long devId = Long.parseLong(parts[parts.length - 2]);
            // Third to last is dashId
            long dashId = Long.parseLong(parts[parts.length - 3]);
            // Everything before that is userId
            String userId = String.join("-", java.util.Arrays.copyOf(parts, parts.length - 3));

            // Reconstruct expected token format
            String expectedToken = userId + "-" + dashId + "-" + devId + "-" + randomPart;
            
            // Verify token matches stored token for device
            String storedToken = deviceService.findTokenByDeviceId(userId, dashId, devId);
            if (storedToken != null && storedToken.equals(expectedToken)) {
                return new String[]{userId, String.valueOf(dashId), String.valueOf(devId)};
            }
        } catch (Exception e) {
            // Fall through
        }
        return null;
    }
}
