package nc.gateway.filter;


import lombok.extern.slf4j.Slf4j;
import nc.gateway.service.CommonFunction;
import nc.gateway.util.Constant;
import nc.gateway.util.E2s;
import nc.gateway.util.RequestUtils;
import nc.gateway.util.ResponseUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Slf4j
public class PpuCheckFilter implements Filter {

    @Autowired
    CommonFunction commonFunction;
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest)servletRequest;
        HttpServletResponse httpResp = (HttpServletResponse)servletResponse;
        // 对于register、login等非登录要求的接口直接放过
        if (httpReq.getRequestURI().contains("transfer")) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        //获取Seesion
        Cookie[] cookies = httpReq.getCookies();
        String session = "";
        Long uid = null;
        if (cookies != null && cookies.length > 0) {
            log.info(" act=PpuCheckFilter.doFilter ", " cookies 为空 >>>", " <<<<");
            for (Cookie cookie : cookies) {
                if ("uid".equals(cookie.getName())) {
                    uid = Long.valueOf(cookie.getValue());
                }
                if ("session".equals(cookie.getName())) {
                    session = cookie.getValue();
                }
            }
        }

        boolean checkResult = checkPpu(session, uid, httpReq, httpResp);

        long logid = genLogId(uid); //随机数
        //产生logstr
        StringBuilder logsb = new StringBuilder()
                .append(" logid=").append(logid)
                .append(" ip=").append(RequestUtils.getRemoteAddr(httpReq))
                .append(" cmd=").append(httpReq.getRequestURI());
        String logStr = logsb.toString();
        httpReq.setAttribute(Constant.LOG_STRING, logStr);
        httpReq.setAttribute(Constant.LOG_ID, logid);

        if (!checkResult) {
            try {
                ResponseUtils.buildErrorResult(httpReq, httpResp, "身份校验失败，请重试或重新登录");
            } catch (IOException e) {
                throw e;
            }
            return;
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    private boolean checkPpu(String jwt, Long uid, HttpServletRequest request, HttpServletResponse response) {
        if (StringUtils.isBlank(jwt)) {
            log.info(" act=PpuCheckFilter.checkPPU ", " jwt 是空的，校验不通过 >>>", " <<<<");
            return false;
        }
        try {
            //token验证  防止泄露，demo 解决
            boolean result =  commonFunction.checkToken(request, uid);
            if (!result){
                return false;
            }
            String newPpuStr = commonFunction.create(uid + "");
            if (StringUtils.isNotEmpty(newPpuStr)) {
                // 刷新cookie
                Cookie cookie = new Cookie("PPU", newPpuStr);
                cookie.setDomain("naixuejiaoyu.com");
                cookie.setPath("/");
                cookie.setMaxAge(86400 * 30);// ppu 种在 cookie 里面 30 天 和 entry 保持一致
                response.addCookie(cookie);
            }
            request.setAttribute(Constant.REQ_UID, uid);
            return true;
        } catch (Exception e) {
            log.error(" act=PpuCheckFilter.checkPPU ", E2s.exception2String(e));
            return false;
        }
    }

    private static long genLogId(long param) {
        long nowTime = System.currentTimeMillis();
        return nowTime & 0x7FFFFFFF | (param >> 8 & 65535L) << 47;
    }

    @Override
    public void destroy() {

    }
}
