package com.financeportal.backend.Config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableCaching
public class RedisConfig {

    @JsonDeserialize
    abstract static class PageImplMixin {
        @JsonCreator
        public PageImplMixin(
                @JsonProperty("content") List<?> content,
                @JsonProperty("pageable") PageRequest pageable,
                @JsonProperty("total") long total
        ) {}
    }

    // ✅ REDIS için ObjectMapper (Tip bilgisi VAR)
    @Bean(name = "redisObjectMapper")
    public ObjectMapper redisObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.addMixIn(PageImpl.class, PageImplMixin.class);

        // ✅ Tip bilgisi (sadece Redis için)
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Object.class)
                .build();

        objectMapper.activateDefaultTyping(
                ptv,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        return objectMapper;
    }

    // ✅ API için ObjectMapper (Tip bilgisi YOK) - PRIMARY
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // ❌ TİP BİLGİSİ YOK (Frontend için temiz JSON)

        return objectMapper;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory
    ) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // ✅ Redis için özel ObjectMapper
        ObjectMapper redisMapper = redisObjectMapper();
        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(redisMapper);

        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisCacheManager cacheManager(
            RedisConnectionFactory connectionFactory
    ) {
        // ✅ Redis için özel ObjectMapper
        ObjectMapper redisMapper = redisObjectMapper();
        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(redisMapper);

        RedisCacheConfiguration defaultConfig =
                RedisCacheConfiguration.defaultCacheConfig()
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair
                                        .fromSerializer(serializer)
                        )
                        .entryTtl(Duration.ofMinutes(10));

        // ===== NEWS CACHE =====
        RedisCacheConfiguration newsConfig =
                defaultConfig.entryTtl(Duration.ofMinutes(2));

        RedisCacheConfiguration newsByCategoryConfig =
                defaultConfig.entryTtl(Duration.ofMinutes(5));

        RedisCacheConfiguration allNewsConfig =
                defaultConfig.entryTtl(Duration.ofMinutes(5));

        // ===== INSTRUMENT CACHE =====
        RedisCacheConfiguration instrumentPricesConfig =
                defaultConfig.entryTtl(Duration.ofMinutes(1));

        RedisCacheConfiguration instrumentDetailsConfig =
                defaultConfig.entryTtl(Duration.ofHours(1));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                // News
                .withCacheConfiguration("news", newsConfig)
                .withCacheConfiguration("newsByCategory", newsByCategoryConfig)
                .withCacheConfiguration("allNews", allNewsConfig)
                // Instruments
                .withCacheConfiguration("instrumentPrices", instrumentPricesConfig)
                .withCacheConfiguration("instrumentDetails", instrumentDetailsConfig)
                .build();
    }
}