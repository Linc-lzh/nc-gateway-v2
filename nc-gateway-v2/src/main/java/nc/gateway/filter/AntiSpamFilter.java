package nc.gateway.filter;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import nc.gateway.util.E2s;
import nc.gateway.util.RedisUtil;
import nc.gateway.util.RequestUtils;
import nc.gateway.util.ResponseUtils;
import redis.clients.jedis.Jedis;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AntiSpamFilter implements Filter {

    private static volatile Set<String> deviceIdSet = Sets.newHashSet();
    private static volatile Set<String> ipSet = Sets.newHashSet();//从缓存内获取的封禁IP
    private static volatile Set<String> ipSet_1 = Sets.newHashSet();//从antispider服务获取的封禁IP
    private static volatile Set<String> uidSet = Sets.newHashSet();
    private static final long PERIOD = 30L * 1000;
    private static final long DELAY = 0;
    public AntiSpamFilter() {
        initCache();
    }

    public void initCache() {

        /* 单线程后台定期更新缓存 */
        ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
        service.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                log.debug("refresh anti spam cache");
                refreshCaches();
            }
        }, DELAY, PERIOD, TimeUnit.MILLISECONDS);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    public Boolean isContainsDeviceId(String deviceId) {
        return !(deviceId == null || deviceId.trim().equals("")) &&
                deviceIdSet.contains(deviceId);
    }

    public Boolean isContainsIp(String logStr, String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        if (ipSet.contains(ip)) {
            log.info(logStr + " k=s act=entry_forbid_ip from=redis forbidIP=" + ip);
            return true;
        }
        if (ipSet_1.contains(ip)) {
            log.info(logStr + " k=s act=entry_forbid_ip from=antispider forbidIP=" + ip);
            return true;
        }
        return false;
    }

    public Boolean isContainsUid(String uid) {
        if (uid == null || uid.trim().equals("")) {
            return false;
        }
        if (uidSet.contains(uid)) {
            return true;
        }
        return false;
    }

    public static void refreshCaches() {
        /* 类加载时更新缓存 */
        deviceIdSet = getUpdateCache("spam:deviceIds");
        ipSet = getUpdateCache("spam:ips");
        uidSet = getUpdateCache("spam:uids");
    }

    private static Set<String> getUpdateCache(String key) {
        String logStr = "antispiderutil update " + key;
        Jedis jedis = null;
        String redisKey = key;
        Set<String> container = new HashSet<String>();
        try {
            Stopwatch stopwatch = Stopwatch.createStarted();
            log.info(logStr, " begin key=", redisKey);
            jedis = RedisUtil.getJedis();
            if (jedis != null) {
                container = jedis.smembers(redisKey);
                log.info(logStr, " size=", container == null ? 0 : container.size());
            }
            log.info(logStr, " finish, took:" + stopwatch.toString());
        } catch (Exception ex) {
            log.error(logStr, " act=updateCache_error, ex=", E2s.exception2String(ex));
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return container;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest)servletRequest;
        HttpServletResponse httpResp = (HttpServletResponse)servletResponse;

        String ip = RequestUtils.getRemoteAddr(httpReq);
        Cookie[] cookies = httpReq.getCookies();
        String uid = null;
        String deviceId = "";
        if (cookies != null && cookies.length > 0) {
            log.info(" act=PpuCheckFilter.doFilter ", " cookies 为空 >>>", " <<<<");
            for (Cookie cookie : cookies) {
                if ("uid".equals(cookie.getName())) {
                    uid = cookie.getValue();
                }
                if ("deviceId".equals(cookie.getName())) {
                    deviceId = cookie.getValue();
                }
            }
        }
        //反作弊拦截
        if (isContainsIp("AntispamFilter", ip) || isContainsDeviceId(deviceId) || isContainsUid(uid)) {
            try {
                ResponseUtils.buildErrorResult(httpReq, httpResp, "身份校验失败，请重试或重新登录");
            } catch (IOException e) {
                throw e;
            }
            return;
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {

    }
}
