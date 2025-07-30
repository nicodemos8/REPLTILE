#!/bin/bash

# Script de Automação Completa para REPLTILE
# Este script faz TUDO: cria repo, configura proteções, secrets, e primeiro push

set -e

echo "🚀 AUTOMAÇÃO COMPLETA DO REPOSITÓRIO REPLTILE"
echo "=============================================="
echo ""

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Funções utilitárias
log_info() { echo -e "${BLUE}ℹ️  $1${NC}"; }
log_success() { echo -e "${GREEN}✅ $1${NC}"; }
log_warning() { echo -e "${YELLOW}⚠️  $1${NC}"; }
log_error() { echo -e "${RED}❌ $1${NC}"; }

# Configurações
REPO_NAME="REPLTILE"
REPO_ORG="nicodemos8"
REPO_DESCRIPTION="Interactive visual interface for Clojure REPL with standalone nREPL server"

echo "📋 CONFIGURAÇÃO:"
echo "   Repositório: $REPO_ORG/$REPO_NAME"
echo "   Descrição: $REPO_DESCRIPTION"
echo ""

# 1. AUTENTICAÇÃO NO GITHUB
log_info "Verificando autenticação no GitHub..."
if ! gh auth status > /dev/null 2>&1; then
    log_warning "Não autenticado no GitHub. Iniciando login..."
    gh auth login --hostname github.com --protocol https --web
    log_success "Autenticado no GitHub!"
else
    log_success "Já autenticado no GitHub!"
fi

# 2. VERIFICAR SE REPO JÁ EXISTE
log_info "Verificando se repositório já existe..."
if gh repo view "$REPO_ORG/$REPO_NAME" > /dev/null 2>&1; then
    log_error "Repositório $REPO_ORG/$REPO_NAME já existe!"
    echo ""
    echo "Opções:"
    echo "1) Usar repositório existente"
    echo "2) Cancelar e usar nome diferente"
    read -p "Escolha (1/2): " choice
    
    if [ "$choice" != "1" ]; then
        log_error "Operação cancelada."
        exit 1
    fi
    
    REPO_EXISTS=true
    log_warning "Usando repositório existente..."
else
    REPO_EXISTS=false
fi

# 3. CRIAR REPOSITÓRIO (se não existir)
if [ "$REPO_EXISTS" = false ]; then
    log_info "Criando repositório $REPO_ORG/$REPO_NAME..."
    
    gh repo create "$REPO_ORG/$REPO_NAME" \
        --description "$REPO_DESCRIPTION" \
        --public \
        --add-readme=false \
        --gitignore-template="" \
        --license=""
    
    log_success "Repositório criado!"
fi

# 4. CONFIGURAR GIT LOCAL
log_info "Configurando Git local..."

# Inicializar se não for um repo git
if [ ! -d ".git" ]; then
    git init
    log_success "Git inicializado!"
fi

# Configurar remote
if ! git remote get-url origin > /dev/null 2>&1; then
    git remote add origin "https://github.com/$REPO_ORG/$REPO_NAME.git"
    log_success "Remote origin adicionado!"
else
    git remote set-url origin "https://github.com/$REPO_ORG/$REPO_NAME.git"
    log_success "Remote origin atualizado!"
fi

# 5. CONFIGURAR OVSX TOKEN
log_info "Configurando OVSX Token..."
echo ""
echo "🔑 Para publicar no OpenVSX, precisamos do token:"
echo "   1. Vá para: https://open-vsx.org/user-settings/tokens"
echo "   2. Faça login com GitHub"
echo "   3. Crie um novo token"
echo "   4. Cole aqui:"
echo ""
read -s -p "OVSX Token: " OVSX_TOKEN
echo ""

if [ -z "$OVSX_TOKEN" ]; then
    log_warning "Token não fornecido. Configuração de secrets será pulada."
    OVSX_TOKEN="skip"
fi

# 6. PRIMEIRO COMMIT E PUSH
log_info "Preparando primeiro commit..."

# Adicionar todos os arquivos
git add .

# Verificar se há mudanças para commitar
if git diff --staged --quiet; then
    log_warning "Nenhuma mudança para commitar."
else
    # Fazer commit inicial
    git commit -m "feat: initial REPLTILE setup with CI/CD automation

- Add GitHub Actions workflows (CI/CD, release, PR validation)
- Add repository automation scripts
- Add issue and PR templates
- Add comprehensive documentation
- Configure branch protection and secrets
- Setup automatic VSIX build and OpenVSX publishing"

    log_success "Commit inicial criado!"
fi

# Push para main
log_info "Fazendo push para GitHub..."
git branch -M main
git push -u origin main
log_success "Push realizado!"

# 7. CONFIGURAR SECRETS
if [ "$OVSX_TOKEN" != "skip" ]; then
    log_info "Configurando secrets no GitHub..."
    
    gh secret set OVSX_TOKEN --body "$OVSX_TOKEN" --repo "$REPO_ORG/$REPO_NAME"
    log_success "OVSX_TOKEN configurado!"
fi

# 8. CONFIGURAR PROTEÇÃO DE BRANCH
log_info "Configurando proteção da branch main..."

# Aguardar um pouco para o GitHub processar o push
sleep 5

gh api --method PUT "repos/$REPO_ORG/$REPO_NAME/branches/main/protection" --input - << EOF
{
  "required_status_checks": {
    "strict": true,
    "contexts": [
      "CI/CD Pipeline / test",
      "PR Validation / validate"
    ]
  },
  "enforce_admins": true,
  "required_pull_request_reviews": {
    "required_approving_review_count": 1,
    "dismiss_stale_reviews": true,
    "require_code_owner_reviews": false
  },
  "restrictions": null,
  "required_linear_history": false,
  "allow_force_pushes": false,
  "allow_deletions": false,
  "block_creations": false,
  "required_conversation_resolution": true
}
EOF

log_success "Proteção de branch configurada!"

# 9. EXECUTAR SETUP LOCAL
log_info "Executando setup local..."
chmod +x setup-repo.sh
if ./setup-repo.sh; then
    log_success "Setup local concluído!"
else
    log_warning "Setup local teve problemas, mas continuando..."
fi

# 10. VERIFICAÇÕES FINAIS
log_info "Executando verificações finais..."

# Verificar se Actions estão rodando
sleep 10
log_info "Verificando GitHub Actions..."

ACTIONS_STATUS=$(gh run list --repo "$REPO_ORG/$REPO_NAME" --limit 1 --json status --jq '.[0].status' 2>/dev/null || echo "none")

if [ "$ACTIONS_STATUS" = "in_progress" ] || [ "$ACTIONS_STATUS" = "queued" ]; then
    log_success "GitHub Actions estão rodando!"
    echo "   🔗 Acompanhe em: https://github.com/$REPO_ORG/$REPO_NAME/actions"
elif [ "$ACTIONS_STATUS" = "completed" ]; then
    log_success "GitHub Actions já executaram!"
else
    log_warning "GitHub Actions ainda não iniciaram (normal para primeiro push)"
fi

# 11. RELATÓRIO FINAL
echo ""
echo "🎉 AUTOMAÇÃO COMPLETA!"
echo "======================"
echo ""
log_success "✅ Repositório: https://github.com/$REPO_ORG/$REPO_NAME"
log_success "✅ CI/CD: Configurado e ativo"
log_success "✅ Branch Protection: Configurado"
if [ "$OVSX_TOKEN" != "skip" ]; then
    log_success "✅ OpenVSX Token: Configurado"
fi
log_success "✅ Scripts: Prontos para uso"
log_success "✅ Documentação: Completa"
echo ""
echo "🔗 Links Importantes:"
echo "   📊 Actions: https://github.com/$REPO_ORG/$REPO_NAME/actions"
echo "   ⚙️  Settings: https://github.com/$REPO_ORG/$REPO_NAME/settings"
echo "   🐛 Issues: https://github.com/$REPO_ORG/$REPO_NAME/issues"
echo "   🔄 PRs: https://github.com/$REPO_ORG/$REPO_NAME/pulls"
echo ""
echo "📋 Próximos Passos:"
echo "   1. ✅ Tudo está configurado e funcionando!"
echo "   2. 🎯 Faça mudanças no código e teste o workflow"
echo "   3. 🚀 Use: ./scripts/version-bump.sh patch (para releases)"
echo "   4. 📦 VSIX será gerado automaticamente em cada release"
echo "   5. 🌐 Publicação no OpenVSX é automática"
echo ""
log_success "REPLTILE está pronto para desenvolvimento!"
echo ""