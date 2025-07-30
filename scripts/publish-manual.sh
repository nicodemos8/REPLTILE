#!/bin/bash

# Script para publicação manual no OpenVSX
# Use apenas se a publicação automática falhar

set -e

echo "📦 REPLTILE Manual Publish to OpenVSX"
echo "======================================"

# Verificar se o token está configurado
if [ -z "$OVSX_TOKEN" ]; then
    echo "❌ OVSX_TOKEN não configurado."
    echo "   Obtenha em: https://open-vsx.org/user-settings/tokens"
    echo "   Execute: export OVSX_TOKEN=your-token-here"
    exit 1
fi

# Verificar se há VSIX
VSIX_FILE=$(ls *.vsix 2>/dev/null | head -n1)
if [ -z "$VSIX_FILE" ]; then
    echo "❌ Nenhum arquivo VSIX encontrado."
    echo "   Execute: npm run package"
    exit 1
fi

echo "📦 Arquivo VSIX encontrado: $VSIX_FILE"

# Publicar
echo "🚀 Publicando no OpenVSX..."
npx ovsx publish "$VSIX_FILE" -p "$OVSX_TOKEN"

if [ $? -eq 0 ]; then
    echo "✅ Publicado com sucesso no OpenVSX!"
    echo "🔗 Acesse: https://open-vsx.org/extension/repltile/repltile-clojure-repl"
else
    echo "❌ Falha na publicação"
    exit 1
fi