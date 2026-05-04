#!/bin/bash

# Render Deployment Helper Script
# This script helps prepare and deploy the backend to Render

set -e

echo "🚀 Render Deployment Helper"
echo "============================"
echo ""

# Check if git is installed
if ! command -v git &> /dev/null; then
    echo "❌ Git is not installed. Please install Git first."
    exit 1
fi

# Check if we're in a git repository
if ! git rev-parse --git-dir > /dev/null 2>&1; then
    echo "❌ Not a git repository. Please initialize git first:"
    echo "   git init"
    echo "   git add ."
    echo "   git commit -m 'Initial commit'"
    exit 1
fi

# Check for uncommitted changes
if ! git diff-index --quiet HEAD --; then
    echo "⚠️  You have uncommitted changes. Commit them before deploying."
    echo ""
    read -p "Do you want to commit all changes now? (y/n) " -n 1 -r
    echo ""
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        git add .
        read -p "Enter commit message: " commit_message
        git commit -m "$commit_message"
        echo "✅ Changes committed"
    else
        echo "❌ Please commit your changes manually before deploying."
        exit 1
    fi
fi

# Check if render.yaml exists
if [ ! -f "render.yaml" ]; then
    echo "❌ render.yaml not found. Please create it first."
    exit 1
fi

echo "✅ Pre-deployment checks passed"
echo ""

# Display current branch
current_branch=$(git branch --show-current)
echo "📍 Current branch: $current_branch"
echo ""

# Check if remote is set
if ! git remote get-url origin &> /dev/null; then
    echo "⚠️  No git remote 'origin' found."
    echo ""
    echo "Please add your GitHub repository as remote:"
    echo "   git remote add origin https://github.com/yourusername/your-repo.git"
    echo ""
    read -p "Enter your GitHub repository URL: " repo_url
    if [ ! -z "$repo_url" ]; then
        git remote add origin "$repo_url"
        echo "✅ Remote added"
    else
        echo "❌ No remote URL provided"
        exit 1
    fi
fi

# Display remote URL
remote_url=$(git remote get-url origin)
echo "🔗 Remote repository: $remote_url"
echo ""

# Ask if user wants to push
read -p "Do you want to push to GitHub now? (y/n) " -n 1 -r
echo ""
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "📤 Pushing to GitHub..."
    git push origin "$current_branch"
    echo "✅ Pushed to GitHub"
else
    echo "⏭️  Skipping push. Remember to push manually:"
    echo "   git push origin $current_branch"
fi

echo ""
echo "🎉 Deployment preparation complete!"
echo ""
echo "Next steps:"
echo "1. Go to https://dashboard.render.com"
echo "2. Click 'New +' → 'Blueprint'"
echo "3. Connect your GitHub repository: $remote_url"
echo "4. Render will detect render.yaml and create services"
echo "5. Update CORS_ALLOWED_ORIGINS with your frontend URL"
echo ""
echo "📚 For detailed instructions, see RENDER_DEPLOYMENT.md"
