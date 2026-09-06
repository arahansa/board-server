# 빌드와 실행을 나눈다. 실행 이미지에 Gradle 과 소스가 남을 이유가 없다
FROM eclipse-temurin:17-jdk AS build
WORKDIR /src

# 래퍼와 빌드 스크립트를 먼저 넣고 의존성을 받는다. 소스만 고쳤을 때
# 이 레이어가 캐시에 남아 다시 받지 않는다
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies --quiet || true

COPY src ./src
# 테스트는 여기서 돌리지 않는다. 기획서(plan-docs)가 이 저장소에 없어 대조 테스트가
# skip 되므로 이미지 빌드에서 도는 것은 반쪽이다 — CI 나 로컬에서 PLAN_DOCS_DIR 를
# 주고 도는 쪽이 진짜다
RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /src/build/libs/*.jar app.jar

# 포트는 application.yml 이 ${PORT:8080} 으로 읽는다. 여기서 한 번 더 덮으면
# 덮는 자리가 둘이 되고, 그때부터 어느 쪽이 이기는지를 외워야 한다
EXPOSE 8080

# 볼륨을 안 붙이면 VolumeGuard 가 여기서 선다 (application.yml 참고)
ENTRYPOINT ["java", "-jar", "app.jar"]
