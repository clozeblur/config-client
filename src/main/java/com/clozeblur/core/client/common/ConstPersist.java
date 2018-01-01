package com.clozeblur.core.client.common;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author clozeblur
 * <html>暂存在内存中的配置信息,同时提供channels应用于刷新配置的消息</html>
 */
public class ConstPersist {

    private ConstPersist() {}

    private static String env;

    private static int connectTimeout;

    private static int readTimeout;

    private static String configUris;

    private static String configBranch;

    public static String getEnv() {
        return env;
    }

    public static void setEnv(String env) {
        ConstPersist.env = env;
    }

    public static int getConnectTimeout() {
        return connectTimeout;
    }

    public static void setConnectTimeout(int connectTimeout) {
        ConstPersist.connectTimeout = connectTimeout;
    }

    public static int getReadTimeout() {
        return readTimeout;
    }

    public static void setReadTimeout(int readTimeout) {
        ConstPersist.readTimeout = readTimeout;
    }

    public static String getConfigUris() {
        return configUris;
    }

    public static void setConfigUris(String configUris) {
        ConstPersist.configUris = configUris;
    }

    public static String getConfigBranch() {
        return configBranch;
    }

    public static void setConfigBranch(String configBranch) {
        ConstPersist.configBranch = configBranch;
    }

    public static List<String> getChannels() {
        if (StringUtils.isEmpty(configBranch) || StringUtils.isEmpty(env) || StringUtils.isEmpty(configUris)) {
            return Collections.emptyList();
        }
        List<String> channels = new ArrayList<>();
        String[] files = configUris.trim().split(",");
        for (String filename : files) {
            String shortName = filename.endsWith("-" + env) ? filename.substring(0, filename.lastIndexOf("-" + env)) : filename;
            String channel = configBranch + ":" + env + ":" + shortName;
            channels.add(channel);
        }
        channels.add(ConfigConst.MAIN_BRANCH + ":" + env + ":" + ConfigConst.CONFIG_COMMON);
        return channels;
    }
}
