# Gradle Command Optimization Guide

**Purpose**: Reduce Gradle execution time from 15+ minutes to under 2 minutes for common operations.

## Project Structure

This is a multi-module Gradle project with the following modules:
- `application/` - Main Spring Boot application
- `domain/` - Domain layer
- `entity/` - Entity definitions
- `persistence/` - Database persistence layer
- `codegen/` - jOOQ code generation

## Critical Performance Rules

### 1. NEVER Use Full Build for Quick Checks

**❌ BAD** (Takes 15+ minutes):
```bash
./gradlew build
./gradlew :application:build
./gradlew :application:bootRun
```

**✅ GOOD** (Takes 10-30 seconds):
```bash
# Compile only (no tests, no codegen)
./gradlew :application:compileKotlin

# Check compilation without running
./gradlew :application:compileKotlin --dry-run
```

### 2. Skip Code Generation When Not Needed

Code generation (`:codegen:generateJooq`) is the SLOWEST task. Skip it unless you changed database schema.

**❌ BAD**:
```bash
./gradlew :application:build  # Runs codegen
```

**✅ GOOD**:
```bash
./gradlew :application:compileKotlin -x :codegen:generateJooq
```

### 3. Avoid Test Execution During Development

**❌ BAD**:
```bash
./gradlew test
./gradlew :application:test
```

**✅ GOOD**:
```bash
# Compile tests without running
./gradlew :application:compileTestKotlin

# Or explicitly skip
./gradlew :application:build -x test
```

### 4. Use Module-Specific Tasks

**❌ BAD** - Builds ALL modules:
```bash
./gradlew build
./gradlew compileKotlin
```

**✅ GOOD** - Builds only what you need:
```bash
# Working on application code?
./gradlew :application:compileKotlin

# Working on domain code?
./gradlew :domain:compileKotlin

# Working on persistence code?
./gradlew :persistence:compileKotlin
```

## Fast Commands for Common Tasks

### Checking Compilation (Fastest)

```bash
# 10-20 seconds
./gradlew :application:compileKotlin

# All modules
./gradlew compileKotlin
```

### Checking for Errors Without Full Build

```bash
# 5-10 seconds (dry run)
./gradlew :application:compileKotlin --dry-run

# Check task dependencies
./gradlew :application:compileKotlin --graph
```

### Running the Application

**❌ BAD** - Uses Gradle which is slow:
```bash
./gradlew :application:bootRun  # 2-3 minutes
```

**✅ GOOD** - Use IntelliJ IDEA:
1. Open `CoreApplication.kt`
2. Click the green play button next to `main()` function
3. OR press `Ctrl+Shift+R` (Run)

### Running Tests (Only When Needed)

```bash
# Run specific test class (fast)
./gradlew :application:test --tests "AppleClientSecretGeneratorTest"

# Run all tests (slow, use sparingly)
./gradlew :application:test
```

## Gradle Daemon Optimization

Add to `~/.gradle/gradle.properties`:
```properties
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true
org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m
```

## Project-Specific Optimization Flags

### For Quick Compilation Checks
```bash
./gradlew :application:compileKotlin \
  --configuration-cache \
  --parallel \
  -x :codegen:generateJooq \
  --quiet
```

### For Testing Changes
```bash
# Compile test sources without running
./gradlew :application:compileTestKotlin \
  -x :codegen:generateJooq \
  --quiet
```

### For Checking Code Style
```bash
# Skip ktLint during development (it's SLOW)
./gradlew :application:compileKotlin -x :application:ktlintMainSourceSetCheck
```

## Module Dependencies (Know What You're Building)

```
codegen (slowest, generates jOOQ classes)
  ↓
domain
  ↓
entity
  ↓
persistence
  ↓
application (depends on all above)
```

**Rule**: If you only changed `application/` code, you ONLY need to compile `:application`.

## Timeout Settings for AI Assistant

When running Gradle commands, use appropriate timeouts:

| Task Type | Timeout | Command |
|-----------|---------|---------|
| Compile Kotlin only | 30s | `./gradlew :application:compileKotlin` |
| Compile all modules | 60s | `./gradlew compileKotlin` |
| Run specific test | 60s | `./gradlew :application:test --tests "MyTest"` |
| Check errors | 120s | `./gradlew :application:build -x test` |
| Full build (AVOID) | 600s | `./gradlew build` |

## Environment-Specific Notes

### LOCAL Development
- Use IntelliJ IDEA to run the application (NOT `./gradlew bootRun`)
- Compile only the module you're working on
- Skip tests during active development

### CI/CD (GitHub Actions)
- Full build is okay here (parallel execution)
- Tests should run in CI
- Use `--build-cache` for faster subsequent builds

## Troubleshooting Slow Builds

### Check What's Taking Time
```bash
# See which tasks are running
./gradlew :application:compileKotlin --info

# Scan for performance issues
./gradlew :application:compileKotlin --scan
```

### Clean Only When Necessary
```bash
# ❌ BAD - Clean rebuild (VERY SLOW)
./gradlew clean build

# ✅ GOOD - Incremental build
./gradlew :application:compileKotlin

# Only clean if you have caching issues
./gradlew :application:clean :application:compileKotlin
```

### Kill Hanging Gradle Daemons
```bash
# Stop all daemons
./gradlew --stop

# Then run your command again
./gradlew :application:compileKotlin
```

## Quick Reference Card

```
WANT TO:                    | USE:                            | TIME:
----------------------------|---------------------------------|-------
Check compilation           | :application:compileKotlin      | 10-20s
Check errors                | :application:compileKotlin      | 10-20s
Run app locally             | IntelliJ IDEA Run button         | < 10s
Run specific test           | :application:test --tests "X"   | 30-60s
Check all modules compile   | compileKotlin                   | 30-60s
Full build (AVOID!)         | build                           | 15min+
```

## Remember

1. **IntelliJ IDEA > Gradle bootRun** - Always use IDE for running the app
2. **compileKotlin > build** - Compile only, don't build
3. **Specific module > all modules** - Target what you changed
4. **Skip codegen** - Unless you changed DB schema
5. **Skip tests** - Until you're ready to verify

Follow these rules and you'll see 10-30x performance improvement!
