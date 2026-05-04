# Render Deployment Helper Script (PowerShell)
# This script helps prepare and deploy the backend to Render

Write-Host "🚀 Render Deployment Helper" -ForegroundColor Cyan
Write-Host "============================" -ForegroundColor Cyan
Write-Host ""

# Check if git is installed
try {
    git --version | Out-Null
} catch {
    Write-Host "❌ Git is not installed. Please install Git first." -ForegroundColor Red
    exit 1
}

# Check if we're in a git repository
try {
    git rev-parse --git-dir | Out-Null
} catch {
    Write-Host "❌ Not a git repository. Please initialize git first:" -ForegroundColor Red
    Write-Host "   git init"
    Write-Host "   git add ."
    Write-Host "   git commit -m 'Initial commit'"
    exit 1
}

# Check for uncommitted changes
$status = git status --porcelain
if ($status) {
    Write-Host "⚠️  You have uncommitted changes. Commit them before deploying." -ForegroundColor Yellow
    Write-Host ""
    $commit = Read-Host "Do you want to commit all changes now? (y/n)"
    if ($commit -eq "y" -or $commit -eq "Y") {
        git add .
        $message = Read-Host "Enter commit message"
        git commit -m "$message"
        Write-Host "✅ Changes committed" -ForegroundColor Green
    } else {
        Write-Host "❌ Please commit your changes manually before deploying." -ForegroundColor Red
        exit 1
    }
}

# Check if render.yaml exists
if (-not (Test-Path "render.yaml")) {
    Write-Host "❌ render.yaml not found. Please create it first." -ForegroundColor Red
    exit 1
}

Write-Host "✅ Pre-deployment checks passed" -ForegroundColor Green
Write-Host ""

# Display current branch
$currentBranch = git branch --show-current
Write-Host "📍 Current branch: $currentBranch" -ForegroundColor Cyan
Write-Host ""

# Check if remote is set
try {
    $remoteUrl = git remote get-url origin 2>$null
} catch {
    Write-Host "⚠️  No git remote 'origin' found." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Please add your GitHub repository as remote:"
    Write-Host "   git remote add origin https://github.com/yourusername/your-repo.git"
    Write-Host ""
    $repoUrl = Read-Host "Enter your GitHub repository URL"
    if ($repoUrl) {
        git remote add origin "$repoUrl"
        Write-Host "✅ Remote added" -ForegroundColor Green
        $remoteUrl = $repoUrl
    } else {
        Write-Host "❌ No remote URL provided" -ForegroundColor Red
        exit 1
    }
}

# Display remote URL
Write-Host "🔗 Remote repository: $remoteUrl" -ForegroundColor Cyan
Write-Host ""

# Ask if user wants to push
$push = Read-Host "Do you want to push to GitHub now? (y/n)"
if ($push -eq "y" -or $push -eq "Y") {
    Write-Host "📤 Pushing to GitHub..." -ForegroundColor Cyan
    git push origin $currentBranch
    Write-Host "✅ Pushed to GitHub" -ForegroundColor Green
} else {
    Write-Host "⏭️  Skipping push. Remember to push manually:" -ForegroundColor Yellow
    Write-Host "   git push origin $currentBranch"
}

Write-Host ""
Write-Host "🎉 Deployment preparation complete!" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "1. Go to https://dashboard.render.com"
Write-Host "2. Click 'New +' → 'Blueprint'"
Write-Host "3. Connect your GitHub repository: $remoteUrl"
Write-Host "4. Render will detect render.yaml and create services"
Write-Host "5. Update CORS_ALLOWED_ORIGINS with your frontend URL"
Write-Host ""
Write-Host "📚 For detailed instructions, see RENDER_DEPLOYMENT.md" -ForegroundColor Cyan
