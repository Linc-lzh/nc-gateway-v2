package nc.gateway.util;


public class Constant {

    //返回错误码定义
    //监控程序认为大于0 为请求是正常的

    final public static int RESP_CODE_SUCCESS = 0;
    final public static int RESP_CODE_LEAK_PARAM = -1;
    final public static int RESP_CODE_SERVER_ERROR = -2;
    final public static int RESP_CODE_NO_LOGIN = -3;
    final public static int RESP_CODE_TOKEN_EXCEPTION = -4;
    //json 字符串常量定义

    final public static String JSON_RESP_CODE = "respCode";
    final public static String JSON_ERROR_MSG = "errMsg";
    final public static String JSON_RESP_DATA = "respData";


    final public static String LOG_ID = "logId";
    final public static String LOG_STRING = "logstr";
    final public static String REQ_UID = "uid";
    final public static String CLIENT_IP = "clientIp";
    final public static String VERSION = "version";
    final public static String APP_TYPE = "appType";
    final public static String REQ_TOKEN = "token";

    final public static String TOKEN_MDS_STR = "WEREFMOEGMFWEG";

    final public static String FILE_TYPE = "file";
    final public static String JAR_TYPE = "jar";
}
