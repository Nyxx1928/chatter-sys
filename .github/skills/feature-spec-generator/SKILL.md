---
name: feature-spec-generator
description: "Generate a full feature spec package: requirements -> design -> tasks. Use when asked to create a new feature, write a requirements doc, produce a design doc, or build an implementation plan. Includes acceptance-criteria numbering, optional diagrams, and requirement traceability in tasks."
argument-hint: "Describe the feature, scope, constraints, and target stack."
user-invocable: true
---

# Feature Spec Generator

Create a consistent, three-part feature guide in this sequence:

1. Requirements document
2. Design document
3. Tasks (implementation plan)

## When to Use

- The user asks to create a new feature spec or full guide.
- The user wants requirements, design, and tasks generated together.
- The user wants an implementation plan with requirement traceability.

## Inputs to Confirm

- Feature name and short summary.
- In-scope vs out-of-scope items.
- Target stack (backend/frontend, frameworks, versions).
- Non-functional constraints (performance, security, scale).
- External integrations or dependencies.
- Testing expectations and deployment considerations.

If any input is missing, ask concise questions before writing docs.

## Output Rules

- Always generate in the sequence: requirements -> design -> tasks.
- Requirements: numbered requirements with user story and acceptance criteria using SHALL language.
- Design: include architecture, flows, components, data models, error handling, testing strategy.
- Tasks: numbered checklist with substeps; include requirement traceability tags like _Requirements: 1.1, 2.3_.
- Diagrams: optional; include Mermaid diagrams only if they add clarity.
- Use consistent terminology across all three documents.
- Keep content ASCII unless the repo already uses non-ASCII.

## Templates

### 1) Requirements Document Template

```
# Requirements Document

## Introduction
<Short description and goals>

## Glossary
- **Term**: Definition

## Requirements

### Requirement 1: <Title>

**User Story:** As a <role>, I want <capability>, so that <benefit>.

#### Acceptance Criteria

1. WHEN <condition>, THE <system> SHALL <behavior>
2. WHEN <condition>, THE <system> SHALL <behavior>
3. IF <condition>, THEN THE <system> SHALL <behavior>
```

### 2) Design Document Template

```
# Design Document: <Feature Name>

## Overview
<Architecture summary and goals>

### Key Technologies
- <Backend>
- <Frontend>

### Design Principles
1. <Principle>

## Architecture
### High-Level Architecture
<Optional Mermaid diagram>

### Communication/Data Flow
1. <Step>

## Components and Interfaces
### Backend Components
#### <Component>
- Responsibilities
- Key endpoints or methods

### Frontend Components
#### <Component>
- Responsibilities

## Data Models
<Entities and types>

## Error Handling
<Exceptions, status codes, user messages>

## Testing Strategy
<Unit, integration, E2E>
```

### 3) Tasks Document Template

```
# Implementation Plan: <Feature Name>

## Overview
<Short plan summary>

## Tasks

### <Phase>
- [ ] 1. <Task>
  - Subtask
  - _Requirements: 1.1, 2.3_

- [ ] 2. <Task>
  - Subtask
  - _Requirements: 3.1_

- [ ] 3. Checkpoint - <Phase outcome>
  - Ensure tests pass, ask questions if needed.
```

## Procedure

1. Draft Requirements using the template and confirmed inputs.
2. Draft Design that maps to the requirements and includes optional diagrams.
3. Draft Tasks with requirement traceability tags for each task or subtask.
4. Present files in order and confirm with the user before writing to disk.

## File Placement (recommended)

- .kiro/specs/<feature-name>/requirements.md
- .kiro/specs/<feature-name>/design.md
- .kiro/specs/<feature-name>/tasks.md
