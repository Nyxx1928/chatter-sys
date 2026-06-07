---
name: feature-spec-generator
description: "Generate professional three-part specs (requirements → design → tasks) in FEATURE or BUGFIX mode. Reads the repository first to infer tech stack, patterns, and context, then iterates through each phase with user confirmation before proceeding. Matches repository standards with formal specifications, correctness properties, comprehensive testing strategies, and requirement traceability. Use for new features, bug fixes, or implementation planning."
argument-hint: "Specify mode (feature|bugfix), feature name, scope, constraints, and target stack."
user-invocable: true
---

# Feature Spec Generator (Enhanced)

Generate professional, three-part specification packages that match repository standards:

1. **Requirements Document** — User stories, acceptance criteria, glossary, scope
2. **Design Document** — Architecture, components, data models, correctness properties, testing strategy
3. **Tasks Document** — Phased implementation plan with checkpoints and requirement traceability

## When to Use

- Creating a new feature spec with full requirements → design → implementation flow, reviewed phase by phase
- Documenting a bug fix with formal bug conditions and correctness properties, reviewed phase by phase
- Building an implementation plan with requirement traceability, confirmed before writing
- Establishing architecture and testing strategy for a new system component
- When you want the agent to read the repo first and base the spec on existing code conventions rather than guessing

## Operating Modes

### Mode 1: FEATURE
Use for new functionality, user-facing capabilities, and architectural additions.

**Emphasis:**
- User stories and value proposition
- Scope and non-goals
- Architectural decisions and design principles
- Component interfaces and data flows
- Correctness properties (what should always be true)
- Comprehensive testing strategy

**Example:** Direct Messaging, Mobile-First Redesign, OAuth Integration

### Mode 2: BUGFIX
Use for defect fixes, security vulnerabilities, and regression prevention.

**Emphasis:**
- Problem statement and impact
- Formal bug condition specifications with examples
- Root cause analysis
- Correctness properties (fix validation)
- Preservation properties (regression prevention)
- Three-phase testing (exploratory → fix checking → preservation)

**Example:** Chat Functionality Fixes, CI Test Failures, Security Hardening

## Context Gathering (Repo-Aware)

Before generating anything, read the repository to infer as much context as possible. Do NOT ask the user for inputs the repo can answer.

### Phase 1: Analyze Repository
Read the repo and extract:
- **Target Stack** — read `pom.xml`, `package.json`, `build.gradle`, `Cargo.toml`, Dockerfiles, etc. to determine backend framework, language version, frontend framework, testing libraries, database, CI/CD
- **Existing Patterns** — read a sample controller, service, model, and test file to understand conventions (naming, package structure, error handling, DTO patterns)
- **Existing Features** — scan routes, API endpoints, and existing feature directories to know what already exists
- **Similar Features** — if the spec is for a feature type that already exists (e.g., another auth-adjacent feature), read those specs for pattern reuse

### Phase 2: Infer from Repo + User Prompt
From the combined context, automatically deduce:
- **Spec Name** — kebab-case from the prompt
- **Short Summary** — 1-2 sentence synthesis
- **Target Stack** — from repo analysis
- **Non-Functional Constraints** — from existing patterns (e.g., "follows existing logging pattern", "matches existing error handling convention")
- **External Dependencies** — from existing integrations in `pom.xml`/`package.json`
- **User Stories** — from domain model analysis and existing service patterns
- **In-Scope vs Out-of-Scope** — reasoned from the prompt
- **Design Principles** — derived from existing code patterns (e.g., "follow repository pattern", "type-safe", "reuse existing infrastructure")

### Phase 3: Clarify with User
For anything that remains ambiguous, where the repo is silent, or where reasonable alternatives exist, ask the user a concise clarifying question. Do not ask about things already answered by the repo.

Examples of when to ask:
- "I found Spring Boot 3.2 + PostgreSQL in the repo, and a React frontend with Tailwind. Should the spec target that same stack?" (only ask if multiple stacks exist)
- "There are existing controller patterns in `controller/` using `@RestController` and service-layer interfaces. Should I follow the same pattern for this feature?"
- "The prompt mentions rate limiting. What limit strategy do you prefer — token bucket per email, per IP, or both?"
- "Should the forgot-password flow include a CAPTCHA step, or is email-only acceptable?"

If the user's prompt is vague, ask 1-2 targeted questions to sharpen it before proceeding.

## Output Rules (All Modes)

### Structure & Formatting
- **Sequence**: Always generate requirements → design → tasks (in that order)
- **Numbering**: Requirements numbered 1.x, 2.x, etc.; tasks numbered sequentially with phases
- **Terminology**: Consistent glossary terms used throughout all three documents
- **Traceability**: Each task references specific requirements via `_Requirements: 1.1, 2.3_` tags
- **Checkpoints**: Explicit validation points after each major phase

### Requirements Document
- **Glossary**: Define all domain-specific terms (mandatory)
- **Acceptance Criteria**: Use formal "WHEN...THEN...SHALL" language
- **Scope**: Include both acceptance criteria AND preservation requirements (what must NOT change)
- **Numbering**: Requirement 1, 1.1, 1.2, etc. (hierarchical)

### Design Document
- **Architecture**: Include high-level diagram (Mermaid) and component interaction flows
- **Correctness Properties**: Formal statements of what should always be true (3-10 properties)
- **Root Cause Analysis** (bugfix mode): Explain why the bug exists
- **Data Models**: Entity diagrams, schema changes, type definitions
- **Error Handling**: Exceptions, status codes, user-facing messages
- **Testing Strategy**: Explicit approach (unit, integration, E2E, property-based, accessibility)
- **PBT Applicability**: Explicitly state whether property-based testing is appropriate and why

### Tasks Document
- **Phases**: Organize into logical phases (e.g., Phase 1: Exploratory Tests, Phase 2: Implementation)
- **Checkpoints**: Explicit validation points with clear success criteria
- **Requirement Traceability**: Every task tagged with `_Requirements: X.X_`
- **Optional Tasks**: Mark MVP vs optional work with `[x]*` notation
- **Subtasks**: Break complex tasks into actionable subtasks
- **Acceptance Criteria**: Each task includes specific, measurable success criteria

### Diagrams
- Use Mermaid for architecture, data flows, and state machines
- Include only if they add clarity (not decorative)
- Label all diagram elements clearly

### Tone & Style
- **Professional**: Formal, precise language
- **Clear**: Avoid jargon; define all terms
- **Actionable**: Tasks are specific and measurable
- **Honest**: Explicitly address constraints, trade-offs, and limitations

## Templates

### 1) Requirements Document (Feature Mode)

```markdown
# Requirements Document

## Introduction
<2-3 sentence description of feature, value proposition, and goals>

## Glossary

- **Term_Name**: Definition with context
- **Another_Term**: Definition

## Requirements

### Requirement 1: <Descriptive Title>

**User Story:** As a <role>, I want <capability>, so that <benefit>.

#### Acceptance Criteria

1. WHEN <condition>, THE <system> SHALL <behavior>
2. WHEN <condition>, THE <system> SHALL <behavior>
3. IF <condition>, THEN THE <system> SHALL <behavior>

### Requirement 2: <Descriptive Title>

**User Story:** As a <role>, I want <capability>, so that <benefit>.

#### Acceptance Criteria

1. WHEN <condition>, THE <system> SHALL <behavior>
2. ...

## Scope

### In-Scope
- Feature A
- Feature B

### Out-of-Scope
- Feature C (reason)
- Feature D (reason)
```

### 2) Requirements Document (Bugfix Mode)

```markdown
# Bugfix Requirements Document

## Introduction
<Description of the bug, impact, and goals of the fix>

## Glossary

- **Bug_Condition (C)**: The condition that triggers the bug
- **Property (P)**: The desired behavior when bug condition holds
- **Preservation**: Existing functionality that must remain unchanged

## Bug Analysis

### Current Behavior (Defect)

#### Bug Category 1: <Title>

1.1 WHEN <condition>, THEN <buggy behavior occurs>
1.2 WHEN <condition>, THEN <buggy behavior occurs>

### Expected Behavior (Correct)

#### Bug Category 1: <Title>

2.1 WHEN <condition>, THE <system> SHALL <correct behavior>
2.2 WHEN <condition>, THE <system> SHALL <correct behavior>

### Unchanged Behavior (Regression Prevention)

3.1 WHEN <condition>, THE <system> SHALL CONTINUE TO <existing behavior>
3.2 WHEN <condition>, THE <system> SHALL CONTINUE TO <existing behavior>
```

### 3) Design Document (Feature Mode)

```markdown
# Design Document: <Feature Name>

## Overview

<2-3 paragraph description of architecture, goals, and key design decisions>

### Key Technologies

- **Backend**: Spring Boot 3.x, Java 21, PostgreSQL
- **Frontend**: Next.js 14+, React 18+, TypeScript, Tailwind CSS, Zustand

### Design Principles

1. <Principle and rationale>
2. <Principle and rationale>
3. <Principle and rationale>

## Architecture

### High-Level Architecture

\`\`\`mermaid
graph TB
  UI[Frontend] --> API[REST API]
  API --> DB[(Database)]
\`\`\`

### Communication/Data Flow

1. <Step 1>
2. <Step 2>
3. <Step 3>

## Components and Interfaces

### Backend Components

#### ComponentName
- **Responsibilities**: What it does
- **Key Methods/Endpoints**: 
  - POST /api/endpoint
  - GET /api/endpoint/{id}

### Frontend Components

#### ComponentName
- **Responsibilities**: What it does
- **Props Interface**: 
  \`\`\`typescript
  interface Props {
    prop1: string;
    prop2: number;
  }
  \`\`\`

## Data Models

### Database Schema

\`\`\`sql
CREATE TABLE table_name (
  id BIGSERIAL PRIMARY KEY,
  column_name VARCHAR(100) NOT NULL
);
\`\`\`

### TypeScript Types

\`\`\`typescript
export interface DomainType {
  id: number;
  name: string;
}
\`\`\`

## Correctness Properties

### Property 1: <Property Title>

*For any* <input condition>, the system SHALL <expected behavior>.

**Validates: Requirements 1.1, 1.2, 2.3**

### Property 2: <Property Title>

*For any* <input condition>, the system SHALL <expected behavior>.

**Validates: Requirements 3.1, 3.2**

## Error Handling

| Scenario | Response | User Feedback |
|----------|----------|---------------|
| <Scenario> | <Status Code> | <Message> |

## Testing Strategy

### Unit Tests
- <Test focus>
- <Test focus>

### Integration Tests
- <Test focus>

### Property-Based Tests
- <Test focus> (if applicable)

### Accessibility Tests
- <Test focus>

### Property-Based Testing Applicability

**Assessment**: [APPLICABLE / NOT APPLICABLE]

**Rationale**: <Explanation of why PBT is or is not appropriate for this feature>
```

### 4) Design Document (Bugfix Mode)

```markdown
# Design Document: <Bugfix Title>

## Overview

<Description of bug, root causes, and fix strategy>

## Glossary

- **Bug_Condition (C)**: <Formal definition>
- **Property (P)**: <Formal definition>
- **Preservation**: <Formal definition>

## Bug Details

### Bug Condition 1: <Title>

**Formal Specification:**
\`\`\`
FUNCTION isBugCondition_<Name>(input)
  INPUT: <input type>
  OUTPUT: boolean
  
  RETURN <condition>
END FUNCTION
\`\`\`

**Examples:**
- <Example that triggers bug>
- <Example that triggers bug>

## Hypothesized Root Cause

1. **Root Cause 1**: <Explanation>
   - **Location**: <File path>
   - **Evidence**: <Why we think this is the cause>

2. **Root Cause 2**: <Explanation>

## Correctness Properties

### Property 1: <Property Title>

*For any* <bug condition>, the fixed system SHALL <expected behavior>.

**Validates: Requirements 2.1, 2.2, 2.3**

### Property 2: Preservation - <Existing Behavior>

*For any* <non-buggy input>, the fixed system SHALL produce exactly the same behavior as the original system.

**Validates: Requirements 3.1, 3.2, 3.3**

## Fix Implementation

### Fix 1: <Fix Title>

**File**: <Path>

**Changes**:
1. <Change description>
2. <Change description>

## Testing Strategy

### Phase 1: Exploratory Bug Condition Checking

**Goal**: Surface counterexamples that demonstrate the bug BEFORE implementing the fix.

**Test Cases**:
1. <Test case that will fail on unfixed code>
2. <Test case that will fail on unfixed code>

**Expected Counterexamples**:
- <Bug manifestation>
- <Bug manifestation>

### Phase 2: Fix Checking

**Goal**: Verify that for all inputs where the bug condition holds, the fixed system produces the expected behavior.

### Phase 3: Preservation Checking

**Goal**: Verify that for all inputs where the bug condition does NOT hold, the fixed system preserves existing functionality.
```

### 5) Tasks Document (Feature Mode)

```markdown
# Implementation Plan: <Feature Name>

## Overview

<1-2 paragraph summary of implementation approach and phases>

## Tasks

### Phase 1: <Phase Name>

- [ ] 1. <Task Title>
  - Subtask 1
  - Subtask 2
  - _Requirements: 1.1, 1.2_

- [ ] 2. <Task Title>
  - Subtask 1
  - _Requirements: 2.1_

- [ ] 3. Checkpoint - <Phase Outcome>
  - Ensure all tests pass
  - Ask the user if questions arise

### Phase 2: <Phase Name>

- [ ] 4. <Task Title>
  - _Requirements: 3.1, 3.2_

- [ ] 5. Checkpoint - <Phase Outcome>
  - Verify no regressions
  - Ask the user if questions arise

## Notes

- Tasks marked with `*` are optional and can be skipped for MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation throughout development
```

### 6) Tasks Document (Bugfix Mode)

```markdown
# Implementation Plan: <Bugfix Title>

## Overview

<1-2 paragraph summary of three-phase testing and fix approach>

## Tasks

### Phase 1: Exploratory Bug Condition Tests

- [ ] 1. Write bug condition exploration test for <Bug Category 1>
  - **Property 1: Bug Condition** - <Bug Category 1>
  - **CRITICAL**: This test MUST FAIL on unfixed code
  - **DO NOT attempt to fix the test when it fails**
  - _Requirements: 1.1, 1.2, 1.3_

- [ ] 2. Checkpoint - Confirm bugs exist
  - Run tests on unfixed code
  - Document counterexamples found
  - Ask the user if questions arise

### Phase 2: Implementation

- [ ] 3. Implement Fix 1: <Fix Title>
  - Subtask 1
  - Subtask 2
  - _Requirements: 2.1, 2.2_

- [ ] 4. Verify fix checking tests pass
  - **Property 1: Expected Behavior** - <Bug Category 1>
  - Re-run tests from Phase 1
  - Verify they now pass
  - _Requirements: 2.1, 2.2_

- [ ] 5. Verify preservation tests pass
  - **Property 2: Preservation** - <Existing Behavior>
  - Confirm no regressions
  - _Requirements: 3.1, 3.2_

- [ ] 6. Checkpoint - Fix 1 complete
  - All tests pass
  - Ask the user if questions arise

### Phase 3: Final Validation

- [ ] 7. Run complete test suite
  - Verify all tests pass
  - Verify no new failures introduced
  - Ask the user if questions arise
```

## Procedure (Iterative Confirmation Cycle)

**DO NOT generate all three documents at once.** Each phase must be presented, confirmed, and optionally revised before moving to the next.

### Step 1: Analyze Repository
Read the repository to extract tech stack, code conventions, existing patterns, and domain model. Summarize findings to the user in 3-5 bullet points. Ask clarifying questions if the repo is ambiguous (see Context Gathering above).

### Step 2: Generate Requirements

1. Draft the requirements document using the appropriate template (feature or bugfix mode), fully populated from repo context and user input
2. **Present the draft to the user** — show the full document
3. Ask: *"Does this requirements document look correct? Would you like to add, remove, or change anything?"*
4. If the user requests changes, edit the document and re-present
5. Repeat until the user explicitly confirms: *"Requirements confirmed. Proceed to Design."*
6. **Do NOT move to the next phase until confirmation is received**

### Step 3: Generate Design

1. Draft the design document that maps to confirmed requirements and includes:
   - Architecture diagrams (Mermaid)
   - Correctness properties (3-10 formal properties)
   - Component specifications
   - Data models
   - Error handling strategy
   - Testing strategy with explicit PBT applicability assessment
2. **Present the draft to the user** — show the full document
3. Ask: *"Does this design document look correct? Would you like to add, remove, or change anything?"*
4. If the user requests changes, edit the document and re-present
5. Repeat until the user explicitly confirms: *"Design confirmed. Proceed to Tasks."*
6. **Do NOT move to the next phase until confirmation is received**

### Step 4: Generate Tasks

1. Draft the tasks document with:
   - Phased organization
   - Explicit checkpoints
   - Requirement traceability tags
   - Optional task marking
   - Specific acceptance criteria
2. **Present the draft to the user** — show the full document
3. Ask: *"Does this tasks document look correct? Would you like to add, remove, or change anything?"*
4. If the user requests changes, edit the document and re-present
5. Repeat until the user explicitly confirms: *"Tasks confirmed. Proceed to write."*
6. **Do NOT write to disk until confirmation is received**

### Step 5: Write to Disk

Only after all three phases are confirmed, create the spec folder and write:
- `.opencode/specs/<spec-name>/requirements.md`
- `.opencode/specs/<spec-name>/design.md`
- `.opencode/specs/<spec-name>/tasks.md`
- `.opencode/specs/<spec-name>/.config.opencode` (optional metadata)

After writing, confirm to the user: *"Spec written to `.opencode/specs/<spec-name>/`. All three documents are ready for implementation."*

## File Placement

```
.opencode/specs/<feature-name>/
├── requirements.md          (Phase 1)
├── design.md               (Phase 2)
├── tasks.md                (Phase 3)
└── .config.opencode            (optional)
```

## Quality Checklist

Before finalizing, verify:

- [ ] Repository was analyzed first (pom.xml, package.json, code patterns read)
- [ ] Tech stack was inferred from repo, not guessed
- [ ] All three documents follow the appropriate template (feature or bugfix)
- [ ] Glossary is comprehensive and used consistently
- [ ] Requirements are numbered hierarchically (1, 1.1, 1.2, 2, 2.1, etc.)
- [ ] Design includes 3-10 correctness properties
- [ ] Design explicitly addresses PBT applicability
- [ ] Tasks are phased with explicit checkpoints
- [ ] Every task has requirement traceability tags
- [ ] Tone is professional and precise
- [ ] Diagrams (if included) are clear and labeled
- [ ] No jargon without definition
- [ ] Preservation requirements are explicit (bugfix mode)
- [ ] Testing strategy is comprehensive and specific
- [ ] Requirements phase was explicitly confirmed by user before proceeding to Design
- [ ] Design phase was explicitly confirmed by user before proceeding to Tasks
- [ ] Tasks phase was explicitly confirmed by user before writing to disk
