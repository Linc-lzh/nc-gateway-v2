package nc.gateway.filter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class CorsFilter implements Filter {
    private static final String ALLOWED_HOST = "http://m.naixuejiaoyu.com";

    private static final Set<String> ALLOWED_HOST_SET = new HashSet<String>();

    static {
        ALLOWED_HOST_SET.add("http://api.naixuejiaoyu.com");
        ALLOWED_HOST_SET.add(ALLOWED_HOST);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest)servletRequest;
        HttpServletResponse httpResp = (HttpServletResponse)servletResponse;

        httpResp.addHeader("Access-Control-Allow-Credentials", "true");
        if (httpReq.getMethod().equalsIgnoreCase("options")) {
            // 探测请求
            String originalUrl = httpReq.getHeader("Origin");
            if (null != originalUrl && (ALLOWED_HOST_SET.contains(originalUrl))) { // 跨域白名单
                httpResp.addHeader("Access-Control-Allow-Origin", httpReq.getHeader("Origin"));
            } else {
                httpResp.addHeader("Access-Control-Allow-Origin", ALLOWED_HOST);
            }
            httpResp.addHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
            httpResp.addHeader("Access-Control-Max-Age", "3600");
            httpResp.addHeader("Content-Type", "text/plain");
            return;
        } else {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    @Override
    public void destroy() {

    }
}
