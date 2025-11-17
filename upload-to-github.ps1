# Script para fazer upload do projeto no GitHub
# Execute este script no PowerShell: .\upload-to-github.ps1

Write-Host "=== Upload do ItemStatsTracker para GitHub ===" -ForegroundColor Cyan
Write-Host ""

# Verificar se Git está instalado
try {
    $gitVersion = git --version
    Write-Host "✓ Git encontrado: $gitVersion" -ForegroundColor Green
} catch {
    Write-Host "✗ Git não encontrado!" -ForegroundColor Red
    Write-Host "Por favor, instale o Git: https://git-scm.com/download/win" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "Passo 1: Verificando se já é um repositório Git..." -ForegroundColor Cyan

if (Test-Path .git) {
    Write-Host "✓ Repositório Git já inicializado" -ForegroundColor Green
} else {
    Write-Host "Inicializando repositório Git..." -ForegroundColor Yellow
    git init
    Write-Host "✓ Repositório inicializado" -ForegroundColor Green
}

Write-Host ""
Write-Host "Passo 2: Adicionando arquivos..." -ForegroundColor Cyan
git add .
Write-Host "✓ Arquivos adicionados" -ForegroundColor Green

Write-Host ""
Write-Host "Passo 3: Verificando se há mudanças para commitar..." -ForegroundColor Cyan
$status = git status --porcelain
if ($status) {
    Write-Host "Fazendo commit..." -ForegroundColor Yellow
    git commit -m "ItemStatsTracker v3.7 - Sistema completo de rastreamento de itens com itens temporizados"
    Write-Host "✓ Commit realizado" -ForegroundColor Green
} else {
    Write-Host "✓ Nenhuma mudança para commitar" -ForegroundColor Green
}

Write-Host ""
Write-Host "Passo 4: Verificando remote..." -ForegroundColor Cyan
$remote = git remote get-url origin 2>$null
if ($remote) {
    Write-Host "✓ Remote já configurado: $remote" -ForegroundColor Green
} else {
    Write-Host "⚠ Remote não configurado!" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Para configurar o remote, execute:" -ForegroundColor Cyan
    Write-Host "  git remote add origin https://github.com/SEU_USUARIO/ItemStatsTracker.git" -ForegroundColor White
    Write-Host ""
    Write-Host "Substitua SEU_USUARIO pelo seu nome de usuário do GitHub" -ForegroundColor Yellow
    Write-Host ""
    $configure = Read-Host "Deseja configurar o remote agora? (s/n)"
    if ($configure -eq "s" -or $configure -eq "S") {
        $username = Read-Host "Digite seu nome de usuário do GitHub"
        $repoName = Read-Host "Digite o nome do repositório (ou pressione Enter para 'ItemStatsTracker')"
        if ([string]::IsNullOrWhiteSpace($repoName)) {
            $repoName = "ItemStatsTracker"
        }
        git remote add origin "https://github.com/$username/$repoName.git"
        Write-Host "✓ Remote configurado!" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "Passo 5: Fazendo push para o GitHub..." -ForegroundColor Cyan
Write-Host "⚠ Se pedir autenticação, use um Personal Access Token" -ForegroundColor Yellow
Write-Host "   Crie um em: https://github.com/settings/tokens" -ForegroundColor Yellow
Write-Host ""

$branch = git branch --show-current 2>$null
if ([string]::IsNullOrWhiteSpace($branch)) {
    git branch -M main
    $branch = "main"
}

try {
    git push -u origin $branch
    Write-Host ""
    Write-Host "✓ Upload concluído com sucesso!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Seu projeto está disponível no GitHub!" -ForegroundColor Cyan
} catch {
    Write-Host ""
    Write-Host "⚠ Erro ao fazer push. Verifique:" -ForegroundColor Yellow
    Write-Host "  1. Se o repositório existe no GitHub" -ForegroundColor White
    Write-Host "  2. Se você tem permissão para fazer push" -ForegroundColor White
    Write-Host "  3. Se a autenticação está correta" -ForegroundColor White
    Write-Host ""
    Write-Host "Execute manualmente: git push -u origin $branch" -ForegroundColor Cyan
}

Write-Host ""
Write-Host "=== Fim ===" -ForegroundColor Cyan

