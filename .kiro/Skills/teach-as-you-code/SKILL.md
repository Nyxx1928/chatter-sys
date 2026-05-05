---
name: teach-as-you-code
description: Always generate a step-by-step lesson in a dedicated lessons folder using clear, topic-based file names. Keep chat focused on execution updates and point the user to the lesson file.
---

## Teaching Instructions

- Store lessons in a dedicated folder at `.kiro/Skills/teach-as-you-code/lessons/{lesson-set}` (e.g., `frontend`, `backend`, `core`).
- For each new lesson set, create a new directory and corresponding INDEX file to track lessons independently.
- For each new task, create a new lesson file in the appropriate lesson-set folder instead of overwriting an old file.
- Name lesson files with this scalable pattern: `NNN-short-topic-slug.md` where `NNN` is a zero-padded sequential number (001, 002, 003, etc.).
- Make the `short-topic-slug` intuitive from the actual task (for example: `add-rate-limiter`, `fix-login-lockout`, `build-product-grid`).
- Maintain `.kiro/Skills/teach-as-you-code/lessons/{lesson-set}/INDEX.md` and add one entry per lesson with sequence number, title, and file path.
- Use this index row format: `| 001 | Lesson Title | 001-short-topic-slug.md |`.
- In the `## Files Modified` section, list all files that were created, modified, or deleted during the task.
- Use bullet points with relative paths from workspace root (e.g., `- valentines/package.json`, `- backend/src/app.module.ts`).
- Mark file operations clearly: `(created)`, `(modified)`, or `(deleted)` after each file path.
- Put teaching content in the lesson file, including every major step in plain language before and after implementation.
- Break down in the lesson file: what the code does, why this approach, alternatives considered, key concepts, and potential pitfalls.
- Structure every lesson file with these headings in order:
  - `# Lesson: <Title>`
  - `## Task Context`
  - `## Files Modified`
  - `## Step-by-Step Changes`
  - `## Why This Approach`
  - `## Alternatives Considered`
  - `## Key Concepts`
  - `## Potential Pitfalls`
  - `## What You Learned`
- Do **not** teach or explain implementation details in chat unless the user explicitly asks for chat-based teaching.
- Keep chat responses concise and execution-focused, and point the user to the current lesson file.
- Keep code comments focused on code clarity only, not tutorial-style lessons.
- Keep lesson tone friendly and beginner-to-intermediate friendly unless specified otherwise.
- After implementing, include a "What You Learned" summary section in the lesson file.
