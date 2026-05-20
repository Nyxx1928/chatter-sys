# CI Pipeline Improvement Summary

This note combines the strengths observed across the two analyses and proposes a lean set of improvements that boost robustness while keeping the pipeline fast and efficient.

## Combined Strengths

1. **Clear phased architecture** with explicit `needs:` dependencies.
2. **Defense-in-depth security** via SBOM generation, vulnerability scanning, and image signing.
3. **Test pyramid alignment** across unit, integration, and E2E stages.
4. **Property-based testing** (jqwik) for deeper correctness coverage.
5. **Conditional execution** to skip steps when configs are missing.
6. **Summary phase with `always()`** for consistent visibility.

## Improvements That Keep It Fast and Robust

### P0: Reliability and speed wins

- **Add `concurrency` + cancel in-progress** to avoid wasting CI minutes on superseded runs.
- **Add `paths-ignore` / change detection** so docs-only changes do not run the full pipeline.
- **Add `timeout-minutes`** per job to prevent silent hangs.

### P1: Faster feedback without extra complexity

- **Parallelize independent phases** after lint (unit, integration, E2E can start together).
- **Improve dependency caching** with `mvn dependency:go-offline` and npm cache reuse.

### P1: Make integration tests real (not heavier)

- **Add service containers (Postgres)** so integration tests run consistently and do not become no-ops.

### P2: Stability without overengineering

- **Add minimal E2E retries in CI** (e.g., 1-2 retries) to reduce flaky failures.

## Suggestions to Keep Heavy Work Optional

- **Run OWASP dependency-check and SBOM attestation only on `main` or nightly** to keep PRs fast.
- **Keep Docker build in later phases for PRs**, but consider build cache for main to reduce time.

## When to Run OWASP and SBOM Checks

- **Run on `main` + nightly** for full coverage without slowing every PR.
- **Run on PRs only when dependency files change** (`pom.xml`, `package.json`, lockfiles).

### What "nightly run" means

A nightly run is a scheduled CI run (usually once per day, e.g., 2:00 AM UTC) that executes heavier checks on the latest `main` branch. It helps catch issues like new CVEs or dependency updates without adding overhead to every PR.

## Minimal Example Snippets

### Concurrency and path filtering

```yaml
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true

on:
  pull_request:
    paths:
      - "src/**"
      - "frontend/**"
      - "pom.xml"
      - "frontend/package.json"
      - ".github/workflows/ci.yml"
```

### Timeouts

```yaml
jobs:
  phase2-unit-tests:
    timeout-minutes: 15
  phase4-e2e-playwright:
    timeout-minutes: 30
```

### Service container for integration tests

```yaml
services:
  postgres:
    image: postgres:16-alpine
    env:
      POSTGRES_DB: testdb
      POSTGRES_USER: test
      POSTGRES_PASSWORD: test
    ports: ["5432:5432"]
    options: >-
      --health-cmd pg_isready
      --health-interval 10s
      --health-timeout 5s
      --health-retries 5
```

## Outcome Summary

- **Robustness** improves immediately with timeouts, service containers, and flaky test retries.
- **Speed** improves with concurrency, path filtering, and parallelization.
- **No overengineering**: heavier checks remain optional for `main` or nightly runs.
