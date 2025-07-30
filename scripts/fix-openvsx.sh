#!/bin/bash

# Script para corrigir problemas do OpenVSX
set -e

echo "🔧 CORRIGINDO PROBLEMAS DO OpenVSX..."

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

NAMESPACE="repltile"
OVSX_TOKEN="${OVSX_TOKEN:-}"

if [ -z "$OVSX_TOKEN" ]; then
    echo -e "${RED}❌ OVSX_TOKEN não definido!${NC}"
    echo "Execute: export OVSX_TOKEN='seu-token-aqui'"
    exit 1
fi

echo -e "${YELLOW}🔍 Testando token OpenVSX...${NC}"

# Tentar criar namespace (ignora erro se já existir)
echo -e "${YELLOW}📦 Criando namespace '$NAMESPACE'...${NC}"
npx ovsx create-namespace "$NAMESPACE" -p "$OVSX_TOKEN" || {
    echo -e "${YELLOW}⚠️  Namespace pode já existir (erro esperado)${NC}"
}

# Testar publicação de teste (dry-run)
echo -e "${YELLOW}🧪 Testando publicação...${NC}"
if npx vsce package --out "test-package.vsix"; then
    echo -e "${GREEN}✅ VSIX criado com sucesso${NC}"
    
    # Teste se consegue publicar (só valida o token, não publica)
    if npx ovsx publish "test-package.vsix" -p "$OVSX_TOKEN" --dry-run 2>/dev/null; then
        echo -e "${GREEN}✅ Token OpenVSX está válido!${NC}"
    else
        echo -e "${RED}❌ Problema com token OpenVSX${NC}"
        echo "Possíveis soluções:"
        echo "1. Verificar se o token não expirou"
        echo "2. Gerar novo token em: https://open-vsx.org/user-settings/tokens"
        echo "3. Verificar se assinou Publisher Agreement"
        exit 1
    fi
    
    # Limpar arquivo de teste
    rm -f "test-package.vsix"
else
    echo -e "${RED}❌ Erro ao criar VSIX${NC}"
    exit 1
fi

echo -e "${GREEN}🎉 Correção concluída! Token OpenVSX está funcionando.${NC}"