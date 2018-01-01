package com.clozeblur.core.client;

import com.clozeblur.core.client.common.ConfigConst;
import com.clozeblur.core.client.common.ConstPersist;
import com.clozeblur.core.client.common.CryptUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.*;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author clozeblur
 * <html>集中加载、管理配置文件信息,同时提供自动刷新配置的接口</html>
 */
@Order(Integer.MIN_VALUE)
public class ConfigClientConfigurer extends PropertySourcesPlaceholderConfigurer
        implements EnvironmentAware, BeanPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ConfigClientConfigurer.class);

    private static final String CONFIG_CENTER_PROPERTIES_PROPERTY_SOURCE_NAME = "configClientProperties";
    private static final String OTHER_CONFIGURER_PROPERTIES_PROPERTY_SOURCE_NAME = "otherConfigurerProperties";

    private Environment environment;    // spring环境
    private ConfigurableListableBeanFactory beanFactory;    // spring容器唯一beanFactory

    private MutablePropertySources propertySources; // 本类属性
    private PropertySources appliedPropertySources; // 本类属性

    private Map<String, String> map;    // 远程获取到的配置信息暂存在map中
    private Map<String, String> configParams;   // 本地global.properties文件、application文件配置信息暂存在configMap中
    private Properties props = new Properties();   // 获取到的所有properties放置在本类内存中
    private StringValueResolver valueResolver;  // 记录下配置解析器
    private Map<Field, Object> valueFields = new HashMap<>();   // 记录下@Value注解与该bean的对应关系

    /**
     * 配置中心参数
     */
    private String env;
    private int connectTimeout;
    private int readTimeout;
    private String configUris;
    private String configBranch;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
    }

    @Override
    public void setPropertySources(PropertySources propertySources) {
        this.propertySources = new MutablePropertySources(propertySources);
    }

    //-----------------------------------------------------------------------------------------------------------------
    //                                              修改properties的方法
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    protected Properties mergeProperties() throws IOException {
        Properties superProps = super.mergeProperties();
        initParam();
        persistParam();
        loadConfigs2Map();
        loadMap2Properties(superProps);
        loadProperties(superProps);
        CryptUtil.decryptProperty(superProps);
        return superProps;
    }

    /**
     * 初始化本类参数env,connectTimeout,readTimeout,configBranch,configUris
     */
    private void initParam() {
        env = environment.getProperty(ConfigConst.BOOT_ENV, System.getProperty("env"));
        configUris = environment.getProperty(ConfigConst.CONFIG_URIS_PARAMETER);
        configBranch = environment.getProperty(ConfigConst.CONFIG_BRANCH_PARAMETER);
        connectTimeout = 5000;
        readTimeout = 5000;
        configParams = new LocalParamsLoader(environment).initConfigParams();
        if (StringUtils.isEmpty(env)) configParams.put("env", "dev");
    }

    /**
     * 持久化到静态常数中，便于之后调用
     */
    private void persistParam() {
        ConstPersist.setEnv(env);
        ConstPersist.setConnectTimeout(connectTimeout);
        ConstPersist.setReadTimeout(readTimeout);
        ConstPersist.setConfigUris(configUris);
        ConstPersist.setConfigBranch(configBranch);
    }

    /**
     * 远程获取配置信息的类remoteConfig以及装填本类暂存配置信息键值对的map
     */
    private void loadConfigs2Map() {
        RemoteConfig remoteConfig =
                new RemoteConfigBuilder(env).build().initConfig(env, connectTimeout, readTimeout);
        map = new ConfigClientWorker(configUris, configBranch, remoteConfig).getMap();
    }

    /**
     * 将暂存配置数据的本类map与configParams内信息录入properties中
     */
    private void loadMap2Properties(Properties superProps) {
        if (!map.isEmpty()) {
            superProps.putAll(map);
        }
        if (!configParams.isEmpty()) {
            superProps.putAll(configParams);
        }
        if (superProps != null && !superProps.isEmpty()) {
            for (Object key : superProps.keySet()) {
                superProps.setProperty(key.toString(), superProps.get(key).toString().trim());
            }
            this.props.putAll(superProps);
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    //                                              刷新@Value注解的方法
    //-----------------------------------------------------------------------------------------------------------------

    /**
     * 根据自定义需求，执行相应的刷新任务
     */
    public Boolean refresh() {
        return refresh(configUris);
    }

    public Boolean refresh(Map<String, String> param) {
        param.putAll(new LocalParamsLoader(environment).initConfigParams());
        Properties newlyProps = new Properties();
        newlyProps.putAll(param);
        return refreshProperties(newlyProps);
    }

    public Boolean refresh(String configUris) {
        Properties newlyProps = ((ConfigClientInjector) beanFactory.getBean("configInjector"))
                .getConfigProperties(configUris);

        Map<String, String> configParams;
        configParams = new LocalParamsLoader(environment).initConfigParams();
        newlyProps.putAll(configParams);
        return refreshProperties(newlyProps);
    }

    private Boolean refreshProperties(Properties newlyProps) {
        boolean needRefresh = false;
        for (Object key : props.keySet()) {
            String oldVal = props.getProperty(key.toString());
            String newVal = newlyProps.getProperty(key.toString());
            if (!StringUtils.isEmpty(oldVal)) {
                oldVal = oldVal.trim();
            }
            if (!StringUtils.isEmpty(newVal)) {
                newVal = newVal.trim();
            }
            if (!StringUtils.isEmpty(newVal) && (StringUtils.isEmpty(oldVal) || !oldVal.equals(newVal))) {
                needRefresh = true;
                props.put(key, newVal);
                logger.info("配置 " + key + " - " + oldVal + " -> " + newVal);
            }
        }
        if (needRefresh) {
            PropertySource<?> configCenterPropertySource =
                    new PropertiesPropertySource(CONFIG_CENTER_PROPERTIES_PROPERTY_SOURCE_NAME, props);
            this.propertySources.replace(CONFIG_CENTER_PROPERTIES_PROPERTY_SOURCE_NAME, configCenterPropertySource);
            SimpleTypeConverter typeConverter = new SimpleTypeConverter();
            for (Field field : valueFields.keySet()) {
                String newVal = valueResolver.resolveStringValue(field.getAnnotation(Value.class).value());
                if (!StringUtils.isEmpty(newVal)) {
                    boolean accessibleState = field.isAccessible();
                    if (!accessibleState) field.setAccessible(true);
                    ReflectionUtils.setField(field, valueFields.get(field),
                            typeConverter.convertIfNecessary(newVal, field.getType()));
                    if (!accessibleState) field.setAccessible(false);
                }
            }
            return ConfigConst.NEED_REFRESH;
        }
        return ConfigConst.NO_NEED_REFRESH;
    }

    //-----------------------------------------------------------------------------------------------------------------
    //                                          spring容器内部执行的功能方法
    //-----------------------------------------------------------------------------------------------------------------

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (this.propertySources == null) {
            this.propertySources = new MutablePropertySources();
            if (this.environment != null) {
                this.propertySources.addLast(
                        new PropertySource<Environment>(ENVIRONMENT_PROPERTIES_PROPERTY_SOURCE_NAME, this.environment) {
                            @Override
                            public String getProperty(String key) {
                                return this.source.getProperty(key);
                            }
                        }
                );
            }
            try {
                Properties configClientAppProperties = mergeProperties();
                PropertySource<?> configClientPropertySource =
                        new PropertiesPropertySource(CONFIG_CENTER_PROPERTIES_PROPERTY_SOURCE_NAME,
                                configClientAppProperties);

                if (this.localOverride) {
                    this.propertySources.addFirst(configClientPropertySource);
                } else {
                    this.propertySources.addLast(configClientPropertySource);
                }
            } catch (IOException ex) {
                throw new BeanInitializationException("Could not load properties", ex);
            }

            Properties configurerProperties = configurerProperties();
            if (!configurerProperties.isEmpty()) {
                this.props.putAll(configurerProperties);
                PropertySource<?> configurerPropertySource =
                        new PropertiesPropertySource(OTHER_CONFIGURER_PROPERTIES_PROPERTY_SOURCE_NAME,
                                configurerProperties);
                if (this.localOverride) {
                    this.propertySources.addFirst(configurerPropertySource);
                } else {
                    this.propertySources.addLast(configurerPropertySource);
                }
            }
        }

        processProperties(beanFactory, new PropertySourcesPropertyResolver(this.propertySources));
        this.appliedPropertySources = this.propertySources;
    }

    /**
     * 获取从其他configurer获取到的properties并加入到propertySources当中
     * @return configurerProperties
     */
    private Properties configurerProperties() {
        Properties configurerProperties = new Properties();
        Properties propertyPlaceHolderConfigurerProperties = propertyPlaceHolderConfigurerProperties();
        Properties propertySourcesPlaceHolderConfigurerProperties = propertySourcesPlaceHolderConfigurerProperties();
        if (propertyPlaceHolderConfigurerProperties != null) {
            configurerProperties.putAll(propertyPlaceHolderConfigurerProperties);
        }
        if (propertySourcesPlaceHolderConfigurerProperties != null) {
            configurerProperties.putAll(propertySourcesPlaceHolderConfigurerProperties);
        }
        if (!configurerProperties.isEmpty()) {
            CryptUtil.decryptProperty(configurerProperties);
        }
        return configurerProperties;
    }

    /**
     * 获取所有propertyPlaceHolderConfigurer子类读取到的properties
     * @return propertyPlaceHolderConfigurerProperties
     */
    private Properties propertyPlaceHolderConfigurerProperties() {
        Properties propertyPlaceHolderConfigurerProperties = new Properties();

        if (beanFactory != null) {

            ListableBeanFactory listableBeanFactory = this.beanFactory;
            Map<String, PropertyPlaceholderConfigurer> beans = listableBeanFactory
                    .getBeansOfType(PropertyPlaceholderConfigurer.class, false, false);
            for (PropertyPlaceholderConfigurer configurer : beans.values()) {
                Class clazz = configurer.getClass();
                extractProperties(propertyPlaceHolderConfigurerProperties, configurer, clazz);
            }
        }

        if (!propertyPlaceHolderConfigurerProperties.isEmpty()) {
            return propertyPlaceHolderConfigurerProperties;
        }
        return null;
    }

    /**
     * 获取所有propertySourcesPlaceHolderConfigurer子类读取到的properties
     * @return propertySourcesPlaceHolderConfigurerProperties
     */
    private Properties propertySourcesPlaceHolderConfigurerProperties() {
        Properties propertySourcesPlaceHolderConfigurerProperties = new Properties();

        if (beanFactory != null) {

            ListableBeanFactory listableBeanFactory = this.beanFactory;
            Map<String, PropertySourcesPlaceholderConfigurer> beans = listableBeanFactory
                    .getBeansOfType(PropertySourcesPlaceholderConfigurer.class, false,
                            false);
            for (PropertySourcesPlaceholderConfigurer configurer : beans.values()) {
                Class clazz = configurer.getClass();
                if (!clazz.getName().equals(this.getClass().getName())) {
                    extractProperties(propertySourcesPlaceHolderConfigurerProperties, configurer, clazz);
                }
            }
        }

        if (!propertySourcesPlaceHolderConfigurerProperties.isEmpty()) {
            return propertySourcesPlaceHolderConfigurerProperties;
        }
        return null;
    }

    /**
     * 通过反射的方法,从placeHolderConfigurer的bean中执行mergeProperties方法获取配置信息
     * @param configurerProperties mergeProperties返回值
     * @param configurer bean对象
     * @param clazz bean的class
     */
    @SuppressWarnings("unchecked")
    private void extractProperties(Properties configurerProperties, Object configurer, Class clazz) {
        try {
            Method mergeProperties = clazz.getDeclaredMethod("mergeProperties");
            mergeProperties.setAccessible(true);
            Properties properties = (Properties) ReflectionUtils.invokeMethod(mergeProperties, configurer);
            if (properties != null && !properties.isEmpty()) {
                configurerProperties.putAll(properties);
            }
            mergeProperties.setAccessible(false);
        } catch (NoSuchMethodException e) {
            logger.error("未能获取到PropertySourcesPlaceholderConfigurer子类的mergeProperties方法: {}",
                    clazz.getName());
        }
    }

    /**
     * spring较新版本的方法重写,旧版本并没有此方法
     * @return appliedPropertiesSources
     * @throws IllegalStateException 状态异常
     */
    public PropertySources getAppliedPropertySources() throws IllegalStateException {
        Assert.state(this.appliedPropertySources != null, "PropertySources have not get been applied");
        return this.appliedPropertySources;
    }

    @Override
    protected void processProperties(ConfigurableListableBeanFactory beanFactoryToProcess,
                                     final ConfigurablePropertyResolver propertyResolver) throws BeansException {
        super.processProperties(beanFactoryToProcess, propertyResolver);
    }

    @Override
    protected void doProcessProperties(ConfigurableListableBeanFactory beanFactoryToProcess,
                                       StringValueResolver valueResolver) {
        super.doProcessProperties(beanFactoryToProcess, valueResolver);
        this.valueResolver = valueResolver;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // 记录所有@Value的Field
        ReflectionUtils.doWithFields(bean.getClass(), field -> {
            if (field.getAnnotation(Value.class) != null) {
                valueFields.put(field, bean);
            }
        });
        return bean;
    }
}
