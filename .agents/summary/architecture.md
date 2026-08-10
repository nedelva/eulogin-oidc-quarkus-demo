# Architecture

## System Overview

This application demonstrates EU Login OIDC integration with Quarkus using the Authorization Code flow with PKCE. It supports two authentication providers through profile-based configuration.

```mermaid
graph TB
    subgraph "Client Layer"
        Browser[Browser/SPA]
    end
    
    subgraph "Quarkus Application"
        REST[REST Endpoints]
        SEC[Security Layer]
        CTX[Session Context]
    end
    
    subgraph "OIDC Providers"
        KC[Keycloak DevServices<br/>dev/test]
        EU[EU Login ECAS<br/>prod]
    end
    
    Browser --> REST
    REST --> SEC
    SEC --> CTX
    SEC -.-> KC
    SEC -.-> EU
```

## Authentication Architecture

### OIDC Authorization Code Flow with PKCE

```mermaid
sequenceDiagram
    participant B as Browser
    participant Q as Quarkus App
    participant OP as OIDC Provider
    
    B->>Q: GET /secure/*
    Q->>Q: Check session cookie
    Note over Q: No valid session
    Q->>B: 302 Redirect to OP /authorize
    Note over Q: Include code_challenge (PKCE)
    B->>OP: GET /authorize
    OP->>B: Login page
    B->>OP: POST credentials
    OP->>B: 302 Redirect to callback
    Note over OP: Include authorization code
    B->>Q: GET /secure/ecas/callback?code=...
    Q->>OP: POST /token
    Note over Q: Include code_verifier (PKCE)
    OP->>Q: ID Token + Access Token
    Q->>Q: Validate ID Token
    Q->>Q: Create session, set cookie
    Q->>B: 302 Redirect to original URL
    B->>Q: GET /secure/* (with cookie)
    Q->>B: 200 Protected resource
```

## Profile-Based Configuration

The application uses Quarkus profiles to switch between OIDC providers:

```mermaid
graph LR
    subgraph "Configuration"
        AP[application.properties]
    end
    
    subgraph "Profiles"
        DEV[dev profile]
        TEST[test profile]
        PROD[prod profile]
    end
    
    subgraph "Providers"
        KDS[Keycloak DevServices]
        EUL[EU Login]
    end
    
    AP --> DEV
    AP --> TEST
    AP --> PROD
    DEV --> KDS
    TEST --> KDS
    PROD --> EUL
```

| Setting | Dev/Test | Production |
|---------|----------|------------|
| `quarkus.oidc.auth-server-url` | *Not set* (triggers DevServices) | `https://ecas.ec.europa.eu/cas/oauth2` |
| `quarkus.oidc.client-id` | Auto-configured | `eulogin-oicd-quarkus-demo` |
| Client Authentication | `client_secret_basic` | `client_secret_jwt` |

## Security Layers

```mermaid
graph TB
    subgraph "HTTP Request"
        REQ[Incoming Request]
    end
    
    subgraph "Security Pipeline"
        PERM[HTTP Auth Permissions]
        OIDC[OIDC Authentication]
        AUG[Identity Augmentor]
        RBAC[@RolesAllowed]
    end
    
    subgraph "Application"
        RES[REST Resource]
        CTX[EcasSessionContext]
    end
    
    REQ --> PERM
    PERM --> |authenticated path| OIDC
    PERM --> |public path| RES
    OIDC --> AUG
    AUG --> RBAC
    RBAC --> RES
    RES --> CTX
```

### HTTP Auth Permissions

Configured in `application.properties`:

| Permission | Paths | Policy |
|------------|-------|--------|
| public | `/hello`, `/rest/user`, `/rest/me`, `/q/*` | permit |
| secured | `/secure/*` | authenticated |
| admin | `/admin/*` | authenticated |
| protected | `/api/protected` | authenticated |

### Role-Based Access Control

Applied via `@RolesAllowed` annotations at the method level:

- `@RolesAllowed("administrator")` - Admin-only endpoints
- `@RolesAllowed("editor")` - Editor-only endpoints
- `@PermitAll` - Public endpoints (explicit)

## Component Relationships

```mermaid
classDiagram
    class SecurityIdentity {
        <<Quarkus>>
        +getPrincipal()
        +getRoles()
        +getAttribute()
        +isAnonymous()
    }
    
    class EuLoginIdentityAugmentor {
        +augment(identity, context)
    }
    
    class EcasSessionContext {
        -currentUser: UserVO
        +getCurrentUser()
    }
    
    class UserResource {
        +postLogin()
        +getUserDetails()
        +getCurrentUser()
        +logout()
    }
    
    class ProtectedResource {
        +adminOnly()
        +editorOnly()
    }
    
    class UserVO {
        -username
        -name
        -emailAddress
        -roles
    }
    
    SecurityIdentity <|-- EuLoginIdentityAugmentor : augments
    SecurityIdentity <-- EcasSessionContext : injects
    SecurityIdentity <-- UserResource : injects
    SecurityIdentity <-- ProtectedResource : injects
    EcasSessionContext <-- UserResource : injects
    EcasSessionContext ..> UserVO : creates
    UserResource ..> UserVO : returns
```

## Design Decisions

### Why PKCE?
EU Login mandates PKCE (RFC 7636) for all OIDC clients. This prevents authorization code interception attacks. Quarkus handles the `code_challenge`/`code_verifier` exchange automatically when `pkce-required=true`.

### Why client_secret_jwt?
EU Login does not accept plain client secrets. The application must sign a JWT with the shared secret and send it to the token endpoint. This is configured via `quarkus.oidc.credentials.jwt.secret`.

### Why Request-Scoped Context?
The `SecurityIdentityAugmentor` runs before the request scope is active, so user data cannot be stored in a request-scoped bean during authentication. Instead, `EcasSessionContext` lazily builds the `UserVO` from `SecurityIdentity` during the first access within a request.

### Why DevServices?
Keycloak DevServices provides a zero-configuration local OIDC provider, eliminating the need for manual Keycloak setup. The realm configuration (`eulogin-realm.json`) ensures the local environment mirrors EU Login's behavior (PKCE, same claims structure).
