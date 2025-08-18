#!/bin/bash

# 认证授权服务启动脚本

echo "正在启动认证授权服务..."

# 设置Java选项
JAVA_OPTS="-Xms512m -Xmx1024m -Dspring.profiles.active=dev"

# 启动服务
java $JAVA_OPTS -jar target-service-1.0.0.jar

echo "认证授权服务已启动"