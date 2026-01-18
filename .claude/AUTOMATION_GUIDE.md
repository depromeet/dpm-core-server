# Automation Guide: Git, Commits, and Pull Requests

**Purpose**: Standardize and automate git workflow for this project

**Last Updated**: 2025-01-18

---

## Quick Reference: Git Workflow Commands

### Standard Development Flow

```bash
# 1. Create feature branch from develop
git checkout develop
git pull origin develop
git checkout -b refactor/issue-number

# 2. Make changes and commit
git add <files>
git commit -m "[type]: description"

# 3. Push branch
git push -u origin refactor/issue-number

# 4. Create PR (see PR section below)
```

### Commit Message Format

Follow the project's commit convention:

```bash
[type]: short description

# Types:
[fix]      - Bug fix
[refactor] - Code refactoring
[feat]     - New feature
[docs]     - Documentation only
[test]     - Test code
[chore]    - Maintenance tasks

# Examples:
[fix]: fix apple oauth 500 error
[refactor]: simplify authentication flow
[feat]: add Google OAuth support
```

---

## Pull Request Automation

### Method 1: GitHub Web UI

**Direct PR URL**:
```
https://github.com/depromeet/dpm-core-server/compare/develop...refactor/%232
```

Replace `%232` with your branch name (URL-encode special characters like `#` → `%23`)

### Method 2: GitHub CLI (Recommended)

Install GitHub CLI:
```bash
# macOS
brew install gh

# Authenticate
gh auth login
```

Create PR with one command:
```bash
gh pr create \
  --repo depromeet/dpm-core-server \
  --base develop \
  --head refactor/issue-number \
  --title "[fix] Brief description" \
  --body "## Summary\n\n..."
```

### Method 3: API Automation (For CI/CD)

```bash
#!/bin/bash
# create-pr.sh

PR_TITLE="[fix] Fix Apple OAuth 500 Error"
PR_HEAD="refactor/#232"
PR_BASE="develop"
REPO="depromeet/dpm-core-server"

# Create PR via GitHub API (requires GITHUB_TOKEN)
curl -X POST "https://api.github.com/repos/$REPO/pulls" \
  -H "Authorization: token $GITHUB_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -d @- << EOF
{
  "title": "$PR_TITLE",
  "head": "$PR_HEAD",
  "base": "$PR_BASE",
  "body": "$(cat PR_TEMPLATE.md)"
}
EOF
```

---

## Project-Specific Workflows

### Apple OAuth Fix Workflow

This project uses **profile-specific configuration** for OAuth2:

```bash
# 1. Modify profile-specific YAML files
vim application/src/main/resources/application-local.yml   # LOCAL_APPLE_*
vim application/src/main/resources/application-dev.yml     # DEV_APPLE_*
vim application/src/main/resources/application-prod.yml    # PROD_APPLE_*

# 2. Verify base configuration
vim application/src/main/resources/application.yml  # Should NOT have Apple config

# 3. Compile and test
./gradlew :application:compileKotlin

# 4. Commit
git add application/src/main/resources/
git commit -m "[fix]: separate apple oauth configuration by profile"

# 5. Create PR to develop
gh pr create --base develop --head refactor/issue-number
```

### Swagger Documentation Workflow

For `@Controller` visibility issues:

```bash
# 1. Add @ResponseBody to @Controller methods
# 2. Use RedirectView for redirect endpoints
# 3. Add @Operation annotations
# 4. Test with curl

curl -i http://localhost:8080/endpoint-name

# 5. Verify Swagger UI
open http://localhost:8080/swagger-ui/index.html
```

---

## Branch Naming Conventions

```
feat/issue-number           - New features
fix/issue-number            - Bug fixes
refactor/issue-number       - Code refactoring
hotfix/issue-number         - Production hotfixes
chore/short-description    - Maintenance tasks
```

---

## Environment Variable Management

### .env File (Not in Git)

```bash
# .env file format (not committed)
SPRING_PROFILES_ACTIVE=local
LOCAL_APPLE_CLIENT_ID=...
LOCAL_APPLE_KEY_ID=...
LOCAL_APPLE_TEAM_ID=...
LOCAL_APPLE_PRIVATE_KEY=...
LOCAL_APPLE_REDIRECT_URI=...
LOCAL_APPLE_CLIENT_SECRET=generated_by_jwt
```

**Important**: `.env` is loaded automatically via:
```yaml
# application.yml
spring:
  config:
    import: optional:file:.env[.properties]
```

---

## Testing Checklist Before PR

```bash
# 1. Compilation
./gradlew :application:compileKotlin

# 2. Start application (use IntelliJ, NOT bootRun)
# Open CoreApplication.kt and click Run button

# 3. Test endpoints
curl -i http://localhost:8080/login/kakao
curl -i http://localhost:8080/login/apple
curl http://localhost:8080/actuator/health

# 4. Check Swagger UI
open http://localhost:8080/swagger-ui/index.html

# 5. Run tests (if applicable)
./gradlew :application:test --tests SpecificTest
```

---

## Deployment Strategy

| Environment | Trigger | Target Branch |
|-------------|---------|---------------|
| **LOCAL** | Manual | N/A |
| **DEV** | PR merge to `develop` | `develop` |
| **PROD** | PR merge to `main` → CD pipeline | `main` |

**Important**:
- Do NOT merge directly to main from feature branches
- Always merge: `feature` → `develop` → `main`
- Use pull requests for all merges

---

## Useful Aliases

Add to `~/.gitconfig` or `.git/config`:

```bash
[alias]
    # Commit shortcuts
    cm = "commit -m \"[chore]: \""
    fix = "commit -m \"[fix]: \""
    feat = "commit -m \"[feat]: \""
    refactor = "commit -m \"[refactor]: \""

    # PR shortcuts
    pr-dev = "!f() { git push origin $(git branch --show-current) && gh pr create --base develop; }; f"
    pr-prod = "!f() { git push origin $(git branch --show-current) && gh pr create --base main; }; f"

    # Quick status
    st = status
    co = checkout
    br = branch
```

Usage:
```bash
git fix "fix oauth bug"        # Creates [fix] commit
git pr-dev                     # Pushes and creates PR to develop
```

---

## Troubleshooting

### Issue: "Main method not found" with bootRun

**Solution**: Use IntelliJ IDEA to run the application
1. Open `CoreApplication.kt`
2. Click green play button next to `main()` function
3. OR press `Ctrl+Shift+R` (Run)

### Issue: @Controller not visible in Swagger

**Solution**: Add `@ResponseBody` and use `RedirectView`:
```kotlin
@GetMapping("/login/kakao")
@ResponseBody
fun login(): RedirectView {
    return RedirectView("/oauth2/authorization/kakao")
}
```

### Issue: 500 Error on Apple OAuth2

**Solution**: Check YAML configuration separation:
- `application.yml` → Kakao only
- `application-{profile}.yml` → Apple config per environment

### Issue: Gradle takes 15+ minutes

**Solution**: Use optimized commands:
```bash
# Fast (10-30 seconds)
./gradlew :application:compileKotlin

# Slow (15+ minutes) - AVOID
./gradlew build
./gradlew bootRun
```

---

## Reference Documents

- **Gradle Optimization**: `.claude/GRADLE_OPTIMIZATION.md`
- **Research**: `thoughts/shared/research/2025-01-18-controller-swagger-visibility-research.md`
- **Plan**: `thoughts/shared/plans/2025-01-18-swagger-controller-visibility-fix-plan.md`
- **Apple OAuth Plan**: `thoughts/shared/plans/2026-01-18-apple-oauth2-fix-implementation-plan.md`

---

## Quick Commands Reference Card

```bash
# Development
git checkout develop && git pull
git checkout -b feat/new-feature
# ... make changes ...
git add .
git fix "describe changes"
git push -u origin feat/new-feature
gh pr create --base develop --title "[feat] New feature"

# Quick compile check
./gradlew :application:compileKotlin

# Run tests
./gradlew :application:test --tests SpecificTest

# Check git status
git status
git diff --stat

# View recent commits
git log --oneline -10
```

---

## Best Practices

1. **Always create feature branches from `develop`**
2. **Use conventional commit messages** (`[fix]`, `[feat]`, etc.)
3. **Write descriptive PR descriptions** with test plan
4. **Test locally before pushing**
5. **Use IntelliJ IDEA to run the application** (NOT `./gradlew bootRun`)
6. **Verify Swagger UI** for API changes
7. **Keep PRs focused** - one fix/feature per PR
8. **Add links to issues** in PR descriptions

---

## Automation Scripts

### Pre-commit Check Script

```bash
#!/bin/bash
# pre-commit-check.sh

echo "=== Pre-commit Checks ==="

# 1. Compile
echo "1. Compiling..."
./gradlew :application:compileKotlin --quiet
if [ $? -ne 0 ]; then
    echo "❌ Compilation failed"
    exit 1
fi

# 2. Check for TODO/FIXME
echo "2. Checking for TODOs..."
TODO_COUNT=$(git diff --cached | grep -c "TODO")
if [ $TODO_COUNT -gt 0 ]; then
    echo "⚠️  Warning: $TODO_COUNT TODO(s) in staged changes"
fi

# 3. Check file size
echo "3. Checking file size..."
git diff --cached --stat

echo "✅ Pre-commit checks passed"
```

Usage:
```bash
# Add to .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit-check.sh
./git/hooks/pre-commit-check.sh
```

---

## Current PR Template

Save as `.github/PULL_REQUEST_TEMPLATE.md`:

```markdown
## Summary

<!-- Brief description of changes -->

## Changes

<!-- List of files changed and why -->

## Test Plan

- [ ] Code compiles: `./gradlew :application:compileKotlin`
- [ ] Application starts (use IntelliJ IDEA)
- [ ] Endpoints tested with curl
- [ ] Swagger UI verified: http://localhost:8080/swagger-ui/index.html
- [ ] No regressions in existing functionality

## Related Issues

- Resolves #
- Related to #

## Checklist

- [x] Follows commit convention
- [x] Tested locally
- [x] Documentation updated
- [ ] Code review completed
- [ ] Ready for merge
```

---

**Last Updated**: 2025-01-18
**Maintained By**: Development Team
