# Service Startup (Export + Notification)

If you run a Spring Boot fat jar with `java -cp ... com.example...`, Java cannot find your application class because classes are packaged under `BOOT-INF/classes`.

Use one of these supported approaches:

1. `mvn -pl export-service -am spring-boot:run`
2. `mvn -pl notification-service -am spring-boot:run`
3. `java -jar export-service\target\export-service-1.0.0.jar`
4. `java -jar notification-service\target\notification-service-1.0.0.jar`

Convenience scripts from repo root:

1. `.\run-export-service.ps1`
2. `.\run-notification-service.ps1`

Optional profile argument:

1. `.\run-export-service.ps1 -Profile local`
2. `.\run-notification-service.ps1 -Profile local`
