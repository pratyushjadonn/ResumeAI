param(
    [string]$Profile = "local"
)

$ErrorActionPreference = "Stop"

Push-Location $PSScriptRoot
try {
    mvn -pl notification-service -am clean package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        throw "Build failed for notification-service."
    }

    java "-Dspring.profiles.active=$Profile" -jar "notification-service\target\notification-service-1.0.0.jar"
} finally {
    Pop-Location
}
