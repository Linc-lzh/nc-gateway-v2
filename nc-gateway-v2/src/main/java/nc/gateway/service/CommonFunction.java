package nc.gateway.service;

import nc.gateway.util.Constant;
import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.util.Map;

@Component
public class CommonFunction {

    private String authTokenMd5Str = "xxx";
    public String create(String inStr) {
        String valueTokenMd5Str = authTokenMd5Str;

        if(!"".equals(inStr)){
            String fStr = inStr.substring(0, 1);
            String lStr = inStr.substring(1, inStr.length());
            return string2Md5(fStr + valueTokenMd5Str + lStr);
        }
        return null;
    }

    public boolean checkToken(HttpServletRequest request, Long userId) {
        String token = getToken(request);
        if (!"".equals(token)) {
            String authenticatedToken = create(userId + "");
            if (authenticatedToken != null && authenticatedToken.equals(token)) {
                return true;
            }
        }
        return false;
    }

    public String getToken(HttpServletRequest request) {
        String tokenValue = "";
        try {
            //通过参数获取token
            if(null != request.getParameter(Constant.REQ_TOKEN) && !request.getParameter(Constant.REQ_TOKEN).equals("")){
                tokenValue = request.getParameter(Constant.REQ_TOKEN);
            }else if(request.getCookies() != null && request.getCookies().length > 0){//通过cookie获取token
                Cookie[] cookies = request.getCookies();
                for (Cookie cookie : cookies) {
                    if (Constant.REQ_TOKEN.equals(cookie.getName())) {
                        tokenValue = cookie.getValue();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tokenValue;
    }


    public boolean checkParam(Map<String, String> param){
        return true;
    }

    public String string2Md5(String inStr) {
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
            return "";
        }
        char[] charArray = inStr.toCharArray();
        byte[] byteArray = new byte[charArray.length];

        for (int i = 0; i < charArray.length; i++) {
            byteArray[i] = (byte) charArray[i];
        }
        byte[] md5Bytes = md5.digest(byteArray);
        StringBuffer hexValue = new StringBuffer();
        for (int i = 0; i < md5Bytes.length; i++) {
            int val = ((int) md5Bytes[i]) & 0xff;
            if (val < 16) {
                hexValue.append("0");
            }
            hexValue.append(Integer.toHexString(val));
        }
        return hexValue.toString();
    }
}
