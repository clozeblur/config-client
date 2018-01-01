package com.clozeblur.core.client.redis;

import com.clozeblur.core.client.common.ConstPersist;

import java.util.ResourceBundle;

/**
 * @author clozeblur
 * <html>加载redis配置信息</html>
 */
public class RedisConfig {

    private RedisConfig() {}

    private static final ResourceBundle bundle = ResourceBundle.getBundle("core/config/client/config-redis");

    private static String nodes;

    private static Integer maxTotal;

    private static Integer minIdle;

    private static Integer maxIdle;

    private static Integer maxWaitMillis;

    private static Boolean testWhileIdle;

    private static Integer timeout;

    private static Integer maxRedirects;

    static {
        nodes = bundle.getString("core.config.client.redis." + ConstPersist.getEnv() + "-nodes");
        maxTotal = Integer.valueOf(bundle.getString("core.config.client.redis.maxTotal"));
        minIdle = Integer.valueOf(bundle.getString("core.config.client.redis.minIdle"));
        maxIdle = Integer.valueOf(bundle.getString("core.config.client.redis.maxIdle"));
        maxWaitMillis = Integer.valueOf(bundle.getString("core.config.client.redis.maxWaitMillis"));
        testWhileIdle = Boolean.valueOf(bundle.getString("core.config.client.redis.testWhileIdle"));
        timeout = Integer.valueOf(bundle.getString("core.config.client.redis.timeout"));
        maxRedirects = Integer.valueOf(bundle.getString("core.config.client.redis.maxRedirects"));
    }

    public static String getNodes() {
        return nodes;
    }

    public static Integer getMaxTotal() {
        return maxTotal;
    }

    public static Integer getMinIdle() {
        return minIdle;
    }

    public static Integer getMaxIdle() {
        return maxIdle;
    }

    public static Integer getMaxWaitMillis() {
        return maxWaitMillis;
    }

    public static Boolean getTestWhileIdle() {
        return testWhileIdle;
    }

    public static Integer getTimeout() {
        return timeout;
    }

    public static Integer getMaxRedirects() {
        return maxRedirects;
    }
}
