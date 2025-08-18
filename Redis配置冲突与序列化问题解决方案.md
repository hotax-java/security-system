# Redis配置冲突与序列化问题解决方案

## 一、Redis配置冲突问题

### 🔍 **冲突原因**

1. **依赖冲突**：
   - `security-sso`引入了`web-core-sdk`
   - `web-core-sdk`包含Redis相关依赖和自动配置
   - `security-sso`也有自己的Redis配置需求

2. **Bean冲突**：
   - `web-core-sdk`中的`RedisConfig`定义了`RedisTemplate`和`StringRedisTemplate`
   - `security-sso`也需要Redis相关Bean
   - 两个模块的Bean定义产生冲突

3. **自动配置冲突**：
   - Spring Boot的自动配置机制
   - `web-core-sdk`的Redis配置与`security-sso`的配置相互干扰

### ✅ **解决方案**

1. **移除@Primary注解**：
   ```java
   // web-core-sdk/RedisConfig.java
   // 移除@Primary，让security-sso可以定义自己的Bean
   @Bean
   @ConditionalOnMissingBean(RedisTemplate.class)
   public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
       // ...
   }
   ```

2. **使用@ConditionalOnMissingBean**：
   - 确保只有在没有其他Bean时才创建默认Bean
   - 允许应用层覆盖默认配置

## 二、Redis序列化问题详解

### 🔍 **问题根源**

1. **Spring Security OAuth2对象特性**：
   - 很多类没有默认构造函数
   - 使用Builder模式创建实例
   - 包含复杂的嵌套对象结构

2. **Jackson序列化限制**：
   - 无法自动反序列化没有默认构造函数的类
   - 对Builder模式支持有限
   - 对Spring Security特定类型支持不足

### 🔍 **具体错误类型**

#### 1. **Instant序列化错误**
```
SerializationException: Java 8 date/time type `java.time.Instant` not supported by default
```
**原因**：Jackson默认不支持Java 8时间类型

#### 2. **ClientAuthenticationMethod反序列化错误**
```
Cannot construct instance of `ClientAuthenticationMethod` (no delegate- or property-based Creator)
```
**原因**：Spring Security类没有默认构造函数

#### 3. **ClientSettings反序列化错误**
```
Cannot construct instance of `ClientSettings` (no Creators, like default constructor, exist)
```
**原因**：使用Builder模式，没有默认构造函数

### ✅ **完整解决方案**

#### 1. **添加Java 8时间支持**
```xml
<!-- web-core-sdk/pom.xml -->
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
</dependency>
```

#### 2. **创建自定义Redis序列化器**
```java
// security-sso/SpringSecurityRedisSerializer.java
public class SpringSecurityRedisSerializer implements RedisSerializer<Object> {
    
    private final ObjectMapper objectMapper;
    
    public SpringSecurityRedisSerializer() {
        this.objectMapper = createObjectMapper();
    }
    
    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // 注册Java 8时间模块
        mapper.registerModule(new JavaTimeModule());
        
        // 创建自定义模块处理Spring Security类型
        SimpleModule springSecurityModule = new SimpleModule();
        springSecurityModule.addDeserializer(ClientAuthenticationMethod.class, 
            new ClientAuthenticationMethodDeserializer());
        springSecurityModule.addDeserializer(AuthorizationGrantType.class, 
            new AuthorizationGrantTypeDeserializer());
        springSecurityModule.addDeserializer(ClientSettings.class, 
            new ClientSettingsDeserializer());
        springSecurityModule.addDeserializer(TokenSettings.class, 
            new TokenSettingsDeserializer());
        mapper.registerModule(springSecurityModule);
        
        // 配置类型信息
        mapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY);
        
        // 配置反序列化特性
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false);
        
        return mapper;
    }
}
```

#### 3. **自定义反序列化器**
```java
// ClientAuthenticationMethod反序列化器
private static class ClientAuthenticationMethodDeserializer extends JsonDeserializer<ClientAuthenticationMethod> {
    @Override
    public ClientAuthenticationMethod deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        
        // 处理 {"value":"client_secret_post"} 格式
        if (node.has("value")) {
            String value = node.get("value").asText();
            return new ClientAuthenticationMethod(value);
        }
        
        // 处理直接字符串格式
        if (node.isTextual()) {
            return new ClientAuthenticationMethod(node.asText());
        }
        
        throw new IOException("Cannot deserialize ClientAuthenticationMethod from: " + node);
    }
}

// ClientSettings反序列化器
private static class ClientSettingsDeserializer extends JsonDeserializer<ClientSettings> {
    @Override
    public ClientSettings deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        // 使用Builder模式创建默认实例
        return ClientSettings.builder().build();
    }
}
```

#### 4. **配置缓存使用自定义序列化器**
```java
// security-sso/CacheConfig.java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new SpringSecurityRedisSerializer()))
            .disableCachingNullValues();
        
        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .withCacheConfiguration("oauth2-clients-by-id", 
                config.entryTtl(Duration.ofMinutes(60)))
            .withCacheConfiguration("oauth2-clients-by-client-id", 
                config.entryTtl(Duration.ofMinutes(60)))
            .build();
    }
}
```

## 三、影响范围分析

### ✅ **只影响特定缓存**
- 只影响使用`@Cacheable`注解的方法
- 只影响OAuth2相关的缓存键：
  - `oauth2-clients-by-id`
  - `oauth2-clients-by-client-id`
  - `token-blacklist`

### ✅ **RedisTemplate不受影响**
- `RedisTemplate`使用原来的序列化配置
- 其他Redis操作不受影响
- 不影响其他模块的Redis使用

## 四、替代方案

### 1. **禁用OAuth2客户端缓存**
```java
// 移除@Cacheable注解
@Override
public RegisteredClient findById(String id) {
    // 直接从数据库查询，不使用缓存
}
```

### 2. **使用内存缓存**
```java
@Bean
public CacheManager cacheManager() {
    ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
    cacheManager.setCacheNames(Arrays.asList("oauth2-clients-by-id", "oauth2-clients-by-client-id"));
    return cacheManager;
}
```

### 3. **使用allowedOriginPatterns解决CORS**
```java
// 解决CORS配置冲突
config.setAllowedOriginPatterns(Arrays.asList("*")); // 替代allowedOrigins("*")
```

## 五、最佳实践总结

1. **模块化设计**：将通用功能封装在`web-core-sdk`中
2. **条件化Bean**：使用`@ConditionalOnMissingBean`避免冲突
3. **自定义序列化**：针对复杂对象创建专门的序列化器
4. **渐进式解决**：先解决配置冲突，再处理序列化问题
5. **影响范围控制**：只对特定缓存使用自定义序列化器

这个解决方案既保持了`web-core-sdk`的通用性，又解决了`security-sso`的特殊需求，是一个相对完善的解决方案。 