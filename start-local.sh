#!/usr/bin/env bash
# start-local.sh: Build and deploy VotoSync platform locally.

set -e

# ANSI color codes
CYAN='\033[0;36m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${CYAN}===================================================${NC}"
echo -e "${CYAN}      VotoSync - Automated Local Deployment        ${NC}"
echo -e "${CYAN}===================================================${NC}"

# Check for Docker
if ! command -v docker &> /dev/null; then
    echo -e "${RED}Error: Docker is not installed or not in PATH.${NC}"
    exit 1
fi

# Check for Maven
if ! command -v mvn &> /dev/null; then
    echo -e "${YELLOW}Warning: Maven is not installed locally. Compilation will happen inside Docker.${NC}"
else
    echo -e "${GREEN}Step 1: Compiling Java microservices locally...${NC}"
    cd backend-services
    mvn clean package -DskipTests -B
    cd ..
    echo -e "${GREEN}Compilation finished successfully!${NC}"
fi

echo -e "${GREEN}Step 2: Launching docker-compose topology...${NC}"
docker compose build --parallel
docker compose up -d

echo -e "${CYAN}===================================================${NC}"
echo -e "${GREEN}✔ VotoSync stack is online!${NC}"
echo -e "${CYAN}===================================================${NC}"
echo -e "Access urls:"
echo -e " - ${YELLOW}React Frontend:${NC}       http://localhost:3000"
echo -e " - ${YELLOW}Identity Service:${NC}     http://localhost:8081"
echo -e " - ${YELLOW}Elections Service:${NC}    http://localhost:8082"
echo -e " - ${YELLOW}Vote Service:${NC}         http://localhost:8083"
echo -e " - ${YELLOW}Audit Service:${NC}        http://localhost:8084"
echo -e " - ${YELLOW}RabbitMQ Admin:${NC}       http://localhost:15672 (guest/guest)"
echo -e " - ${YELLOW}PostgreSQL Master:${NC}    localhost:5432"
echo -e " - ${YELLOW}PostgreSQL Slave:${NC}     localhost:5433"
echo -e "${CYAN}===================================================${NC}"
echo -e "To tear down the network, run: ${YELLOW}docker compose down -v${NC}"
