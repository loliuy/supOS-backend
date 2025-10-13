# FROM maven:amazoncorretto AS builder
# WORKDIR /app
# COPY / /app
# RUN mvn clean package -DskipTests

# RUN ls -l /app/bootstrap/target/

# FROM ibm-semeru-runtimes:open-17-jdk

# COPY  --from=builder /app/bootstrap/target/bootstrap*.jar /app.jar
# RUN pwd
# RUN ls -l /app.jar

# RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime
# RUN echo 'Asia/Shanghai' >/etc/timezone

# RUN mkdir -p /data/apps /data/resource/i18n

# # copy i18n properties to /data/resource/i18n
# RUN cd /tmp/ && jar -xf /app.jar && cp /tmp/BOOT-INF/classes/i18n/*.properties /data/resource/i18n

# EXPOSE 8080 19099 8000
# ENTRYPOINT ["sh","-c","java $MEM_OPTS $JAVA_OPTS  -Djava.security.egd=file:/dev/./urandom -Dlogging.file.name=./logs/supos.log -jar /app.jar"]
# ===== Build stage =====
FROM maven:3.9.8-eclipse-temurin-17 AS builder
WORKDIR /app

# 先把所有源码拷进去（包含所有子模块）
COPY . .

# 直接构建（会触发 Lombok 注解处理；确保 POM 已配置 annotationProcessorPaths）
RUN mvn -B -U clean package -DskipTests

RUN ls -l /app/bootstrap/target/

# ===== Runtime stage =====
FROM eclipse-temurin:17-jdk   # 因为下面用到了 `jar -xf`，这里用 JDK（含 jar 命令）
WORKDIR /app

COPY --from=builder /app/bootstrap/target/bootstrap*.jar /app/app.jar

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo 'Asia/Shanghai' >/etc/timezone && \
    mkdir -p /data/apps /data/resource/i18n

# 解包 i18n（需要 jar 命令，所以用 JDK 运行层）
RUN cd /tmp/ && jar -xf /app/app.jar && cp /tmp/BOOT-INF/classes/i18n/*.properties /data/resource/i18n

EXPOSE 8080 19099 8000
ENTRYPOINT ["sh","-c","java $MEM_OPTS $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -Dlogging.file.name=./logs/supos.log -jar /app/app.jar"]
