# 🤝 Contribuindo para o REPLTILE

Obrigado por querer contribuir com o REPLTILE! Este guia explica como contribuir de forma efetiva.

## 📋 Índice

- [Como Contribuir](#como-contribuir)
- [Reportando Bugs](#reportando-bugs)
- [Sugerindo Funcionalidades](#sugerindo-funcionalidades)
- [Contribuindo com Código](#contribuindo-com-código)
- [Processo de Pull Request](#processo-de-pull-request)
- [Desenvolvimento Local](#desenvolvimento-local)

## 🚀 Como Contribuir

### 1. Reportando Bugs 🐛

Use o [template de bug report](.github/ISSUE_TEMPLATE/bug_report.md):

- **Descreva** o problema claramente
- **Inclua** passos para reproduzir
- **Adicione** logs e informações do ambiente
- **Anexe** screenshots se aplicável

### 2. Sugerindo Funcionalidades ✨

Use o [template de feature request](.github/ISSUE_TEMPLATE/feature_request.md):

- **Explique** o problema que a funcionalidade resolveria
- **Descreva** a solução desejada
- **Considere** alternativas

### 3. Contribuindo com Código 👨‍💻

1. **Fork** o repositório
2. **Crie** uma branch para sua funcionalidade: `git checkout -b feature/nova-funcionalidade`
3. **Faça** suas mudanças
4. **Teste** localmente
5. **Commit** suas mudanças: `git commit -am 'feat: adiciona nova funcionalidade'`
6. **Push** para a branch: `git push origin feature/nova-funcionalidade`
7. **Abra** um Pull Request

## 🔄 Processo de Pull Request

### Checklist antes de abrir o PR:

- [ ] **Build** funciona: `npm run vscode:prepublish`
- [ ] **VSIX** é gerado: `npm run package`
- [ ] **Testes** passam: `npm test`
- [ ] **Commit messages** seguem o padrão: `type: description`
- [ ] **Code review** próprio realizado

### Tipos de commit:

- `feat`: nova funcionalidade
- `fix`: correção de bug
- `docs`: mudanças na documentação
- `style`: formatação (sem mudança de código)
- `refactor`: refatoração
- `test`: adição ou correção de testes
- `chore`: mudanças de build/CI

### Processo de Review:

1. **Automated checks** devem passar
2. **Pelo menos 1 aprovação** é necessária
3. **Conflitos** devem ser resolvidos
4. **CI/CD pipeline** deve estar verde

## 🛠️ Desenvolvimento Local

### Pré-requisitos:

```bash
# Node.js 18+
node --version

# Java 11+ (para Clojure)
java --version

# Clojure CLI
clojure --version
```

### Setup:

```bash
# Clone o repositório
git clone https://github.com/nubank/REPLTILE.git
cd REPLTILE

# Instalar dependências
npm install

# Build inicial
npm run vscode:prepublish

# Desenvolvimento (watch mode)
npm run dev
```

### Testando:

```bash
# Gerar VSIX local
npm run package

# Instalar no Cursor
# Command Palette → "Extensions: Install from VSIX..."
```

### Estrutura do Projeto:

```
REPLTILE/
├── src/
│   ├── cljs/           # ClojureScript (UI/Logic)
│   └── ts/             # TypeScript (VS Code Integration)
├── .github/            # GitHub Actions & Templates
├── out/                # Build output
├── lib/                # Clojure dependencies
└── package.json        # Extension manifest
```

## 🎯 Guidelines de Código

### ClojureScript:
- Use **kebab-case** para nomes
- **Docstrings** para funções públicas
- **Namespaces** organizados por funcionalidade

### TypeScript:
- Use **camelCase** para variáveis
- **PascalCase** para classes
- **Interfaces** para contratos
- **JSDoc** para documentação

### Geral:
- **Mensagens de commit** em inglês
- **Comentários** em português no código
- **Issues/PRs** em português
- **Testes** para funcionalidades críticas

## 🚨 Importante

### ❌ NÃO faça:
- Push direto para `main`
- Commits com secrets/credentials
- Mudanças sem testes
- Breaking changes sem discussão

### ✅ SEMPRE faça:
- Fork + Pull Request
- Testes locais antes do PR
- Documentação para funcionalidades novas
- Review do próprio código

## 🆘 Precisa de Ajuda?

- **GitHub Issues**: Para bugs e funcionalidades
- **GitHub Discussions**: Para perguntas gerais
- **Code Review**: Deixe comentários no PR

---

**Obrigado por contribuir com o REPLTILE!** 🎉