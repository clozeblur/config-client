package com.clozeblur.core.client;

import org.springframework.core.env.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author clozeblur
 * <html>将spring boot本地配置application.properties、application.yml以及application.yaml文件信息读入</html>
 */
class LocalParamsLoader {

    private Environment environment;

    LocalParamsLoader(Environment environment) {
        this.environment = environment;
    }

    Map<String, String> initConfigParams() {
        Map<String, String> loader = new HashMap<>();
        Map<String, String> bootLocalMap = bootFunction();
        if (bootLocalMap != null && !bootLocalMap.isEmpty()) loader.putAll(bootLocalMap);
        return loader;
    }

    /**
     * 对于spring boot项目，则将环境变量中application.properties或者application.yml(yaml)配置重新录入
     * 其他类型的配置文件内的信息暂不考虑
     * @return map
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> bootFunction() {
        MutablePropertySources propertySources = ((StandardEnvironment) environment).getPropertySources();
        Iterator<PropertySource<?>> iterator = propertySources.iterator();
        Map configParams = new HashMap();
        while (iterator.hasNext()) {
            PropertySource<?> propertySource = iterator.next();
            if (propertySource.getName().contains("applicationConfig")) {
                if (propertySource.getName().contains("application.properties")) {
                    PropertiesPropertySource props = (PropertiesPropertySource)propertySource;
                    configParams.putAll(props.getSource());
                }
                if (propertySource.getName().contains("application.yml")
                        || propertySource.getName().contains("application.yaml") ) {
                    MapPropertySource props = (MapPropertySource)propertySource;
                    configParams.putAll(props.getSource());
                }
            }
        }
        return configParams;
    }
}
