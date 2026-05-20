# Feature Spec Generator - Quick Reference

## Two Operating Modes

### 🎯 FEATURE Mode
**When**: Creating new functionality, user-facing capabilities, architectural additions

**Key Sections**:
- User stories with value proposition
- Design principles (3-5 guiding principles)
- Correctness properties (what should always be true)
- Component interfaces and data flows
- Comprehensive testing strategy

**Examples**: Direct Messaging, Mobile-First Redesign, OAuth Integration

**Typical Structure**:
```
Phase 1: Requirements
  - Glossary
  - Requirements (1, 1.1, 1.2, 2, 2.1, etc.)
  - Scope (in-scope, out-of-scope)

Phase 2: Design
  - Architecture (with Mermaid diagram)
  - Components and interfaces
  - Data models
  - Correctness properties (3-10)
  - Error handling
  - Testing strategy (with PBT applicability)

Phase 3: Tasks
  - Phase 1: Foundation
  - Phase 2: Implementation
  - Phase 3+: Integration, testing, optional enhancements
  - Checkpoints after each phase
```

---

### 🐛 BUGFIX Mode
**When**: Fixing defects, security vulnerabilities, regression prevention

**Key Sections**:
- Formal bug condition specifications
- Root cause analysis
- Correctness properties (fix validation)
- Preservation properties (regression prevention)
- Three-phase testing (exploratory → fix checking → preservation)

**Examples**: Chat Functionality Fixes, CI Test Failures, Security Hardening

**Typical Structure**:
```
Phase 1: Requirements
  - Glossary (Bug_Condition, Property, Preservation)
  - Bug Analysis (current defect vs expected correct)
  - Unchanged Behavior (regression prevention)

Phase 2: Design
  - Bug details with formal specifications
  - Root cause analysis
  - Correctness properties (fix validation)
  - Preservation properties (regression prevention)
  - Fix implementation details
  - Three-phase testing strategy

Phase 3: Tasks
  - Phase 1: Exploratory tests (MUST FAIL on unfixed code)
  - Phase 2: Implementation (with fix checking and preservation)
  - Phase 3: Final validation
  - Checkpoints after each phase
```

---

## Input Checklist

### Common to Both Modes
- [ ] Spec name (kebab-case, e.g., `notification-system`)
- [ ] Short summary (1-2 sentences)
- [ ] Target stack (Spring Boot version, Java version, Next.js version)
- [ ] Non-functional constraints (performance, scale, security, accessibility)
- [ ] External dependencies or integrations

### Feature Mode Only
- [ ] User stories (3-5 personas and their goals)
- [ ] In-scope vs out-of-scope items
- [ ] Design principles (3-5 guiding principles)

### Bugfix Mode Only
- [ ] Bug description (what's broken and how users experience it)
- [ ] Reproduction steps (how to trigger the bug)
- [ ] Impact (severity, affected users, data loss risk)
- [ ] Root cause hypothesis (initial theory)

---

## Key Patterns

### Requirement Numbering
```
Requirement 1: <Title>
  1.1 WHEN <condition>, THE <system> SHALL <behavior>
  1.2 WHEN <condition>, THE <system> SHALL <behavior>

Requirement 2: <Title>
  2.1 WHEN <condition>, THE <system> SHALL <behavior>
  2.2 WHEN <condition>, THE <system> SHALL <behavior>
```

### Correctness Properties
```
### Property 1: <Property Title>

*For any* <input condition>, the system SHALL <expected behavior>.

**Validates: Requirements 1.1, 1.2, 2.3**
```

### Requirement Traceability in Tasks
```
- [ ] 1. <Task Title>
  - Subtask 1
  - Subtask 2
  - _Requirements: 1.1, 1.2_

- [ ] 2. Checkpoint - <Phase Outcome>
  - Ensure tests pass
  - Ask the user if questions arise
```

### Optional Tasks
```
- [ ] 5.* Write property-based tests
  - Generate random inputs
  - Verify correctness properties
  - _Requirements: 3.1, 3.2_
```

---

## Quality Checklist

Before finalizing any spec, verify:

- [ ] **Structure**: All three documents present (requirements, design, tasks)
- [ ] **Glossary**: Comprehensive and used consistently throughout
- [ ] **Numbering**: Hierarchical (1, 1.1, 1.2, 2, 2.1, etc.)
- [ ] **Properties**: 3-10 correctness properties in design
- [ ] **PBT Assessment**: Explicit statement of applicability and rationale
- [ ] **Traceability**: Every task tagged with `_Requirements: X.X_`
- [ ] **Checkpoints**: Explicit validation points after each phase
- [ ] **Tone**: Professional, precise, actionable
- [ ] **Diagrams**: Clear, labeled, add value (not decorative)
- [ ] **Terminology**: Consistent use of glossary terms
- [ ] **Preservation**: Explicit regression prevention requirements (bugfix mode)
- [ ] **Testing**: Comprehensive strategy with specific test cases

---

## File Placement

```
.kiro/specs/<spec-name>/
├── requirements.md          (Phase 1)
├── design.md               (Phase 2)
├── tasks.md                (Phase 3)
└── .config.kiro            (optional metadata)
```

---

## Common Mistakes to Avoid

❌ **Don't**: Skip the glossary
✅ **Do**: Define all domain-specific terms upfront

❌ **Don't**: Use vague acceptance criteria ("should work", "be fast")
✅ **Do**: Use formal "WHEN...THEN...SHALL" language with measurable criteria

❌ **Don't**: Forget requirement traceability in tasks
✅ **Do**: Tag every task with `_Requirements: X.X_`

❌ **Don't**: Skip preservation requirements (bugfix mode)
✅ **Do**: Explicitly state what must NOT change

❌ **Don't**: Forget to assess PBT applicability
✅ **Do**: Explicitly state whether PBT is applicable and why

❌ **Don't**: Create tasks without checkpoints
✅ **Do**: Add explicit validation points after each phase

❌ **Don't**: Mix feature and bugfix modes
✅ **Do**: Choose one mode and follow its structure consistently

---

## Example Acceptance Criteria

### Feature Mode
```
1. WHEN a user creates a new notification preference, THE system SHALL persist it to the database
2. WHEN a user disables notifications for a type, THE system SHALL NOT send notifications of that type
3. IF a user has no preferences set, THEN THE system SHALL use default preferences (all enabled)
```

### Bugfix Mode
```
1.1 WHEN a user sends 5 messages rapidly, THEN the system may display them out of order
1.2 WHEN messages are retrieved from history, THEN they may not be in chronological order

2.1 WHEN multiple messages are sent to a room, THE system SHALL process them in the order they were received
2.2 WHEN messages are persisted, THE system SHALL use database timestamps to ensure chronological ordering

3.1 WHEN a user sends a single message, THE system SHALL CONTINUE TO persist and broadcast it normally
3.2 WHEN messages are retrieved from history, THE system SHALL CONTINUE TO support pagination
```

---

## Testing Strategy Template

### Feature Mode
```
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

**Rationale**: <Explanation of why PBT is or is not appropriate>
```

### Bugfix Mode
```
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

---

## When to Use Property-Based Testing

### ✅ APPLICABLE
- Deterministic functions with clear input/output
- Idempotent operations (calling twice = calling once)
- Mathematical properties that should hold for all inputs
- Validation logic with many edge cases
- Sorting, filtering, transformation functions

**Examples**:
- DM room creation idempotency (calling twice returns same room)
- Message sanitization (sanitizing twice = sanitizing once)
- User search filtering (all results match query)

### ❌ NOT APPLICABLE
- UI rendering and layout
- WebSocket/STOMP protocol behavior
- Infrastructure configuration
- Side-effect operations (broadcasting, persistence)
- External service integration
- Real-time communication flows

**Examples**:
- Responsive design (requires visual inspection)
- STOMP message routing (external service)
- Database schema (infrastructure)
- Message broadcasting (side effects)

---

## Repository Spec Examples

### Feature Mode Examples
- **Direct Messaging** (`.kiro/specs/direct-messaging/`)
- **Frontend Splash and Landing Redesign** (`.kiro/specs/frontend-splash-and-landing-redesign/`)
- **Mobile-First Chat Redesign** (`.kiro/specs/mobile-first-chat-redesign/`)
- **OAuth Email Verification Integration** (`.kiro/specs/oauth-email-verification-integration/`)

### Bugfix Mode Examples
- **Chat Functionality Fixes** (`.kiro/specs/chat-functionality-fixes-race-condition-websocket-errors/`)
- **CI Phase 2 Test Failures** (`.kiro/specs/ci-phase2-test-failures-fix/`)
- **Security Hardening** (`.kiro/specs/security-hardening-websocket-xss-csrf/`)

---

## Quick Start

1. **Determine Mode**: Feature or Bugfix?
2. **Gather Inputs**: Use the input checklist above
3. **Generate Requirements**: Use appropriate template
4. **Generate Design**: Include architecture, properties, testing strategy
5. **Generate Tasks**: Organize into phases with checkpoints
6. **Quality Check**: Verify against the quality checklist
7. **Present & Confirm**: Show user before writing to disk
8. **Write to Disk**: Create spec folder and files

---

## Support

For detailed information, see:
- **Full Documentation**: `.github/skills/feature-spec-generator/SKILL.md`
- **Enhancement Summary**: `.kiro/SPEC_GENERATOR_ENHANCEMENT_SUMMARY.md`
- **Repository Examples**: `.kiro/specs/*/`
