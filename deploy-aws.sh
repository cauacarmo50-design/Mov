#!/bin/bash
set -e

echo "====================================================="
echo "  INSTALADOR AUTOMATICO M.O.V. - AWS EC2 (UBUNTU)    "
echo "====================================================="

# 1. Criar Swap de 2GB caso a maquina AWS tenha pouca RAM (ex: t2.micro / t3.micro)
if [ ! -f /swapfile ]; then
    echo "Criando memoria Swap de 2GB para estabilidade do Java + MySQL..."
    sudo fallocate -l 2G /swapfile
    sudo chmod 600 /swapfile
    sudo mkswap /swapfile
    sudo swapon /swapfile
    echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
fi

# 2. Instalar Docker e Docker Compose se ainda nao estiverem instalados
if ! command -v docker &> /dev/null; then
    echo "Instalando Docker..."
    sudo apt-get update -y
    sudo apt-get install -y ca-certificates curl gnupg
    curl -fsSL https://get.docker.com | sudo sh
    sudo usermod -aG docker $USER
fi

# 3. Subir Banco de Dados MySQL + Plataforma M.O.V. + HTTPS Proxy
echo "Iniciando containers da Plataforma M.O.V. e MySQL..."
sudo docker compose up -d --build

echo "====================================================="
echo "  PLATAFORMA M.O.V. NO AR COM SUCESSO!               "
echo "====================================================="
