# Build in doua etape: JDK 25 compileaza, un JRE 25 mic ruleaza. Imaginea finala nu contine Maven, sursele sau cache-ul.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Dependintele se descarca intr-un strat separat, ca sa nu fie refacute la fiecare modificare de cod.
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B -ntp dependency:go-offline

COPY src src
RUN ./mvnw -B -ntp -DskipTests package \
    && java -Djarmode=tools -jar target/cpa-backend-*.jar extract --layers --destination target/extracted

FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

# Proces fara drepturi de root.
RUN groupadd --system cpa && useradd --system --gid cpa --no-create-home cpa

# Straturile cele mai stabile primele, ca un build nou sa retransmita doar codul aplicatiei.
COPY --from=build /workspace/target/extracted/dependencies/ ./
COPY --from=build /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/target/extracted/application/ ./

USER cpa
EXPOSE 8080

# Heap mic si GC serial: planul gratuit are putina memorie, iar un singur nucleu nu castiga nimic de la GC paralel.
# `$PORT` este setat de unele gazde (Render); implicit 8080.
ENTRYPOINT ["sh", "-c", "exec java -XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Dserver.port=${PORT:-8080} -jar cpa-backend-0.0.1-SNAPSHOT.jar"]
