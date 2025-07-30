# Configuração de Proteção de Branch

## Como configurar proteção para a branch `main`:

### 1. No GitHub Web Interface:

1. **Navegue para seu repositório no GitHub**
2. **Vá para Settings → Branches**
3. **Clique em "Add rule"**
4. **Configure as seguintes opções:**

```
Branch name pattern: main

✅ Restrict pushes that create files:
   - Block pushes that contain secrets

✅ Require a pull request before merging:
   - Required number of reviewers: 1
   - ✅ Dismiss stale PR approvals when new commits are pushed
   - ✅ Require review from code owners (se tiver CODEOWNERS)

✅ Require status checks to pass before merging:
   - ✅ Require branches to be up to date before merging
   - Status checks que devem passar:
     - CI/CD Pipeline / test
     - PR Validation / validate

✅ Require conversation resolution before merging

✅ Restrict pushes that create files

✅ Include administrators (recomendado)
```

### 2. Via GitHub CLI (gh):

```bash
# Instalar GitHub CLI se não tiver
brew install gh

# Autenticar
gh auth login

# Configurar proteção de branch
gh api repos/:owner/:repo/branches/main/protection \
  --method PUT \
  --field required_status_checks='{"strict":true,"contexts":["CI/CD Pipeline / test","PR Validation / validate"]}' \
  --field enforce_admins=true \
  --field required_pull_request_reviews='{"required_approving_review_count":1,"dismiss_stale_reviews":true}' \
  --field restrictions='{"users":[],"teams":[]}'
```

### 3. Configurações Adicionais Recomendadas:

#### Secrets necessários:
- `OVSX_TOKEN`: Token do OpenVSX Registry

#### CODEOWNERS (opcional):
Crie o arquivo `.github/CODEOWNERS`:
```
# Global owners
* @seu-usuario

# Workflows específicos
.github/workflows/ @seu-usuario @admin-usuario
```

### 4. Comandos para configurar os secrets:

```bash
# Via GitHub CLI
gh secret set OVSX_TOKEN --body "seu-token-aqui"

# Via GitHub Web Interface:
# Settings → Secrets and variables → Actions → New repository secret
```

## Como obter o OVSX_TOKEN:

1. Vá para https://open-vsx.org
2. Faça login com GitHub
3. Vá para Settings → Access Tokens
4. Gere um novo token
5. Adicione como secret no GitHub