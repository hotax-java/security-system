package com.webapp.security.sso.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * SSO模块缓存配置
 * 使用Redis作为缓存存储，与web-core-sdk兼容
 * 使用自定义的Jackson配置处理Spring Security OAuth2对象
 */
@Configuration
@EnableCaching
public class CacheConfig {

         @Bean
         public CacheManager cacheManager() {
             ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
             cacheManager.setCacheNames(Arrays.asList(
                 "oauth2-clients-by-id",
                 "oauth2-clients-by-client-id"
             ));
             cacheManager.setAllowNullValues(false);
             return cacheManager;
         }

        /**
         * 创建支持Spring Security OAuth2对象的Redis序列化器
         */
        //private SpringSecurityRedisSerializer createJsonSerializer() {
        //        return new SpringSecurityRedisSerializer();
        //}

        /**
         * SSO模块专用的Redis缓存管理器
         * 使用@Primary确保优先使用此配置
         * 使用自定义的Jackson配置处理Spring Security OAuth2对象
         */
        //@Bean
        //@Primary
        //public CacheManager cacheManager(RedisConnectionFactory factory) {
        //        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
        //                        .entryTtl(Duration.ofMinutes(30)) // 缓存30分钟
        //                        .serializeKeysWith(RedisSerializationContext.SerializationPair
        //                                        .fromSerializer(new StringRedisSerializer()))
        //                //.serializeValuesWith(RedisSerializationContext.SerializationPair
        //                //        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
        //                        .serializeValuesWith(RedisSerializationContext.SerializationPair
        //                                        .fromSerializer(createJsonSerializer()))
        //                        .disableCachingNullValues(); // 不缓存null值
        //
        //        return RedisCacheManager.builder(factory)
        //                        .cacheDefaults(config)
        //                        .withCacheConfiguration("oauth2-clients-by-id",
        //                                        config.entryTtl(Duration.ofMinutes(60))) // 客户端信息缓存1小时
        //                        .withCacheConfiguration("oauth2-clients-by-client-id",
        //                                        config.entryTtl(Duration.ofMinutes(60)))
        //                        .withCacheConfiguration("token-blacklist",
        //                                        config.entryTtl(Duration.ofDays(1))) // 黑名单缓存1天
        //                        .build();
        //}
}
