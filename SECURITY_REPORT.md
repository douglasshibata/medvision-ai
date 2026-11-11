# MedVision AI - Security Audit Report

**Date:** 2026-03-12
**Scope:** Full-stack security review (Spring Boot backend, Python AI worker, static frontend, Docker infrastructure)
**Classification:** CONFIDENTIAL - Contains vulnerability details for PHI/PII processing system

---

## Executive Summary

This audit identified **5 Critical**, **6 High**, **7 Medium**, and **3 Low** severity vulnerabilities across the MedVision AI platform. Given that this system processes Protected Health Information (PHI) and Personally Identifiable Information (PII) under HIPAA/LGPD regulations, the critical findings require immediate remediation before any production deployment.

Key risk areas:
- **Authentication bypass** via hardcoded default secrets
- **Missing security headers** exposing the application to XSS, clickjacking, and MIME sniffing attacks
- **Unauthenticated service-to-service communication** between AI worker and backend
- **Sensitive patient data leakage** through audit logs and stdout

---

## Vulnerabilities Found

### CRITICAL

| # | Issue | File | Line | Description | CVSS | Remediation |
|---|-------|------|------|-------------|------|-------------|
| C1 | Default JWT Secret | `application.properties` | 12 | Hardcoded default `change-me-in-production-must-be-at-least-32-characters-long!!` allows token forgery if `JWT_SECRET` env var not set | 9.8 | Fail-fast startup validation rejecting default/placeholder values |
| C2 | Default API Key | `application.properties` | 14 | Hardcoded default `change-me-in-production` allows unauthorized access if `AUTH_API_KEY` env var not set | 9.8 | Fail-fast startup validation rejecting default/placeholder values |
| C3 | Missing Security Headers | `SecurityConfig.java` | 26-38 | No CSP, X-Frame-Options, X-Content-Type-Options, HSTS headers configured. Exposes to XSS, clickjacking, MIME sniffing | 7.5 | Add comprehensive security headers via Spring Security `.headers()` config |
| C4 | AI Worker → Backend Unauthenticated | `worker.py` | 149 | `requests.post(BACKEND_URL, json=result)` sends results without any authentication token. Any network-accessible attacker can inject fake analysis results | 9.1 | Add service-to-service API key authentication |
| C5 | Timing Attack on API Key | `AuthController.java` | 30 | `apiKey.equals(key)` is vulnerable to timing side-channel attacks; allows character-by-character brute force of API key | 7.5 | Use `MessageDigest.isEqual()` for constant-time comparison |

### HIGH

| # | Issue | File | Line | Description | CVSS | Remediation |
|---|-------|------|------|-------------|------|-------------|
| H1 | Sensitive Data in Audit Logs | `AuditAspect.java` | 32-34 | Logs full method arguments including patient names, file paths, and DICOM metadata. Violates HIPAA minimum necessary principle | 7.5 | Remove raw argument logging; log only method name, class, and status |
| H2 | Rate Limit Bypass via X-Forwarded-For | `RateLimitFilter.java` | 38 | Uses `request.getRemoteAddr()` which returns proxy IP when behind reverse proxy, allowing rate limit bypass | 5.3 | Parse `X-Forwarded-For` header for real client IP |
| H3 | Auth Endpoint Exempt from Rate Limiting | `RateLimitFilter.java` | 54 | `/auth` excluded from rate limit, enabling unlimited brute-force attempts on API key | 7.5 | Remove `/auth` from rate limit exemptions |
| H4 | No CORS Configuration | `SecurityConfig.java` | - | Missing explicit CORS policy; browser-based attacks may exploit default permissive behavior | 6.1 | Add restrictive CORS configuration with allowed origins from env var |
| H5 | ConcurrentHashMap Memory Leak | `RateLimitFilter.java` | 21 | Buckets map grows unbounded with no eviction policy. Attackers can exhaust memory by sending requests from many IPs | 5.3 | Add scheduled cleanup to evict stale IP bucket entries |
| H6 | No DICOM File Type Validation | `DicomController.java` | 27 | No validation that uploaded file is actually a DICOM file before processing. May allow arbitrary file upload | 6.5 | Validate DICOM preamble magic bytes and file extension |

### MEDIUM

| # | Issue | File | Line | Description | CVSS | Remediation |
|---|-------|------|------|-------------|------|-------------|
| M1 | No Input Validation Annotations | `AnalysisController.java` | 23 | `@RequestBody` without `@Valid`; no field constraints on `AnalysisResult` | 5.3 | Add `@Valid` annotation and Jakarta validation constraints |
| M2 | Path Traversal in WadoController | `WadoController.java` | 62-71 | `findDicomFile()` ignores `instanceUID` parameter, returns first `.dcm` file found. Path variables not validated | 5.3 | Validate path variables against allowed patterns |
| M3 | No Redis Authentication | `docker-compose.yml` | 14-17 | Redis has no password configured; accessible to any container on the network | 6.5 | Add `--requirepass` and configure Spring Redis password |
| M4 | Frontend CDN Scripts Without SRI | `index.html` | 7-10 | Tailwind CSS and Cornerstone.js loaded from CDN without Subresource Integrity hashes | 5.3 | Add `integrity` and `crossorigin` attributes |
| M5 | Python Dependencies Unpinned | `requirements.txt` | 1-7 | Uses `>=` constraints allowing arbitrary future versions. Supply chain risk | 5.3 | Pin exact versions with `==` |
| M6 | Patient Data in Print Statements | `worker.py` | 88,144,151 | Patient names and study UIDs printed to stdout. Log aggregation systems may capture PHI | 5.3 | Replace `print()` with `logging` module; sanitize patient identifiers |
| M7 | No DB Password in Backend Docker Env | `docker-compose.yml` | 26-28 | `SPRING_DATASOURCE_PASSWORD` not set in backend environment; DB connection will fail or use empty password | 5.3 | Pass `POSTGRES_PASSWORD` to backend service |

### LOW

| # | Issue | File | Line | Description | CVSS | Remediation |
|---|-------|------|------|-------------|------|-------------|
| L1 | System.out.println in Production | `AiQueueService.java` | 26 | Uses stdout instead of structured logging framework | 2.0 | Replace with SLF4J logger |
| L2 | Generic Exception Catching in JWT | `JwtUtil.java` | 47 | `catch (Exception e)` swallows all errors silently including unexpected runtime exceptions | 3.0 | Log token validation failures at DEBUG level |
| L3 | No Actuator Security | `SecurityConfig.java` | 31 | `/actuator/health` is public, but other actuator endpoints aren't explicitly blocked | 3.0 | Restrict actuator to health endpoint only |

---

## Dependency Update Recommendations

| Component | Current | Recommended | Reason |
|-----------|---------|-------------|--------|
| Spring Boot | 3.4.3 | 3.4.4+ | Latest security patches |
| Bucket4j | 8.14.0 | 8.14.0 | Already current |
| jjwt | 0.12.6 | 0.12.6 | Already current |
| dcm4che | 5.33.1 | 5.33.1 | Already current |
| PostgreSQL image | 15 | 16 | Better performance, security improvements |
| Redis image | 7 | 7-alpine | Smaller attack surface |
| Python Pillow | >=10.0.0 | ==11.1.0 | Pin exact, latest security patches |
| Python requests | >=2.32.0 | ==2.32.3 | Pin exact version |
| Python numpy | >=2.2.0 | ==2.2.3 | Pin exact version |

---

## Remediation Summary

### Files Modified

| File | Fixes Applied |
|------|---------------|
| `SecurityConfig.java` | C3 (security headers), H4 (CORS) |
| `AuthController.java` | C2 (fail-fast validation), C5 (timing-safe comparison) |
| `JwtUtil.java` | C1 (fail-fast secret validation), L2 (logging) |
| `RateLimitFilter.java` | H2 (X-Forwarded-For), H3 (auth rate limit), H5 (bucket eviction) |
| `DicomController.java` | H6 (DICOM validation) |
| `AnalysisController.java` | M1 (@Valid annotation) |
| `WadoController.java` | M2 (path variable validation) |
| `AuditAspect.java` | H1 (sanitize logs) |
| `AnalysisResult.java` | M1 (validation annotations) |
| `application.properties` | M3 (Redis password), H4 (CORS origins) |
| `worker.py` | C4 (auth headers), M6 (structured logging) |
| `requirements.txt` | M5 (pinned versions) |
| `docker-compose.yml` | M3 (Redis auth), M7 (DB password), C4 (worker API key) |
| `frontend/index.html` | M4 (SRI hashes) |

### Compliance Notes

- **HIPAA**: Audit log sanitization (H1) and patient data removal from stdout (M6) address minimum necessary principle
- **LGPD**: Same data minimization fixes apply under Brazilian data protection law
- **OWASP Top 10**: Fixes address A01 (Broken Access Control), A02 (Cryptographic Failures), A03 (Injection), A05 (Security Misconfiguration), A07 (Authentication Failures)

---

## Verification Checklist

- [ ] Backend compiles: `cd backend && mvn compile`
- [ ] Docker Compose valid: `docker compose config`
- [ ] Python syntax: `python -m py_compile ai-worker/worker.py`
- [ ] Application rejects default placeholder secrets on startup
- [ ] Security headers present in HTTP responses
- [ ] AI Worker authenticates to backend
- [ ] Rate limiting applies to `/auth` endpoint
- [ ] Audit logs contain no patient data
- [ ] Redis requires authentication
