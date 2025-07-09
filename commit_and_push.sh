#!/bin/bash

# Script to commit and push the super app changes

# Configure Git user (for this repository)
git config user.email "misterprem@gmail.com"
git config user.name "misterprem"

# Add a commit message
echo "fixes"

# Commit with the message
git commit -F-

# Set up authentication and push to remote
# Note: You may need to authenticate with your personal access token
# When prompted for password, use: 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
git push origin main

echo "✅ Changes committed and pushed successfully!"
