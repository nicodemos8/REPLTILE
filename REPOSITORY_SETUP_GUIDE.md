# 🚀 Guia de Configuração do Repositório REPLTILE

Este guia explica como configurar completamente o repositório REPLTILE com CI/CD, proteção de branch e publicação automática.

## 📋 Visão Geral

**O que foi configurado:**
- ✅ **GitHub Actions** para CI/CD automático
- ✅ **Proteção de branch** main
- ✅ **Build automático** de VSIX
- ✅ **Publicação automática** no OpenVSX
- ✅ **Templates** para Issues e PRs
- ✅ **Scripts** de automação

## 🏗️ Estrutura Criada

```
REPLTILE/
├── .github/
│   ├── workflows/
│   │   ├── ci.yml              # CI/CD principal
│   │   ├── release.yml         # Release automático
│   │   └── pr-validation.yml   # Validação de PRs
│   ├── ISSUE_TEMPLATE/
│   │   ├── bug_report.md
│   │   └── feature_request.md
│   ├── pull_request_template.md
│   ├── branch-protection.md    # Instruções de configuração
│   └── FUNDING.yml
├── scripts/
│   ├── publish-manual.sh       # Publicação manual
│   └── version-bump.sh         # Bump de versão
├── setup-repo.sh               # Setup inicial
├── CONTRIBUTING.md             # Guia de contribuição
└── .gitignore                  # Arquivos ignorados
```

## 🚀 Configuração Inicial

### 1. Execute o Setup Automático

```bash
# Torna executável e executa o setup
chmod +x setup-repo.sh
./setup-repo.sh
```

Este script:
- ✅ Instala dependências
- ✅ Verifica Clojure
- ✅ Faz build inicial
- ✅ Gera VSIX de teste
- ✅ Configura Git hooks

### 2. Configurar Secrets no GitHub

**Vá para GitHub → Settings → Secrets and variables → Actions:**

```bash
# Necessário para publicação no OpenVSX
OVSX_TOKEN = "seu-token-aqui"
```

**Como obter o OVSX_TOKEN:**
1. Acesse https://open-vsx.org
2. Faça login com GitHub
3. Vá para User Settings → Access Tokens
4. Crie um novo token
5. Adicione como secret no GitHub

### 3. Configurar Proteção da Branch Main

**Siga as instruções em `.github/branch-protection.md`**

**Via GitHub Web Interface:**
1. Vá para **Settings → Branches**
2. Clique em **"Add rule"**
3. Configure:
   - Branch name pattern: `main`
   - ✅ Require pull request before merging
   - ✅ Require status checks to pass
   - ✅ Include administrators

**Status checks obrigatórios:**
- `CI/CD Pipeline / test`
- `PR Validation / validate`

## 🔄 Workflow Automático

### Push para `main`:
1. **Trigger**: Push ou merge para `main`
2. **Build**: ClojureScript + TypeScript
3. **Package**: Gera arquivo VSIX
4. **Release**: Cria release no GitHub
5. **Publish**: Publica no OpenVSX automaticamente

### Pull Requests:
1. **Validation**: Build e testes
2. **Package**: Gera VSIX para teste
3. **Comment**: Comenta no PR com status

## 📦 Comandos Úteis

### Build e Package Local:
```bash
# Build completo
npm run vscode:prepublish

# Apenas VSIX
npm run package

# Desenvolvimento (watch)
npm run dev
```

### Releases:
```bash
# Bump de versão automático
./scripts/version-bump.sh patch    # 1.0.0 → 1.0.1
./scripts/version-bump.sh minor    # 1.0.0 → 1.1.0
./scripts/version-bump.sh major    # 1.0.0 → 2.0.0
./scripts/version-bump.sh 1.2.3    # Versão específica
```

### Publicação Manual (se automático falhar):
```bash
# Configurar token
export OVSX_TOKEN="seu-token-aqui"

# Publicar
./scripts/publish-manual.sh
```

## 🎯 Fluxo de Trabalho

### Para Desenvolvedores:

1. **Fork** o repositório
2. **Clone** seu fork
3. **Crie** uma branch: `git checkout -b feature/nova-funcionalidade`
4. **Desenvolva** e teste localmente
5. **Push** para seu fork
6. **Abra** Pull Request para `main`

### Para Maintainers:

1. **Review** Pull Requests
2. **Merge** para `main` (dispara release automático)
3. **Monitor** GitHub Actions
4. **Verificar** publicação no OpenVSX

## 🔧 Configurações Avançadas

### Customizar CI/CD:
- Edite `.github/workflows/*.yml`
- Adicione steps conforme necessário
- Configure diferentes ambientes

### Adicionar Mais Checks:
- Linting
- Security scanning
- Performance tests
- Documentation builds

### Configurar Pre-release:
```bash
# Para versões beta/alpha
./scripts/version-bump.sh 1.0.0-beta.1
```

## 🚨 Troubleshooting

### Build Falha:
```bash
# Limpar cache
npm run clean
npm install
npm run vscode:prepublish
```

### VSIX não Gera:
```bash
# Verificar dependências
npm list --depth=0
npm run build:cljs
npm run build:ts
npm run package
```

### OpenVSX Falha:
```bash
# Verificar token
echo $OVSX_TOKEN

# Publicar manualmente
./scripts/publish-manual.sh
```

## 📚 Recursos

- **Documentação**: `CONTRIBUTING.md`
- **Issues**: Use os templates em `.github/ISSUE_TEMPLATE/`
- **PRs**: Siga o template em `.github/pull_request_template.md`
- **Releases**: Automático via tags `v*`

## 🎉 Pronto!

Agora seu repositório REPLTILE está configurado com:
- ✅ CI/CD completo
- ✅ Proteção de branch
- ✅ Build automático
- ✅ Publicação automática
- ✅ Templates e documentação

**Próximo passo**: Faça seu primeiro commit e push para testar o workflow!