//package com.webapp.security.sso.config;
//
//import com.fasterxml.jackson.annotation.JsonTypeInfo;
//import com.fasterxml.jackson.core.JsonParser;
//import com.fasterxml.jackson.databind.DeserializationContext;
//import com.fasterxml.jackson.databind.JsonDeserializer;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
//import com.fasterxml.jackson.databind.module.SimpleModule;
//import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
//import org.springframework.data.redis.serializer.RedisSerializer;
//import org.springframework.data.redis.serializer.SerializationException;
//import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
//import org.springframework.security.oauth2.core.AuthorizationGrantType;
//import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
//import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
//
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//
///**
// * 专门处理Spring Security OAuth2对象的Redis序列化器
// * 解决Spring Security对象序列化问题
// */
//public class SpringSecurityRedisSerializer implements RedisSerializer<Object> {
//
//    private final ObjectMapper objectMapper;
//
//    public SpringSecurityRedisSerializer() {
//        this.objectMapper = createObjectMapper();
//    }
//
//    /**
//     * 创建专门处理Spring Security对象的ObjectMapper
//     */
//    private ObjectMapper createObjectMapper() {
//        ObjectMapper mapper = new ObjectMapper();
//
//        // 注册Java 8时间模块
//        mapper.registerModule(new JavaTimeModule());
//
//        // 创建自定义模块来处理Spring Security类型
//        SimpleModule springSecurityModule = new SimpleModule();
//        springSecurityModule.addDeserializer(ClientAuthenticationMethod.class,
//                new ClientAuthenticationMethodDeserializer());
//        springSecurityModule.addDeserializer(AuthorizationGrantType.class, new AuthorizationGrantTypeDeserializer());
//        springSecurityModule.addDeserializer(ClientSettings.class, new ClientSettingsDeserializer());
//        springSecurityModule.addDeserializer(TokenSettings.class, new TokenSettingsDeserializer());
//        mapper.registerModule(springSecurityModule);
//
//        // 配置类型信息，用于反序列化
//        mapper.activateDefaultTyping(
//                LaissezFaireSubTypeValidator.instance,
//                ObjectMapper.DefaultTyping.NON_FINAL,
//                JsonTypeInfo.As.PROPERTY);
//
//        // 禁用将日期写为时间戳
//        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//
//        // 启用将日期写为ISO-8601格式
//        mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_WITH_ZONE_ID);
//
//        // 配置反序列化特性，处理Spring Security对象
//        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
//        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES,
//                false);
//
//        // 配置序列化特性
//        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
//
//        return mapper;
//    }
//
//    /**
//     * ClientAuthenticationMethod自定义反序列化器
//     */
//    private static class ClientAuthenticationMethodDeserializer extends JsonDeserializer<ClientAuthenticationMethod> {
//        @Override
//        public ClientAuthenticationMethod deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
//            JsonNode node = p.getCodec().readTree(p);
//
//            // 处理 {"value":"client_secret_post"} 格式
//            if (node.has("value")) {
//                String value = node.get("value").asText();
//                return new ClientAuthenticationMethod(value);
//            }
//
//            // 处理直接字符串格式
//            if (node.isTextual()) {
//                return new ClientAuthenticationMethod(node.asText());
//            }
//
//            throw new IOException("Cannot deserialize ClientAuthenticationMethod from: " + node);
//        }
//    }
//
//    /**
//     * AuthorizationGrantType自定义反序列化器
//     */
//    private static class AuthorizationGrantTypeDeserializer extends JsonDeserializer<AuthorizationGrantType> {
//        @Override
//        public AuthorizationGrantType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
//            JsonNode node = p.getCodec().readTree(p);
//
//            // 处理 {"value":"authorization_code"} 格式
//            if (node.has("value")) {
//                String value = node.get("value").asText();
//                return new AuthorizationGrantType(value);
//            }
//
//            // 处理直接字符串格式
//            if (node.isTextual()) {
//                return new AuthorizationGrantType(node.asText());
//            }
//
//            throw new IOException("Cannot deserialize AuthorizationGrantType from: " + node);
//        }
//    }
//
//    /**
//     * ClientSettings自定义反序列化器
//     */
//    private static class ClientSettingsDeserializer extends JsonDeserializer<ClientSettings> {
//        @Override
//        public ClientSettings deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
//            // ClientSettings使用Builder模式，需要特殊处理
//            return ClientSettings.builder().build();
//        }
//    }
//
//    /**
//     * TokenSettings自定义反序列化器
//     */
//    private static class TokenSettingsDeserializer extends JsonDeserializer<TokenSettings> {
//        @Override
//        public TokenSettings deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
//            // TokenSettings使用Builder模式，需要特殊处理
//            return TokenSettings.builder().build();
//        }
//    }
//
//    @Override
//    public byte[] serialize(Object object) throws SerializationException {
//        if (object == null) {
//            return new byte[0];
//        }
//        try {
//            return objectMapper.writeValueAsBytes(object);
//        } catch (Exception e) {
//            throw new SerializationException("Could not serialize object: " + object, e);
//        }
//    }
//
//    @Override
//    public Object deserialize(byte[] bytes) throws SerializationException {
//        if (bytes == null || bytes.length == 0) {
//            return null;
//        }
//        try {
//            return objectMapper.readValue(bytes, Object.class);
//        } catch (Exception e) {
//            throw new SerializationException("Could not deserialize object", e);
//        }
//    }
//}