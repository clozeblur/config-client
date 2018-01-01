package com.clozeblur.core.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.clozeblur.core.client.common.ConstPersist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * @author clozeblur
 * <html>手动获取配置信息的功能类,根据需求自行使用</html>
 */
public class ConfigClientInjector {

    private static final Logger logger = LoggerFactory.getLogger(ConfigClientInjector.class);

    private String env = ConstPersist.getEnv();

    private int connectTimeout = ConstPersist.getConnectTimeout();

    private int readTimeout = ConstPersist.getReadTimeout();

    private String configBranch = ConstPersist.getConfigBranch();

    private String localConfigUris = ConstPersist.getConfigUris();

    /**
     * 根据传入的远程配置路径手动获取其json字符串数组
     * 如果是分段获取，url连接以","为中间符号
     * configBranch默认为本项目的值，亦提供传入不同configBranch值
     */
    public List<String> getConfigJsonList(String configUris) {
        return getConfigJsonList(configUris, configBranch);
    }

    public List<String> getConfigJsonList(String configUris, String configBranch) {
        RemoteConfig remoteConfig = new RemoteConfigBuilder(env).build().initConfig(env, connectTimeout, readTimeout);
        String configs;
        try {
            configs = remoteConfig.getConfigData(configUris.trim(), configBranch);
        } catch (Exception e) {
            logger.error("获取配置信息失败");
            return null;
        }
        if (StringUtils.isEmpty(configs)) {
            return new ArrayList<>();
        }
        return getListFromJson(configs);
    }

    public Object getInstance(String configUris, String configBranch, Class clazz)
            throws IntrospectionException, IllegalAccessException, InstantiationException, InvocationTargetException {
        Object obj = clazz.newInstance();
        Map<String, String> localConfigMap = getConfigMap(configUris, configBranch);
        BeanInfo beanInfo = Introspector.getBeanInfo(clazz);
        PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
        for (PropertyDescriptor descriptor : propertyDescriptors) {
            String propertyName = descriptor.getName();
            if (localConfigMap.containsKey(propertyName)) {
                Object value = localConfigMap.get(propertyName);
                try {
                    Object[] args = new Object[1];
                    args[0] = value;
                    descriptor.getWriteMethod().invoke(obj, args);
                } catch (Exception e) {
                    logger.error("属性赋值失败: {}", propertyName);
                }
            }
        }
        return obj;
    }

    /**
     * 根据指定键值从默认配置路径中获取对应value值
     */
    public String getValue(String key) {
        return getValue(key, localConfigUris);
    }

    /**
     * 根据指定键值与路径获取配置信息对应的value值
     */
    public String getValue(String key, String fromUris) {
        return getValue(key, fromUris, configBranch);
    }

    public String getValue(String key, String fromUris, String configBranch) {
        List<String> jsonList = getConfigJsonList(fromUris, configBranch);
        String value = null;
        for (String jsonStr : jsonList) {
            JSONObject json = JSON.parseObject(jsonStr);
            if (json.containsKey(key)) {
                value = String.valueOf(json.get(key));
            }
        }
        return value;
    }

    /**
     * 根据指定路径获取Map格式数据
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> getConfigMap(String configUris) {
        return getConfigMap(configUris, configBranch);
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> getConfigMap(String configUris, String configBranch) {
        List<String> jsonList = getConfigJsonList(configUris, configBranch);
        Map<String, String> configMap = new HashMap<>();
        if (jsonList.isEmpty()) return configMap;
        for (String jsonStr : jsonList) {
            JSONObject json = JSON.parseObject(jsonStr);
            configMap.putAll((Map) json);
        }
        return configMap;
    }

    /**
     * 根据指定路径获取Properties格式数据
     */
    public Properties getConfigProperties(String configUris) {
        return getConfigProperties(configUris, configBranch);
    }

    public Properties getConfigProperties(String configUris, String configBranch) {
        Map map = getConfigMap(configUris, configBranch);
        Properties properties = new Properties();
        properties.putAll(map);
        return properties;
    }

    /**
     * 对json字符串进行处理，得到map格式字符串
     */
    private List<String> getListFromJson(String configs) {
        List<String> jsonList = new ArrayList<>();
        Map eMap = JSON.parseObject(configs);
        String psStr = JSON.toJSONString(eMap.get("propertySources"));
        JSONArray psArray = JSON.parseArray(psStr);
        for (Object aPsArray : psArray) {
            String propertySource = JSON.toJSONString(aPsArray);
            Map sourceMap = JSON.parseObject(propertySource);
            String sourceJson = JSON.toJSONString(sourceMap.get("source"));
            JSONObject json = JSONObject.parseObject(sourceJson);
            Map<String, String> result = new HashMap<>();
            for (String key : json.keySet()) {
                result.put(key, json.getString(key).trim());
            }
            jsonList.add(JSON.toJSONString(result));
        }
        return jsonList;
    }
}
