package cloud.cydc;

import cloud.cydc.service.DeviceInfoService;
import cloud.cydc.util.TokenValidator;
import cloud.cydc.model.DeviceInfo;
import cloud.cydc.db.DeviceInfoDao;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.HashMap;

public class TestTokenValidator {
    private TokenValidator tokenValidator;
    private DeviceInfoService deviceService;
    private DeviceInfoDao dao;

    @BeforeEach
    public void setup() {
        dao = mock(DeviceInfoDao.class);
        deviceService = new DeviceInfoService(dao);
        tokenValidator = new TokenValidator(deviceService);
    }

    @Test
    public void testTokenValidation() {
        // Test case: Token format is userId-dashId-deviceId-randomPart
        String userId = "apitest@example.com";
        long dashId = 100L;
        long devId = 1L;
        String randomPart = "00eab6c272d241659af31c7b48092bce";
        String fullToken = userId + "-" + dashId + "-" + devId + "-" + randomPart;
        
        // Create a DeviceInfo with the token
        DeviceInfo deviceInfo = new DeviceInfo(
            devId, "TestDevice", "ESP32", fullToken,
            "Espressif", "WiFi", null, 0L, 0L, 0L, 0L, null,
            new HashMap<>(), false
        );
        
        // Mock the DAO to return the token
        when(dao.findJsonById(userId, dashId, devId))
            .thenReturn("{\"id\":1,\"name\":\"TestDevice\",\"boardType\":\"ESP32\",\"token\":\"" + fullToken + "\"}");
        
        // Test validation
        System.out.println("Testing token: " + fullToken);
        boolean result = tokenValidator.validateToken(fullToken);
        System.out.println("Validation result: " + result);
        
        assertTrue(result, "Token should be valid");
    }

    @Test
    public void testInvalidToken() {
        String userId = "apitest@example.com";
        long dashId = 100L;
        long devId = 1L;
        String invalidToken = "invalid-token-format";
        
        System.out.println("Testing invalid token: " + invalidToken);
        boolean result = tokenValidator.validateToken(invalidToken);
        System.out.println("Validation result: " + result);
        
        assertFalse(result, "Invalid token should fail validation");
    }

    @Test
    public void testExtractTokenInfo() {
        String userId = "apitest@example.com";
        long dashId = 100L;
        long devId = 1L;
        String randomPart = "00eab6c272d241659af31c7b48092bce";
        String fullToken = userId + "-" + dashId + "-" + devId + "-" + randomPart;
        
        // Mock the DAO
        when(dao.findJsonById(userId, dashId, devId))
            .thenReturn("{\"id\":1,\"name\":\"TestDevice\",\"boardType\":\"ESP32\",\"token\":\"" + fullToken + "\"}");
        
        String[] info = tokenValidator.extractTokenInfo(fullToken);
        
        assertNotNull(info, "Token info should be extracted");
        assertEquals(3, info.length, "Should return [userId, dashId, devId]");
        assertEquals(userId, info[0], "UserId should match");
        assertEquals(String.valueOf(dashId), info[1], "DashId should match");
        assertEquals(String.valueOf(devId), info[2], "DevId should match");
    }
}
