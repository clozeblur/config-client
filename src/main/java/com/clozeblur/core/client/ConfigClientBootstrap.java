package com.clozeblur.core.client;

import com.clozeblur.core.client.common.ConstPersist;
import com.clozeblur.core.client.redis.Receiver;
import com.clozeblur.core.client.redis.RedisConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import redis.clients.jedis.JedisPoolConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * @author clozeblur
 * <html>公共配置bean</html>
 */
@Configuration
@Order(Integer.MIN_VALUE)
public class ConfigClientBootstrap {

    @Bean(name = "configClientConfigurer")
    @ConditionalOnMissingBean(name = "configClientConfigurer")
    public static ConfigClientConfigurer configClientConfigurer() {
        ConfigClientConfigurer configClientConfigurer = new ConfigClientConfigurer();
        configClientConfigurer.setIgnoreUnresolvablePlaceholders(true);
        configClientConfigurer.setIgnoreResourceNotFound(true);
        return configClientConfigurer;
    }

    @Bean(name = "configClientInjector")
    @ConditionalOnMissingBean(name = "configClientInjector")
    public ConfigClientInjector configInjector() {
        return new ConfigClientInjector();
    }

    @Bean(name = "configClientReceiver")
    @ConditionalOnBean(name = "configClientConfigurer")
    @ConditionalOnMissingBean(name = "configClientReceiver")
    public Receiver configClientReceiver(ConfigClientConfigurer configClientConfigurer) {
        return new Receiver(configClientConfigurer);
    }

    @Bean(name = "configClientConnectionFactory")
    @ConditionalOnMissingBean(name = "configClientConnectionFactory")
    public RedisConnectionFactory configClientConnectionFactory() {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(RedisConfig.getMaxTotal());
        jedisPoolConfig.setMaxIdle(RedisConfig.getMaxIdle());
        jedisPoolConfig.setMinIdle(RedisConfig.getMinIdle());
        jedisPoolConfig.setMaxWaitMillis(RedisConfig.getMaxWaitMillis());
        jedisPoolConfig.setTestWhileIdle(RedisConfig.getTestWhileIdle());

        RedisClusterConfiguration configuration = new RedisClusterConfiguration();
        String[] arr = RedisConfig.getNodes().split(",");
        for(String subArr : arr){
            String[] hap  = subArr.split(":");
            configuration.addClusterNode(new RedisNode(hap[0],Integer.valueOf(hap[1])));
        }
        configuration.setMaxRedirects(RedisConfig.getMaxRedirects());

        JedisConnectionFactory connectionFactory = new JedisConnectionFactory(configuration,jedisPoolConfig);
        connectionFactory.setTimeout(RedisConfig.getTimeout());
        return connectionFactory;
    }

    @Bean(name = "configClientRedisMessageListenerContainer")
    @ConditionalOnBean(name = {"configClientConnectionFactory", "configClientListenerAdapter"})
    @ConditionalOnMissingBean(name = "configClientRedisMessageListenerContainer")
    public RedisMessageListenerContainer container(RedisConnectionFactory configClientConnectionFactory,
                                            MessageListenerAdapter configClientListenerAdapter) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();

        List<String> channels = ConstPersist.getChannels();
        if (channels.isEmpty()) return null;

        container.setConnectionFactory(configClientConnectionFactory);
        List<PatternTopic> topics = new ArrayList<>();
        for (String channel : channels) {
            topics.add(new PatternTopic(channel));
        }
        container.addMessageListener(configClientListenerAdapter, topics);
        return container;
    }

    @Bean(name = "configClientListenerAdapter")
    @ConditionalOnBean(name = "configClientReceiver")
    @ConditionalOnMissingBean(name = "configClientListenerAdapter")
    public MessageListenerAdapter configClientListenerAdapter(Receiver configClientReceiver) {
        return new MessageListenerAdapter(configClientReceiver, "receiveMessage");
    }
}
