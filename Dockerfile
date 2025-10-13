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
# 统一用明确版本与 JDK（建议与本地一致：17 或 21）
FROM maven:3.9.8-eclipse-temurin-17 AS builder
WORKDIR /app

# 先复制 pom 再拉依赖，可利用缓存
COPY pom.xml .
# 如果是多模块，顺带复制子模块 pom
# COPY common/pom.xml common/pom.xml
# COPY bootstrap/pom.xml bootstrap/pom.xml

# 预拉依赖，加快后续构建
RUN mvn -B -U -q -Dmaven.test.skip=true dependency:go-offline

# 再复制源码
COPY . .

# （关键）先对可能含 Lombok 的模块做一次 compile，用来“验证”注解处理是否生效
# 如果你的 ResultDTO 在 common 模块，请打开下面这行
# RUN mvn -B -U -pl common -am clean compile -DskipTests

# 这里也可以直接全仓库打包
RUN mvn -B -U clean package -DskipTests

# 可选：调试输出，确认 jar 产物存在
RUN ls -l /app/bootstrap/target/

# ===== Runtime stage =====
FROM eclipse-temurin:17-jre
WORKDIR /app

# 拷贝构建产物
COPY --from=builder /app/bootstrap/target/bootstrap*.jar /app/app.jar

# 时区
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
 && echo 'Asia/Shanghai' >/etc/timezone

# 运行所需目录
RUN mkdir -p /data/apps /data/resource/i18n

# 解包 i18n（保证 jar 命令可用；temurin JRE 已包含 jlink 剪裁外的 jre 工具，无 jar 命令时可换成 jdk 或改为 Spring Boot 的 layers）
RUN cd /tmp/ && jar -xf /app/app.jar && cp /tmp/BOOT-INF/classes/i18n/*.properties /data/resource/i18n

EXPOSE 8080 19099 8000
ENTRYPOINT ["sh","-c","java $MEM_OPTS $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -Dlogging.file.name=./logs/supos.log -jar /app/app.jar"]
