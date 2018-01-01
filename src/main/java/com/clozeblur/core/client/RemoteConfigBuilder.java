package com.clozeblur.core.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author clozeblur
 * <html>加载配置中心服务端域名</html>
 */
class RemoteConfigBuilder {

    private String env;

    RemoteConfigBuilder(String env){
        this.env = env;
    }

    RemoteConfig build() {
        if (this.env == null || this.env.trim().isEmpty()) {
            throw new IllegalArgumentException("环境参数非法：env=" + env);
        }
        // 读取配置文件
        String classpathProperties = "core/config/client/config-client.properties";
        // classloader是从classpath路径开始查找资源，class则会预处理下name
        Properties loginProperties = new Properties();
        // fail fast
        try (InputStream is =
                     Thread.currentThread().getContextClassLoader().getResourceAsStream(classpathProperties)) {
            if (is == null) {
                throw new IllegalArgumentException("找不到登录配置文件，请确认环境参数正确：env=" + this.env);
            }
            loginProperties.load(is);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        //配置中心地址
        String prePath = loginProperties.getProperty(this.env + ".core.config.client.host");

        return new RemoteConfig(prePath);
    }

}
