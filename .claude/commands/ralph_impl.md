---
description: Implement highest priority small Linear ticket with feature branch setup
model: zai
---

## LANGUAGE POLICY / 언어 정책

**IMPORTANT**: Always respond in Korean, even if the user asks questions in English or other languages.

---

## PART I - IF A TICKET IS MENTIONED

0c. use `linear` cli to fetch the selected item into thoughts with the ticket number - ./thoughts/shared/tickets/ENG-xxxx.md
0d. read the ticket and all comments to understand the implementation plan and any concerns

## PART I - IF NO TICKET IS MENTIOND

0.  read .claude/commands/linear.md
0a. fetch the top 10 priority items from linear in status "ready for dev" using the MCP tools, noting all items in the `links` section
0b. select the highest priority SMALL or XS issue from the list (if no SMALL or XS issues exist, EXIT IMMEDIATELY and inform the user)
0c. use `linear` cli to fetch the selected item into thoughts with the ticket number - ./thoughts/shared/tickets/ENG-xxxx.md
0d. read the ticket and all comments to understand the implementation plan and any concerns

## PART II - NEXT STEPS

think deeply

1. move the item to "in dev" using the MCP tools
1a. identify the linked implementation plan document from the `links` section
1b. if no plan exists, move the ticket back to "ready for spec" and EXIT with an explanation

think deeply about the implementation

2. set up feature branch for implementation:
2a. read the implementation plan document completely
2b. create a new feature branch: `git checkout -b feature/ENG-XXXX-description`
2c. begin implementation following the plan

think deeply, use TodoWrite to track your tasks. When fetching from linear, get the top 10 items by priority but only work on ONE item - specifically the highest priority SMALL or XS sized issue.

## Implementation Guidelines

### Reading the Plan
- Read the ENTIRE implementation plan before starting
- Understand all phases and their dependencies
- Note the success criteria for each phase

### During Implementation
1. **Follow the phase order** - Implement phases in the order specified
2. **Automated verification first** - Run all automated tests before manual verification
3. **Pause for manual confirmation** - After each phase, wait for human to confirm manual testing passed
4. **Use TodoWrite** - Track all implementation tasks
5. **Run tests frequently** - `./gradlew test` after significant changes
6. **Check compilation** - `./gradlew compileKotlin` to catch type errors early

### Gradle Commands Reference
- **Compile**: `./gradlew compileKotlin` or `./gradlew :module:compileKotlin`
- **Test**: `./gradlew test` or `./gradlew :module:test`
- **Integration tests**: `./gradlew integrationTest` or `./gradlew :module:integrationTest`
- **Build**: `./gradlew build`
- **Check style**: `./gradlew ktlintCheck`
- **Format code**: `./gradlew ktlintFormat`
- **Run application**: `./gradlew bootRun`

### Kotlin/Spring Boot Best Practices
- Use data classes for DTOs and entities
- Follow constructor injection pattern
- Use `@Transactional` annotations appropriately
- Handle exceptions with `@ControllerAdvice`
- Write tests with JUnit 5 and MockK
- Use TestContainers for integration tests

### When Complete
After all phases are complete and all tests pass:
1. Run final verification: `./gradlew clean build`
2. Commit your changes (read .claude/commands/commit.md)
3. Create a pull request (read .claude/commands/describe_pr.md)
4. Add a comment to the Linear ticket with the PR link

think deeply about each implementation step, and don't hesitate to ask questions if the plan is unclear or you encounter unexpected issues.