# ---- 빌드 단계: Gradle wrapper로 실행 jar(bootJar)를 만든다 ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# 의존성 다운로드 캐시를 소스 변경과 분리한다
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null

COPY src src
RUN ./gradlew --no-daemon clean bootJar

# ---- 실행 단계: JRE만 담은 가벼운 이미지 ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Railway 화면의 Start Command(java -jar build/libs/executor-server.jar)와도 경로가 맞도록 같은 위치에 둔다
COPY --from=build /workspace/build/libs/executor-server.jar build/libs/executor-server.jar

# 컨테이너 메모리 한도 안에서 힙을 잡는다 (작은 플랜에서 메모리 초과로 강제 종료되는 것을 막음)
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC"

# 서버 포트는 Railway가 넣어 주는 PORT 환경 변수를 따른다 (application.properties: ${PORT:8080})
EXPOSE 8080
CMD ["java", "-jar", "build/libs/executor-server.jar"]
