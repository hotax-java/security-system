FROM openjdk:8-jre-slim
WORKDIR /app
# 复制实际的 Jar 包（名称改为 security-sso-1.0.0.jar）
COPY target/security-admin-1.0.0.jar ./security-admin.jar
EXPOSE 9011
ENTRYPOINT ["java", "-jar", "security-admin.jar", "--spring.profiles.active=prod"]