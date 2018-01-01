package com.clozeblur.core.client.redis;

import com.alibaba.fastjson.JSONObject;
import com.clozeblur.core.client.ConfigClientConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * @author clozeblur
 * <html>处理消息</html>
 */
public class Receiver {

    private static final Logger logger = LoggerFactory.getLogger(Receiver.class);

    @Autowired
    private ConfigClientConfigurer configurer;

    public Receiver(ConfigClientConfigurer configurer) {
        this.configurer = configurer;
    }

    @SuppressWarnings("unchecked")
    public void receiveMessage(String message) {
        try {
            Object o = JSONObject.parse(message);
            if (o != null) {
                /*
                 * 这里可以做更多的扩展
                 */
                Map<String, String> content = (Map<String, String>) ((Map<String, Object>) o).get("content");
                configurer.refresh(content);
            }
        } catch (Exception e) {
            logger.warn("配置信息更新推送读取异常...message: {}", message);
        }
    }
}
