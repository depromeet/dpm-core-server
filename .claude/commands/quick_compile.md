# Quick Compile Command

Use this command for FAST compilation checks (10-30 seconds instead of 15+ minutes):

```bash
./gradlew :application:compileKotlin --quiet
```

## Why This is Fast

- ✅ Compiles ONLY the application module
- ✅ Does NOT run tests (saves 5-10 minutes)
- ✅ Does NOT generate jOOQ code (saves 2-3 minutes)
- ✅ Does NOT create JAR files (saves 1-2 minutes)
- ✅ Uses incremental compilation (only recompiles changed files)

## When to Use This

- After modifying Kotlin code
- Before committing changes
- To check for syntax/compilation errors
- During active development

## Module-Specific Variants

Working on different modules? Use these instead:

```bash
# Application code
./gradlew :application:compileKotlin --quiet

# Domain code
./gradlew :domain:compileKotlin --quiet

# Entity code
./gradlew :entity:compileKotlin --quiet

# Persistence code
./gradlew :persistence:compileKotlin --quiet

# All modules (still fast)
./gradlew compileKotlin --quiet
```

## Even Faster: Dry Run

Just checking if command will work? Use dry-run (5 seconds):

```bash
./gradlew :application:compileKotlin --dry-run
```

## What NOT to Use

These commands are SLOW (15+ minutes). Avoid them:

```bash
# ❌ SLOW - Don't use
./gradlew build
./gradlew :application:build
./gradlew :application:bootRun
./gradlew clean build
```

## Running the Application

**DO NOT use** `./gradlew bootRun` (it's slow and unreliable).

Instead, use **IntelliJ IDEA**:
1. Open `CoreApplication.kt`
2. Click the green play button (▶) next to `main()`
3. OR press `Ctrl+Shift+R` (Windows/Linux) or `Cmd+Shift+R` (Mac)

This starts the application in < 10 seconds!

## Time Comparison

| Command | Time |
|---------|------|
| `./gradlew build` | 15+ minutes ❌ |
| `./gradlew :application:build` | 10+ minutes ❌ |
| `./gradlew bootRun` | 2-3 minutes ❌ |
| `./gradlew :application:compileKotlin` | 10-30 seconds ✅ |
| IntelliJ Run button | < 10 seconds ✅✅✅ |
