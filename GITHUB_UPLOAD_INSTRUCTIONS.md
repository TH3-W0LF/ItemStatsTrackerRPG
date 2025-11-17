# Instruções para Fazer Upload no GitHub

## Passo 1: Instalar Git (se ainda não tiver)

1. Baixe o Git em: https://git-scm.com/download/win
2. Instale seguindo o assistente
3. Reinicie o terminal/PowerShell

## Passo 2: Configurar Git (primeira vez)

Abra o PowerShell ou Git Bash na pasta do projeto e execute:

```bash
git config --global user.name "SeuNome"
git config --global user.email "seu.email@exemplo.com"
```

## Passo 3: Inicializar o Repositório

Na pasta do projeto (`C:\Users\MestreBR\Documents\att 3`), execute:

```bash
git init
```

## Passo 4: Adicionar Arquivos

```bash
git add .
```

## Passo 5: Fazer o Primeiro Commit

```bash
git commit -m "Initial commit - ItemStatsTracker v3.7"
```

## Passo 6: Criar Repositório no GitHub

1. Acesse https://github.com
2. Clique em "New repository" (ou vá em https://github.com/new)
3. Escolha um nome (ex: `ItemStatsTracker`)
4. **NÃO** marque "Initialize with README" (já temos um)
5. Clique em "Create repository"

## Passo 7: Conectar ao GitHub

Após criar o repositório, o GitHub mostrará comandos. Use estes:

```bash
git remote add origin https://github.com/SEU_USUARIO/ItemStatsTracker.git
git branch -M main
git push -u origin main
```

**Nota:** Substitua `SEU_USUARIO` pelo seu nome de usuário do GitHub.

## Passo 8: Autenticação

O Git pode pedir autenticação. Você pode usar:

### Opção A: Personal Access Token (Recomendado)
1. Vá em: https://github.com/settings/tokens
2. Clique em "Generate new token (classic)"
3. Dê um nome e selecione escopos: `repo` (todos)
4. Copie o token gerado
5. Quando pedir senha, use o token no lugar da senha

### Opção B: GitHub CLI
```bash
gh auth login
```

## Comandos Úteis para Futuro

### Ver status das mudanças
```bash
git status
```

### Adicionar mudanças
```bash
git add .
git commit -m "Descrição das mudanças"
git push
```

### Ver histórico
```bash
git log
```

## Arquivos Ignorados

O arquivo `.gitignore` já está configurado para ignorar:
- Arquivos compilados (`target/`)
- Arquivos de IDE (`.idea/`, etc.)
- Bancos de dados (`*.db`)
- Subprojetos (`ItemEdit-master/`, `ItemTag-master/`)

## Problemas Comuns

### "fatal: not a git repository"
Execute `git init` primeiro.

### "remote origin already exists"
Remova e adicione novamente:
```bash
git remote remove origin
git remote add origin https://github.com/SEU_USUARIO/ItemStatsTracker.git
```

### "Authentication failed"
Use um Personal Access Token em vez da senha.

