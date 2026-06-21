# start-local.ps1: Build and deploy VotoSync platform locally.

Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "      VotoSync - Automated Local Deployment        " -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan

# Check for Docker
if (-not (Get-Command "docker" -ErrorAction SilentlyContinue)) {
    Write-Error "Error: Docker is not installed or not in PATH."
    Exit
}

# Check for Maven
if (-not (Get-Command "mvn" -ErrorAction SilentlyContinue)) {
    Write-Host "Warning: Maven is not installed locally. Compilation will happen inside Docker." -ForegroundColor Yellow
} else {
    Write-Host "Step 1: Compiling Java microservices locally..." -ForegroundColor Green
    Push-Location backend-services
    mvn clean package -DskipTests -B
    Pop-Location
    Write-Host "Compilation finished successfully!" -ForegroundColor Green
}

Write-Host "Step 2: Launching docker-compose topology..." -ForegroundColor Green
docker compose build --parallel
docker compose up -d

Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "VotoSync stack is online!" -ForegroundColor Green
Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "Access urls:"
Write-Host " - React Frontend:       http://localhost:3000" -ForegroundColor Yellow
Write-Host " - Identity Service:     http://localhost:8081" -ForegroundColor Yellow
Write-Host " - Elections Service:    http://localhost:8082" -ForegroundColor Yellow
Write-Host " - Vote Service:         http://localhost:8083" -ForegroundColor Yellow
Write-Host " - Audit Service:        http://localhost:8084" -ForegroundColor Yellow
Write-Host " - RabbitMQ Admin:       http://localhost:15672 (guest/guest)" -ForegroundColor Yellow
Write-Host " - PostgreSQL Master:    localhost:5432" -ForegroundColor Yellow
Write-Host " - PostgreSQL Slave:     localhost:5433" -ForegroundColor Yellow
Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "To tear down the network, run: docker compose down -v" -ForegroundColor Yellow
