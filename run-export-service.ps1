param(
    [string]$Profile = "local"
)

$ErrorActionPreference = "Stop"

Push-Location $PSScriptRoot
try {
    mvn -pl export-service -am clean package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        throw "Build failed for export-service."
    }

    java "-Dspring.profiles.active=$Profile" -jar "export-service\target\export-service-1.0.0.jar"
} finally {
    Pop-Location
}
