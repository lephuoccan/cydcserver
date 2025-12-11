package cloud.cydc.blynk;

/**
 * Blynk Protocol Constants
 * Compatible with Blynk Library v0.6.1
 */
public class BlynkProtocol {
    // Commands
    public static final byte BLYNK_CMD_RESPONSE = 0;
    public static final byte BLYNK_CMD_REGISTER = 1;
    public static final byte BLYNK_CMD_LOGIN = 2;
    public static final byte BLYNK_CMD_SAVE_PROF = 3;
    public static final byte BLYNK_CMD_LOAD_PROF = 4;
    public static final byte BLYNK_CMD_GET_TOKEN = 5;
    public static final byte BLYNK_CMD_PING = 6;
    public static final byte BLYNK_CMD_ACTIVATE = 7;
    public static final byte BLYNK_CMD_DEACTIVATE = 8;
    public static final byte BLYNK_CMD_REFRESH = 9;
    public static final byte BLYNK_CMD_GET_GRAPH_DATA = 10;
    public static final byte BLYNK_CMD_GET_GRAPH_DATA_RESPONSE = 11;
    
    public static final byte BLYNK_CMD_TWEET = 12;
    public static final byte BLYNK_CMD_EMAIL = 13;
    public static final byte BLYNK_CMD_NOTIFY = 14;
    public static final byte BLYNK_CMD_BRIDGE = 15;
    public static final byte BLYNK_CMD_HARDWARE_SYNC = 16;
    public static final byte BLYNK_CMD_INTERNAL = 17;
    public static final byte BLYNK_CMD_SMS = 18;
    public static final byte BLYNK_CMD_PROPERTY = 19;
    public static final byte BLYNK_CMD_HARDWARE = 20;
    
    public static final byte BLYNK_CMD_CREATE_DASH = 21;
    public static final byte BLYNK_CMD_LOGIN_2 = 29;  // Extended LOGIN command
    public static final byte BLYNK_CMD_SAVE_DASH = 22;
    public static final byte BLYNK_CMD_DELETE_DASH = 23;
    public static final byte BLYNK_CMD_LOAD_PROFILE_GZIPPED = 24;
    public static final byte BLYNK_CMD_SHARING = 25;
    public static final byte BLYNK_CMD_ADD_PUSH_TOKEN = 26;
    
    public static final byte BLYNK_CMD_REDIRECT = 41;
    public static final byte BLYNK_CMD_DEBUG_PRINT = 55;
    
    // Status codes
    public static final short BLYNK_SUCCESS = 200;
    public static final short BLYNK_QUOTA_LIMIT_EXCEPTION = 1;
    public static final short BLYNK_ILLEGAL_COMMAND = 2;
    public static final short BLYNK_NOT_REGISTERED = 3;
    public static final short BLYNK_ALREADY_REGISTERED = 4;
    public static final short BLYNK_NOT_AUTHENTICATED = 5;
    public static final short BLYNK_NOT_ALLOWED = 6;
    public static final short BLYNK_NO_ACTIVE_DASHBOARD = 7;
    public static final short BLYNK_INVALID_TOKEN = 8;
    public static final short BLYNK_ILLEGAL_COMMAND_BODY = 9;
    public static final short BLYNK_GET_GRAPH_DATA_EXCEPTION = 10;
    public static final short BLYNK_NO_DATA_EXCEPTION = 11;
    public static final short BLYNK_DEVICE_WENT_OFFLINE = 12;
    public static final short BLYNK_SERVER_EXCEPTION = 13;
    
    public static final short BLYNK_NTF_INVALID_BODY = 14;
    public static final short BLYNK_NTF_NOT_AUTHORIZED = 15;
    public static final short BLYNK_NTF_EXCEPTION = 16;
    
    public static final short BLYNK_TIMEOUT = 17;
    public static final short BLYNK_NO_LOGIN_TIMEOUT = 18;
    public static final short BLYNK_INVALID_COMMAND_FORMAT = 19;
    
    // Protocol constants
    public static final int BLYNK_HEADER_SIZE = 5;
    public static final int BLYNK_MAX_BODY_SIZE = 1024;
    
    public static String getCommandName(byte cmd) {
        switch (cmd) {
            case BLYNK_CMD_RESPONSE: return "RESPONSE";
            case BLYNK_CMD_REGISTER: return "REGISTER";
            case BLYNK_CMD_LOGIN: return "LOGIN";
            case BLYNK_CMD_PING: return "PING";
            case BLYNK_CMD_HARDWARE: return "HARDWARE";
            case BLYNK_CMD_HARDWARE_SYNC: return "HARDWARE_SYNC";
            case BLYNK_CMD_INTERNAL: return "INTERNAL";
            case BLYNK_CMD_BRIDGE: return "BRIDGE";
            case BLYNK_CMD_TWEET: return "TWEET";
            case BLYNK_CMD_EMAIL: return "EMAIL";
            case BLYNK_CMD_NOTIFY: return "NOTIFY";
            case BLYNK_CMD_PROPERTY: return "PROPERTY";
            case BLYNK_CMD_ACTIVATE: return "ACTIVATE";
            case BLYNK_CMD_DEACTIVATE: return "DEACTIVATE";
            case BLYNK_CMD_REFRESH: return "REFRESH";
            default: return "UNKNOWN(" + cmd + ")";
        }
    }
}
