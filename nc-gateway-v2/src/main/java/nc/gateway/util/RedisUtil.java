package nc.gateway.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.annotation.PostConstruct;

@Slf4j
@Component
public class RedisUtil {

    @Value("${jedis.config.host}")
    private String host;

    @Value("${jedis.config.port}")
    private int port;

    @Value("${jedis.config.timeout}")
    private int timeout;

    @Value("${jedis.config.password}")
    private String password;
    private static JedisPool jedisPool = null;

    @PostConstruct
    public void init(){
        if(null == jedisPool){
            JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
            jedisPoolConfig.setMaxTotal(20);
            jedisPoolConfig.setMaxIdle(10);
            jedisPoolConfig.setMinIdle(5);
            if(StringUtils.isEmpty(password))
                password = null;
            jedisPool = new JedisPool(jedisPoolConfig, host, port, timeout, password);
        }
        log.info("act=end_init_redis_pool endTime=" + System.currentTimeMillis());
    }

    public RedisUtil() {

    }

    public static Jedis getJedis() throws Exception{
        if(null != jedisPool){
            return jedisPool.getResource();
        }
        throw  new Exception("redis 连接池是null!");
    }
}
