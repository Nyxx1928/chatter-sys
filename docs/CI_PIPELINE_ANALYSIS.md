# CI Pipeline Analysis — `.github/workflows/ci.yml`

## Strengths

1. **Clear phased architecture** — 7 distinct stages with explicit `needs:` dependencies make the pipeline easy to reason about and debug.
2. **Defense-in-depth security** — SBOM generation (Anchore), vulnerability scanning (Trivy), and image signing (Cosign) cover the software supply chain well.
3. **Test pyramid alignment** — Unit → Integration → E2E separation with appropriate test selectors (`*Test`, `*IntegrationTest`, Playwright).
4. **Property-based testing** — jqwik inclusion shows maturity beyond basic unit tests.
5. **Conditional execution** — Playwright detects config existence before running; Docker steps gate on Dockerfile presence and push events.
6. **Summary phase with `always()`** — Guarantees visibility into all phase results regardless of failures.

## Weaknesses

| # | Issue | Impact |
|---|-------|--------|
| 1 | **Fully sequential phases** — Each phase re-checks out code and re-resolves dependencies from scratch | ~2-3x slower than necessary |
| 2 | **No `paths-ignore` / change detection** — A `README.md` edit triggers the entire 7-phase pipeline | Wasted CI minutes |
| 3 | **No Maven dependency caching beyond `setup-java` cache** — No local repo cache key pinning | Unpredictable cache hits |
| 4 | **No `testcontainers` or service containers for integration tests** — `*IntegrationTest` found 0 tests, likely because no DB is available | Integration tests are a no-op |
| 5 | **No code coverage collection or gates** — No JaCoCo, no coverage thresholds, no PR coverage diff | No quality regression detection |
| 6 | **No `timeout-minutes` on jobs** — A hung test or download blocks the runner indefinitely | CI can stall silently |
| 7 | **Docker build happens after E2E** — The Docker image is built in Phase 5, but E2E (Phase 4) runs against a dev server, not the production image | E2E doesn't validate the actual artifact |
| 8 | **No multi-platform or matrix builds** — Single `ubuntu-latest`, single Java 17, single Node 20 | No compatibility guarantees |
| 9 | **Cosign signing only on push with DockerHub secrets** — PR builds never get signed; no keyless transparency log verification | Supply chain security gap for PRs |
| 10 | **No artifact attestation** — SBOMs are files, not in-toto attestations bound to the image | SBOMs can be decoupled from images |
| 11 | **No retry logic for flaky E2E** — Playwright has 165 tests across 5 browsers; any flaky test fails the entire phase | Brittle CI |
| 12 | **No `concurrency` group** — Multiple pushes to the same branch run all phases in parallel, wasting resources | Resource waste |

## Recommended Improvements

### 1. Add concurrency control and path filtering

```yaml
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true

# Add to each job:
paths-ignore:
  - '**.md'
  - 'docs/**'
  - '.gitignore'
```

### 2. Parallelize independent phases

Phases 2 (unit), 3 (integration), and frontend lint can run in parallel after Phase 1:

```yaml
phase2-unit-tests:
  needs: [phase1-lint-validate]

phase3-integration-tests:
  needs: [phase1-lint-validate]  # not phase2

phase4-e2e-playwright:
  needs: [phase1-lint-validate]  # can start earlier
```

### 3. Add service containers for integration tests

```yaml
services:
  postgres:
    image: postgres:16-alpine
    env:
      POSTGRES_DB: testdb
      POSTGRES_USER: test
      POSTGRES_PASSWORD: test
    ports: ['5432:5432']
    options: >-
      --health-cmd pg_isready
      --health-interval 10s
      --health-timeout 5s
      --health-retries 5
```

### 4. Add coverage with JaCoCo and gates

```xml
<!-- pom.xml -->
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <version>0.8.12</version>
  <executions>
    <execution>
      <goals><goal>prepare-agent</goal><goal>report</goal><goal>check</goal></goals>
      <configuration>
        <rules>
          <rule><limits><limit><counter>LINE</counter><value>COVEREDRATIO</value><minimum>0.80</minimum></limit></limits></rule>
        </rules>
      </configuration>
    </execution>
  </executions>
</plugin>
```

### 5. Add timeouts and retries

```yaml
phase2-unit-tests:
  timeout-minutes: 15

phase4-e2e-playwright:
  timeout-minutes: 30
  # In Playwright config:
  # retries: process.env.CI ? 2 : 0
```

### 6. Build Docker earlier and test against the actual image

Move Docker build to run in parallel with tests, then run E2E against the built container:

```yaml
phase5-test-build-docker:
  needs: [phase1-lint-validate]  # start early

phase4-e2e-playwright:
  needs: [phase5-test-build-docker]  # E2E runs against built image
```

### 7. Add `setup-java` cache key pinning

```yaml
- uses: actions/setup-java@v5
  with:
    cache: maven
- run: mvn -DskipTests=true dependency:go-offline  # pre-fetch all deps
```

### 8. Add PR-specific lightweight mode

```yaml
on:
  pull_request:
    paths:
      - 'src/**'
      - 'frontend/**'
      - 'pom.xml'
      - 'frontend/package.json'
      - '.github/workflows/ci.yml'
```

### 9. Add dependency scanning (not just container scanning)

```yaml
- name: Maven dependency audit
  run: mvn org.owasp:dependency-check-maven:check -DfailBuildOnCVSS=7

- name: npm audit
  working-directory: frontend
  run: npm audit --audit-level=high
```

### 10. Use in-toto attestations for SBOMs

Replace the file-based SBOM with proper image attestations:

```yaml
- uses: anchore/sbom-action@v0
  with:
    image: ${{ env.BACKEND_IMAGE }}:${{ env.IMAGE_TAG }}
    format: spdx-json
    push-to-registry: true  # attaches as OCI attestation
```

## Priority Ranking

| Priority | Improvement | Effort | Impact |
|----------|------------|--------|--------|
| P0 | Add `timeout-minutes` to all jobs | 5 min | Prevents silent CI hangs |
| P0 | Add `concurrency` group | 2 min | Saves CI minutes immediately |
| P1 | Parallelize phases 2-4 | 10 min | ~40% faster feedback |
| P1 | Add service containers for integration tests | 30 min | Makes Phase 3 actually run |
| P2 | Add JaCoCo coverage gates | 20 min | Quality regression detection |
| P2 | Add OWASP dependency-check | 10 min | Catches vulnerable deps early |
| P3 | Build Docker earlier, test against it | 45 min | E2E validates production artifact |
| P3 | In-toto SBOM attestations | 20 min | Proper supply chain security |
