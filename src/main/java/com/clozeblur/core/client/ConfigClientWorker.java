package com.clozeblur.core.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.clozeblur.core.client.common.ConfigConst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yuanjx
 * <html>先加载公共配置,再加载定制化的配置文件</html>
 */
class ConfigClientWorker {

    private static final Logger logger = LoggerFactory.getLogger(ConfigClientWorker.class);

    private Map map = new HashMap();

    ConfigClientWorker(String configUris, String configBranch, RemoteConfig remoteConfig) {
        loadConfigAndSave(ConfigConst.CONFIG_COMMON, ConfigConst.MAIN_BRANCH, remoteConfig);
        loadConfigAndSave(configUris, configBranch, remoteConfig);
    }

    @SuppressWarnings("unchecked")
    Map<String, String> getMap() {
        return map;
    }

    /**
     * 对于所需配置文件的地址进行分段获取，并将返回的json字符串处理成map
     */
    private void loadConfigAndSave(String configUris, String configBranch, RemoteConfig remoteConfig) {
        if (StringUtils.isEmpty(configUris)) return;
        File tmpDir = new File(System.getProperty("java.io.tmpdir") + "/config-temp");
        if (!tmpDir.exists() && !tmpDir.mkdirs()) logger.error("创建临时目录失败,无法存储本地缓存配置文件");

        String configs = "";
        try {
            configs = remoteConfig.getConfigData(configUris.trim(), configBranch);
        } catch (Exception e) {
            logger.error("配置数据获取失败: {}", configUris);
        }
        if (!StringUtils.isEmpty(configs)) {
            loadEnvironment2Map(configs);
        }
    }

    /**
     * 将已封装好的configs中的配置数据键值对提取出来并写入本类map中
     */
    @SuppressWarnings("unchecked")
    private void loadEnvironment2Map(String configs) {
        Map eMap = JSON.parseObject(configs);
        String psStr = JSON.toJSONString(eMap.get("propertySources"));
        JSONArray psArray = JSON.parseArray(psStr);
        for (Object aPsArray : psArray) {
            String propertySource = JSON.toJSONString(aPsArray);
            Map sourceMap = JSON.parseObject(propertySource);
            String sourceJson = JSON.toJSONString(sourceMap.get("source"));
            JSONObject json = JSONObject.parseObject(sourceJson);
            map.putAll(json);
        }
    }

}
