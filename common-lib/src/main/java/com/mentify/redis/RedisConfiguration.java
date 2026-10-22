package com.mentify.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

@AutoConfiguration
@AutoConfigureAfter(RedisAutoConfiguration.class)
@EnableConfigurationProperties(RedisFeatureProperties.class)
@ConditionalOnProperty(prefix = "mentify.redis", name = "enabled", havingValue = "true")
public class RedisConfiguration {

    @Bean
    public RedisCacheService redisCacheService(StringRedisTemplate redis, ObjectMapper objectMapper,
                                               RedisFeatureProperties properties) {
        return new RedisCacheService(redis, objectMapper, properties);
    }

    @Bean
    public RedisLockService redisLockService(StringRedisTemplate redis, RedisFeatureProperties properties) {
        return new RedisLockService(redis, properties);
    }

    @Bean
    public RedisIdempotencyService redisIdempotencyService(StringRedisTemplate redis,
                                                           RedisFeatureProperties properties) {
        return new RedisIdempotencyService(redis, properties);
    }
}
