# AGENTS.md

> AI Agent Context File - Provides navigation and context for AI coding assistants working with this codebase.

## Project Overview

**oidc-quarkus-demo** demonstrates EU Login (ECAS) OpenID Connect authentication with Quarkus. It serves as a reference implementation for European Commission projects migrating from traditional ECAS to OIDC-based authentication.

**Tech Stack:** Quarkus 3.34.3 • Java 21 • Maven

## Directory Map

```
src/main/java/eu/europa/ec/digit/eulogin/
├── GreetingResource.java          # Public endpoint (/hello)
├── resource/
│   ├── UserResource.java          # Auth lifecycle (/secure/*, /rest/*, /admin/*)
│   └── ProtectedResource.java     # Role-protected endpoints (/api/protected)
├── security/
│   ├── EcasSessionContext.java    # Request-scoped user context
│   └── EuLoginIdentityAugmentor.java  # Auth event logging
└── vo/
    └── UserVO.java                # User data transfer object

src/main/resources/
├── application.properties         # OIDC config (profile-based)
└── eulogin-realm.json            # Keycloak DevServices realm

docs/
└── oidc-configuration.md         # Detailed OIDC setup guide
```

## Key Entry Points

| Task | Start Here |
|------|-----------|
| Add REST endpoint | `UserResource.java` or create new `@Path` class in `resource/` |
| Add role-based access | Use `@RolesAllowed("role")` on method, see `ProtectedResource.java` |
| Modify user claims extraction | `EcasSessionContext.getCurrentUser()` |
| Configure OIDC | `application.properties` - note `%prod` prefix for EU Login settings |
| Add test users (dev) | `eulogin-realm.json` |

## Critical Patterns

### Profile-Based OIDC Configuration

The absence of `quarkus.oidc.auth-server-url` in dev/test profiles triggers Keycloak DevServices:

```properties
# Only active in prod - triggers EU Login
%prod.quarkus.oidc.auth-server-url=https://ecas.ec.europa.eu/cas/oauth2

# Shared settings - always active
quarkus.oidc.application-type=web-app
quarkus.oidc.authentication.pkce-required=true
```

### Request-Scoped User Context

`EcasSessionContext` lazily builds `UserVO` from `SecurityIdentity` because the augmentor runs before request scope is active:

```java
@RequestScoped
public class EcasSessionContext {
    @Inject SecurityIdentity identity;
    
    public UserVO getCurrentUser() {
        // Builds UserVO on first access within request
    }
}
```

### eCertis Equivalents

Classes contain Javadoc referencing their eCertis counterparts:
- `UserResource` → `StartUpController`
- `EcasSessionContext` → `EcasSessionContext` (same name, different impl)
- `EuLoginIdentityAugmentor` → `EcasAuthenticationSuccessListener`
- `@RolesAllowed` → `@PreAuthorize("hasAuthority('...')")`

## HTTP Permissions

Configured in `application.properties`:

| Paths | Policy | Notes |
|-------|--------|-------|
| `/hello`, `/rest/*`, `/q/*` | permit | Public, no auth required |
| `/secure/*` | authenticated | Triggers OIDC flow |
| `/admin/*` | authenticated | Logout endpoint |
| `/api/protected` | authenticated | Role check via `@RolesAllowed` |

## Test Users (DevServices)

| User | Password | Role |
|------|----------|------|
| test1 | test | administrator |
| editor1 | test | editor |

## Testing Pattern

Use `@TestSecurity` to mock authenticated users without OIDC flow:

```java
@QuarkusTest
class MyTest {
    @Test
    @TestSecurity(user = "test1", roles = "administrator")
    void testAdminEndpoint() {
        given().get("/api/protected").then().statusCode(200);
    }
}
```

## Configuration Files

| File | Purpose |
|------|---------|
| `pom.xml` | Dependencies managed via `quarkus-bom:3.34.3` |
| `application.properties` | OIDC, HTTP permissions, CORS |
| `eulogin-realm.json` | Keycloak realm for dev/test |

## Detailed Documentation

For comprehensive documentation, see `.agents/summary/`:
- `index.md` - Documentation index and quick reference
- `architecture.md` - System architecture with diagrams
- `components.md` - Detailed class documentation
- `interfaces.md` - REST API reference
- `workflows.md` - Authentication flows with sequence diagrams

## Custom Instructions
<!-- This section is for human and agent-maintained operational knowledge.
     Add repo-specific conventions, gotchas, and workflow rules here.
     This section is preserved exactly as-is when re-running codebase-summary. -->

