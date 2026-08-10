---
marp: true
theme: default
paginate: true
header: "GROW | Quarkus POC"
footer: "EU Login OIDC -- Quarkus vs Spring Boot"
---

# Quarkus as an Alternative to Spring Boot

## EU Login OIDC Integration -- Proof of Concept

GROW -- April 2026

---

# Context

- Our Java back-end applications typically use **Spring Boot 3.x** deployed in Tomcat or as standalone JARs
- EU Login (ECAS) is the mandatory authentication provider via CAS and **OpenID Connect**
- Goal: evaluate whether **Quarkus** is a viable alternative for new services

We built the **same EU Login OIDC integration** in both frameworks to compare them side by side.

---

# What is Quarkus?

- Java framework by Red Hat, designed for **cloud-native** and **container-first** workloads
- Uses **build-time processing** instead of runtime reflection -- faster startup, lower memory
- Built on proven standards: **Jakarta REST, CDI, MicroProfile, Vert.x**
- Supports **GraalVM native compilation** for sub-second startup
- Backed by Red Hat; used in production at scale

---

# Side-by-Side: EU Login OIDC Integration

| Aspect                  | Spring Boot 3.2                                             | Quarkus 3.34                                     |
|-------------------------|-------------------------------------------------------------|--------------------------------------------------|
| OIDC config             | `application.properties` + 3 Java config classes (~120 LOC) | `application.properties` only (0 config classes) |
| PKCE                    | Manual resolver wiring                                      | `pkce-required=true`                             |
| Token signature (PS512) | Custom `JwtDecoderFactory` bean                             | Auto-discovered from provider metadata           |
| Client auth             | `private_key_jwt` (keypair in properties)                   | `client_secret_jwt` (single secret)              |
| Security rules          | `SecurityFilterChain` bean                                  | Declarative in properties                        |
| Dev/test OIDC           | Requires real EU Login server                               | Keycloak DevServices (auto-starts in Docker)     |

---

# Code Volume Comparison

### Spring Boot

```
SecurityConfiguration.java    -- filter chain, PKCE, logout
OAuth2ClientConfig.java        -- JWK source, JWT resolver, token client
JWTConfig.java                 -- ID token decoder override
HomeController.java            -- 2 endpoints
2 Thymeleaf templates
```

### Quarkus

```
application.properties         -- entire OIDC + security config
ProtectedResource.java         -- role-protected endpoints
UserResource.java              -- user info endpoints
EcasSessionContext.java        -- request-scoped user context
```

**Zero boilerplate configuration classes in Quarkus.**

---

# Developer Experience

|                     | Spring Boot                 | Quarkus                                              |
|---------------------|-----------------------------|------------------------------------------------------|
| Live reload         | Spring DevTools (restart)   | **Continuous testing + hot reload** (no restart)     |
| Local OIDC provider | Must connect to real server | **Keycloak DevServices** -- zero setup, just Docker  |
| Dev UI              | Spring Actuator             | Built-in Dev UI at `/q/dev-ui` with OIDC token tools |
| Test support        | `@WithMockUser`             | `@TestSecurity` + `KeycloakTestClient`               |

Quarkus DevServices eliminated the need for an EU Login client registration during development.

---

# Deployment Models

### Spring Boot (current)

- **Standalone JAR** with embedded Tomcat (`java -jar app.jar`)
- **WAR** deployed to external Tomcat

### Quarkus

- **Standalone JAR** with embedded Vert.x (`java -jar quarkus-run.jar`)
- **Uber-JAR** -- single fat JAR (`./mvnw package -Dquarkus.package.jar.type=uber-jar`)
- **Native executable** -- GraalVM compiled, ~50ms startup (`./mvnw package -Dnative`)
- **Container image** -- built-in Jib/Docker support

> Quarkus does **not** deploy to Tomcat. It embeds its own HTTP server (Vert.x/Netty).
> This is by design: no servlet container management, no WAR packaging overhead.

---

# Why Not Tomcat?

Tomcat deployment adds operational complexity:

- Tomcat version management and patching
- Shared classloader issues between WARs
- Configuration split between app and container
- Scaling means scaling the whole Tomcat instance

Quarkus standalone deployment:

- **Self-contained** -- one artefact, one process
- **Immutable** -- the same artefact in dev, test, and prod
- **Container-friendly** -- small image, fast startup, low memory
- Aligns with **Docker/Kubernetes** deployment patterns

---

# Performance Characteristics

|               | Spring Boot (JVM)  | Quarkus (JVM)      | Quarkus (Native) |
|---------------|--------------------|--------------------|------------------|
| Startup time  | ~2-4s              | ~0.8-1.5s          | **~0.02-0.05s**  |
| Memory (RSS)  | ~150-250 MB        | ~80-130 MB         | **~30-50 MB**    |
| First request | After full startup | After full startup | **Near-instant** |

*Indicative ranges for a typical OIDC web-app. Actual numbers depend on extensions and workload.*

Native compilation is ideal for **serverless, scale-to-zero**, and **CLI tools**.
JVM mode is recommended for long-running services where peak throughput matters.

---

# Migration Path

Quarkus provides **Spring compatibility extensions** for incremental migration:

- `quarkus-spring-di` -- `@Autowired`, `@Component`, `@Service`
- `quarkus-spring-web` -- `@RestController`, `@GetMapping`
- `quarkus-spring-security` -- `@PreAuthorize`
- `quarkus-spring-data-jpa` -- Spring Data repositories

Teams can **reuse existing Spring knowledge** while gradually adopting CDI and Jakarta REST.

Not a full Spring runtime -- it's a compatibility layer compiled at build time.

---

# When to Choose Quarkus

**Good fit:**

- New microservices and APIs
- Container/Kubernetes deployments
- Serverless functions (AWS Lambda, Azure Functions)
- Applications where startup time and memory matter
- Teams willing to adopt Jakarta EE / MicroProfile standards

**Less ideal:**

- Existing large Spring Boot codebases (migration cost)
- Heavy reliance on Spring ecosystem libraries without Quarkus equivalents
- Requirement to deploy as WAR to existing Tomcat infrastructure

---

# Live Demo

1. Start the application: `./mvnw quarkus:dev`
2. Open `http://localhost:8080` -- dashboard shows the Authorization Code flow
3. **Login as administrator** (`test1` / `test`) -- observe the OIDC redirect to Keycloak
4. Test endpoints -- admin-only passes, editor-only is denied (403)
5. **Logout and login as editor** (`editor1` / `test`) -- roles reversed
6. Show Dev UI at `/q/dev-ui` -- OIDC token inspection

---

# Summary

|                       | Spring Boot 3.x         | Quarkus 3.x                          |
|-----------------------|-------------------------|--------------------------------------|
| Maturity              | Industry standard       | Production-ready, growing adoption   |
| EU Login OIDC         | Works, more boilerplate | Works, convention-driven             |
| Config classes needed | 3                       | 0                                    |
| Dev experience        | Good                    | Excellent (DevServices, live reload) |
| Deployment            | JAR or WAR/Tomcat       | JAR, uber-JAR, or native             |
| Startup (JVM)         | ~2-4s                   | ~0.8-1.5s                            |
| Memory (JVM)          | ~150-250 MB             | ~80-130 MB                           |

**Quarkus is a viable alternative** for new Java services in our ecosystem,
especially for containerized and cloud-native deployments.

---

# Questions?

**Resources:**

- Quarkus guides: https://quarkus.io/guides/
- OIDC guide: https://quarkus.io/guides/security-openid-connect
- Spring migration: https://quarkus.io/guides/spring-di
- This POC: `eulogin-oidc-quarkus-demo`
- Spring Boot comparison: `openid-connect-springboot3-demo`
