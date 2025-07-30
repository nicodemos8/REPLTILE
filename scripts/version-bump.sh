#!/bin/bash

# Script para atualizar versão e fazer release

set -e

if [ $# -eq 0 ]; then
    echo "Usage: $0 <version-type>"
    echo "   version-type: major, minor, patch, or specific version (e.g., 1.2.3)"
    exit 1
fi

VERSION_TYPE=$1

echo "🏷️  REPLTILE Version Bump"
echo "========================"

# Verificar se estamos na main
CURRENT_BRANCH=$(git branch --show-current)
if [ "$CURRENT_BRANCH" != "main" ]; then
    echo "❌ Você deve estar na branch 'main' para fazer release"
    echo "   Branch atual: $CURRENT_BRANCH"
    exit 1
fi

# Verificar se está limpo
if [ -n "$(git status --porcelain)" ]; then
    echo "❌ Há mudanças não commitadas"
    git status --short
    exit 1
fi

# Atualizar versão no package.json
echo "📝 Atualizando versão..."
if [[ $VERSION_TYPE =~ ^[0-9]+\.[0-9]+\.[0-9]+(-.*)?$ ]]; then
    # Versão específica
    NEW_VERSION=$VERSION_TYPE
    npm version $NEW_VERSION --no-git-tag-version
else
    # Tipo de versão (major, minor, patch)
    NEW_VERSION=$(npm version $VERSION_TYPE --no-git-tag-version)
    NEW_VERSION=${NEW_VERSION#v}  # Remove 'v' prefix
fi

echo "📦 Nova versão: $NEW_VERSION"

# Build e package
echo "🔨 Fazendo build..."
npm run vscode:prepublish
npm run package

# Commit e tag
echo "📝 Commitando mudanças..."
git add package.json
git commit -m "chore: bump version to $NEW_VERSION"

echo "🏷️  Criando tag..."
git tag -a "v$NEW_VERSION" -m "Release v$NEW_VERSION"

echo "🚀 Fazendo push..."
git push origin main
git push origin "v$NEW_VERSION"

echo ""
echo "✅ Release v$NEW_VERSION criado com sucesso!"
echo "   O GitHub Actions irá:"
echo "   - Fazer build automaticamente"
echo "   - Criar release no GitHub"
echo "   - Publicar no OpenVSX"
echo ""
echo "🔗 Acompanhe em: https://github.com/nubank/REPLTILE/actions"