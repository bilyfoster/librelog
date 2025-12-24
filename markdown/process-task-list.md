# Task List Management

Guidelines for managing task lists in markdown files to track progress on completing a PRD

## Task Implementation
- One sub-task at a time
- **Completion protocol:**  
  1. When you finish a **sub‑task**, immediately mark it as completed by changing `[ ]` to `[x]`.
  2. If **all** subtasks underneath a parent task are now `[x]`, follow this sequence:
    - **First**: Run the full test suite and verify build:
      - For Java/Spring Boot projects: Run `mvn clean install` to compile, run all tests, and generate JaCoCo coverage reports
      - **Verify all tests pass**: Ensure the build completes successfully with no test failures
      - **Verify JaCoCo coverage**: Confirm that code coverage meets the minimum 80% requirement (build will fail if coverage is below threshold)
      - For other projects: Use appropriate test commands (`pytest`, `npm test`, `bin/rails test`, etc.)
    - **Only if all tests pass and coverage requirements are met**: Stage changes (`git add .`)
    - **Clean up**: Remove any temporary files and temporary code before committing
    - **Commit**: Use a descriptive commit message that:
      - Uses conventional commit format (`feat:`, `fix:`, `refactor:`, etc.)
      - Summarizes what was accomplished in the parent task
      - Lists key changes and additions
      - References the task number and PRD context
      - **Formats the message as a single-line command using `-m` flags**, e.g.:

        ```
        git commit -m "feat: add payment validation logic" -m "- Validates card type and expiry" -m "- Adds unit tests for edge cases" -m "Related to T123 in PRD"
        ```
  3. Once all the subtasks are marked completed and changes have been committed, mark the **parent task** as completed.
  4. After the main task is complete:
    - **Stage all changes**: Add all modified and new files to git (`git add .`)
    - **Commit**: Create a final commit for the completed main task (if not already committed in step 2)
    - **Do NOT push**: Never push commits automatically; wait for explicit user instruction to push
- Stop after each main task and wait for the user's go‑ahead.

## Task List Maintenance

1. **Update the task list as you work:**
   - Mark tasks and subtasks as completed (`[x]`) per the protocol above.
   - Add new tasks as they emerge.

2. **Maintain the "Relevant Files" section:**
   - List every file created or modified.
   - Give each file a one‑line description of its purpose.

## AI Instructions

When working with task lists, the AI must:

1. Regularly update the task list file after finishing any significant work.
2. Follow the completion protocol:
   - Mark each finished **sub‑task** `[x]`.
   - Mark the **parent task** `[x]` once **all** its subtasks are `[x]`.
3. Add newly discovered tasks.
4. Keep "Relevant Files" accurate and up to date.
5. Before starting work, check which sub‑task is next.
6. After implementing a sub‑task, update the file and then pause for user approval.