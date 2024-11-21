package nc.gateway.util;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

@Slf4j
public class ResponseUtils {
    private String content = "";

    public ResponseUtils(String content, String logStr, int level) {
        this.content = content;
        StringBuffer logBuffer = new StringBuffer();
        logBuffer.append(logStr).append(" len=").append(content.length()).append(" content=").append(content);
        switch (level) {
            case 1:
                log.debug(logBuffer.toString());
                break;
            case 2:
                log.info(logBuffer.toString());
                break;
            case 3:
                log.error(logBuffer.toString(), new Exception());
                break;
            case 4:
                log.error(logBuffer.toString(), new Exception());
            default:
                log.debug(logBuffer.toString());
                break;
        }

    }

    public static void writeJson(HttpServletResponse resp, Object object) {
        PrintWriter out = null;
        try {
            // 设定类容为json的格式
            resp.setContentType("application/json;charset=UTF-8");
            out = resp.getWriter();
            // 写到客户端
            out.write(JSONObject.toJSONString(object));
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public static void writeText(HttpServletResponse resp, String content) {
        PrintWriter out = null;
        try {
            // 设定类容为json的格式
            resp.setContentType("text/plain;charset=UTF-8");
            out = resp.getWriter();
            // 写到客户端
            out.write(content);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public static void writePic(HttpServletResponse resp, byte[] stream) {
        OutputStream out = null;
        try {
            // 设定类容为json的格式
            resp.setContentType("image/jpeg");
            out = resp.getOutputStream();
            // 写到客户端
            out.write(stream);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void responseMethod(int code, Object object, String errorMsg, HttpServletRequest request, HttpServletResponse response) {
        StringBuilder json = new StringBuilder();
        String callback = request.getParameter("callback");
        if (StringUtils.isNotEmpty(callback)) {
            json.append(request.getParameter("callback")).append("(");
            json.append("{").append("\"respCode\":").append(code).append(",\"respData\":").append(JSONObject.toJSONString(object)).append(",\"errMsg\":").append("\"").append(StringUtils.isEmpty(errorMsg) ? "" : errorMsg).append("\"").append("})");
            response.setStatus(500);
            writeText(response, json.toString());
        } else {
            writeJson(response, object);
        }
    }

    public static void buildErrorResult(HttpServletRequest httpReq, HttpServletResponse httpRes, String errorMsg)
            throws IOException {
        ResponseUtils.responseMethod(-1, null, errorMsg, httpReq, httpRes);
    }

    public String getContent() {
        return content;
    }
}
