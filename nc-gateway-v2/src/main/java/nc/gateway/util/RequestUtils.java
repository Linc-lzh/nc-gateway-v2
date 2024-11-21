package nc.gateway.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.ServletRequestDataBinder;

import javax.servlet.http.HttpServletRequest;

@Slf4j
public class RequestUtils {

    /**
     * 将Request参数 封装成对象 当出错 返回null
     *
     * @param request HttpServletRequest
     * @param clazz   Class
     * @param <T>     T
     * @return T
     */
    public static <T> T convertHttpParam(HttpServletRequest request, Class<T> clazz) {
        T bean = null;
        try {
            bean = clazz.newInstance();
            ServletRequestDataBinder servletBinder = new ServletRequestDataBinder(bean);
            servletBinder.bind(request);
        } catch (Exception e) {
            log.error("convert Bean from Request fail " + clazz.getName(), e.getMessage());
        }
        return bean;
    }

    public static String getRemoteAddr(HttpServletRequest httprequest) {
        String ip = httprequest.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = httprequest.getHeader("Proxy-Client-IP");
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = httprequest.getHeader("WL-Proxy-Client-IP");
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = httprequest.getRemoteAddr();
        }

        if (ip.contains(",")) {
            ip = ip.split(",")[0];
        }

        return ip;
    }

}
