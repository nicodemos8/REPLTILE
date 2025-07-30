#!/bin/bash

# Setup script para configurar o repositório REPLTILE

set -e

echo "🚀 REPLTILE Repository Setup"
echo "=============================="

# Verificar se estamos em um repositório git
if [ ! -d ".git" ]; then
    echo "❌ Este não é um repositório Git. Execute 'git init' primeiro."
    exit 1
fi

# 1. Instalar dependências
echo "📦 Instalando dependências..."
if command -v npm &> /dev/null; then
    npm install
else
    echo "❌ npm não encontrado. Instale Node.js primeiro."
    exit 1
fi

# 2. Verificar Clojure
echo "☕ Verificando Clojure..."
if command -v clojure &> /dev/null; then
    echo "✅ Clojure CLI encontrado: $(clojure --version)"
else
    echo "❌ Clojure CLI não encontrado. Instale primeiro:"
    echo "   macOS: brew install clojure/tools/clojure"
    echo "   Linux: https://clojure.org/guides/getting_started#_installation_on_linux"
    exit 1
fi

# 3. Build inicial
echo "🔨 Fazendo build inicial..."
npm run vscode:prepublish

# 4. Gerar VSIX de teste
echo "📦 Gerando VSIX de teste..."
npm run package

# 5. Configurar Git hooks (opcional)
echo "🪝 Configurando Git hooks..."
mkdir -p .git/hooks

cat > .git/hooks/pre-commit << 'EOF'
#!/bin/bash
echo "🔍 Verificando build antes do commit..."
npm run build:cljs && npm run build:ts
if [ $? -ne 0 ]; then
    echo "❌ Build falhou. Commit abortado."
    exit 1
fi
echo "✅ Build OK"
EOF

chmod +x .git/hooks/pre-commit

# 6. Verificar se há VSIX gerado
if [ -f *.vsix ]; then
    echo "✅ VSIX gerado com sucesso: $(ls *.vsix)"
else
    echo "❌ Falha ao gerar VSIX"
    exit 1
fi

echo ""
echo "🎉 Setup concluído com sucesso!"
echo ""
echo "📋 Próximos passos:"
echo "   1. Configure os secrets no GitHub:"
echo "      - OVSX_TOKEN (para publicação no OpenVSX)"
echo "   2. Configure a proteção de branch 'main'"
echo "   3. Faça seu primeiro commit e push"
echo ""
echo "🔗 Links úteis:"
echo "   - GitHub Settings: https://github.com/nubank/REPLTILE/settings"
echo "   - OpenVSX Registry: https://open-vsx.org"
echo "   - Documentação: ./CONTRIBUTING.md"
echo ""