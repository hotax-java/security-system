FROM openjdk:8-jre-slim
WORKDIR /app
# 复制实际的 Jar 包（名称改为 security-sso-1.0.0.jar）
COPY target/security-sso-1.0.0.jar ./security-sso.jar
EXPOSE 9010
ENTRYPOINT ["java", "-jar", "security-sso.jar", "--spring.profiles.active=prod"]