**Note**: This project uses [bd (beads)](https://github.com/steveyegge/beads)
for issue tracking. Use `bd` commands instead of markdown TODOs.
See AGENTS.md for workflow details.

# Project Context
See README.md

# Commit and Push Workflow

**CRITICAL**: You must NEVER commit or push changes without explicit user approval.

## Workflow Steps

1. **Make changes** as requested by the user
2. **Show what changed**: Use `git diff` to show the user what you've changed
3. **Explain the changes**: Briefly explain what you did and why
4. **Wait for approval**: Ask "Should I commit and push these changes?"
5. **Only after user says yes**: Run the commit and push commands

## Example Session

```
User: Fix the bug in spring-boot-latest
Assistant: [makes changes]
Assistant: I've fixed the bug. Here are the changes:
[shows git diff output]
Should I commit and push these changes?

User: yes
Assistant: [commits and pushes]
```

## Never Do This

- Do NOT commit immediately after making changes
- Do NOT push without showing the user what changed
- Do NOT assume the user wants changes committed
- Do NOT skip the approval step
