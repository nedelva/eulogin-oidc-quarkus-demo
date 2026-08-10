# Workflows

## Authentication Workflows

### Login Flow

```mermaid
sequenceDiagram
    actor U as User
    participant B as Browser
    participant Q as Quarkus App
    participant OP as OIDC Provider
    
    U->>B: Navigate to /secure/ecas
    B->>Q: GET /secure/ecas
    Note over Q: No valid session cookie
    Q->>B: 302 Redirect
    Note over Q: Location: {OP}/authorize?<br/>client_id=...&<br/>redirect_uri=/secure/ecas/callback&<br/>code_challenge=...&<br/>scope=openid email profile
    B->>OP: GET /authorize
    OP->>B: Login Page
    U->>B: Enter credentials
    B->>OP: POST credentials
    OP->>B: 302 Redirect
    Note over OP: Location: /secure/ecas/callback?code=...
    B->>Q: GET /secure/ecas/callback?code=...
    Q->>OP: POST /token
    Note over Q: code=...&<br/>code_verifier=...&<br/>client credentials
    OP->>Q: ID Token + Access Token
    Q->>Q: Validate ID Token
    Q->>Q: Create session
    Q->>Q: Run SecurityIdentityAugmentor
    Q->>B: 302 Redirect + Set-Cookie
    Note over Q: Location: /secure/ecas
    B->>Q: GET /secure/ecas (with cookie)
    Q->>B: 302 Redirect
    Note over Q: Location: /rest/me
    B->>Q: GET /rest/me
    Q->>B: 200 UserVO JSON
```

### Logout Flow

```mermaid
sequenceDiagram
    actor U as User
    participant B as Browser
    participant Q as Quarkus App
    participant OP as OIDC Provider
    
    U->>B: Click Logout
    B->>Q: GET /admin/logout
    Q->>B: 302 Redirect
    Note over Q: Location: /logout
    B->>Q: GET /logout
    Q->>Q: Invalidate session
    Q->>Q: Clear cookies
    Q->>B: 302 Redirect
    Note over Q: Location: {OP}/logout?<br/>id_token_hint=...&<br/>post_logout_redirect_uri=/hello
    B->>OP: GET /logout
    OP->>OP: End provider session
    OP->>B: 302 Redirect
    Note over OP: Location: /hello
    B->>Q: GET /hello
    Q->>B: 200 "Hello from Quarkus REST"
```

---

## Authorization Workflows

### Role-Based Access Flow

```mermaid
sequenceDiagram
    participant B as Browser
    participant Q as Quarkus App
    participant RES as ProtectedResource
    
    B->>Q: GET /api/protected
    Q->>Q: Check HTTP auth permission
    Note over Q: Path requires authentication
    Q->>Q: Validate session cookie
    Q->>Q: Load SecurityIdentity
    Q->>RES: Invoke adminOnly()
    Note over RES: @RolesAllowed("administrator")
    alt Has administrator role
        RES->>Q: Return response
        Q->>B: 200 JSON
    else Missing administrator role
        RES->>Q: Throw ForbiddenException
        Q->>B: 403 Forbidden
    end
```

### Permission Evaluation Order

```mermaid
graph TD
    REQ[Incoming Request] --> PERM{HTTP Auth<br/>Permission Check}
    PERM -->|permit| RES[Resource Method]
    PERM -->|authenticated| AUTH{Valid<br/>Session?}
    AUTH -->|no| OIDC[OIDC Redirect]
    AUTH -->|yes| ROLES{@RolesAllowed<br/>Check}
    ROLES -->|pass| RES
    ROLES -->|fail| FORBID[403 Forbidden]
    OIDC --> LOGIN[Login Flow]
    LOGIN --> AUTH
```

---

## Development Workflows

### DevServices Startup

```mermaid
sequenceDiagram
    participant DEV as Developer
    participant MVN as Maven
    participant Q as Quarkus
    participant DS as DevServices
    participant KC as Keycloak Container
    
    DEV->>MVN: ./mvnw quarkus:dev
    MVN->>Q: Start Quarkus
    Q->>Q: Check quarkus.oidc.auth-server-url
    Note over Q: Not set for dev profile
    Q->>DS: Request Keycloak DevServices
    DS->>KC: Start Keycloak container
    KC->>KC: Import eulogin-realm.json
    KC->>DS: Container ready on port XXXXX
    DS->>Q: Configure OIDC client
    Note over Q: Auto-set auth-server-url,<br/>client-id, credentials
    Q->>DEV: Application ready
    Note over DEV: http://localhost:8080
```

### Test Execution

```mermaid
sequenceDiagram
    participant TEST as JUnit Test
    participant Q as Quarkus Test
    participant SEC as @TestSecurity
    participant RES as REST Endpoint
    
    TEST->>Q: Start QuarkusTest
    Q->>Q: Initialize test mode
    Note over Q: DevServices auto-configured
    TEST->>SEC: @TestSecurity(user="test1", roles="administrator")
    SEC->>SEC: Create mock SecurityIdentity
    TEST->>RES: given().get("/api/protected")
    RES->>RES: Check @RolesAllowed
    Note over RES: SecurityIdentity has<br/>administrator role
    RES->>TEST: 200 OK
    TEST->>TEST: Assert response
```

---

## Request Processing Workflow

### Authenticated Request

```mermaid
graph TB
    subgraph "Request Phase"
        REQ[HTTP Request] --> FILTER[Auth Filter]
        FILTER --> COOKIE{Session<br/>Cookie?}
        COOKIE -->|yes| VALIDATE[Validate Session]
        COOKIE -->|no| REDIRECT[OIDC Redirect]
        VALIDATE --> IDENTITY[Build SecurityIdentity]
    end
    
    subgraph "Augmentation Phase"
        IDENTITY --> AUG[EuLoginIdentityAugmentor]
        AUG --> LOG[Log Authentication]
    end
    
    subgraph "Processing Phase"
        LOG --> RBAC[@RolesAllowed Check]
        RBAC --> CTX[Request Scope Active]
        CTX --> RES[REST Resource]
        RES --> ESC[EcasSessionContext]
        ESC --> UVO[Build UserVO]
    end
    
    subgraph "Response Phase"
        UVO --> JSON[JSON Serialization]
        JSON --> RESP[HTTP Response]
    end
```

### UserVO Creation (Lazy Loading)

```mermaid
stateDiagram-v2
    [*] --> RequestStart
    RequestStart --> ResourceInvoked
    ResourceInvoked --> GetCurrentUser: Call ecasSessionContext.getCurrentUser()
    GetCurrentUser --> CheckCache: Check currentUser field
    CheckCache --> ReturnCached: currentUser != null
    CheckCache --> CheckAnonymous: currentUser == null
    CheckAnonymous --> ReturnNull: identity.isAnonymous()
    CheckAnonymous --> BuildUserVO: !identity.isAnonymous()
    BuildUserVO --> ExtractClaims: Extract from SecurityIdentity
    ExtractClaims --> SetCache: Store in currentUser
    SetCache --> ReturnNew: Return UserVO
    ReturnCached --> [*]
    ReturnNull --> [*]
    ReturnNew --> [*]
```

---

## Configuration Workflow

### Profile Selection

```mermaid
graph TD
    START[Application Start] --> CHECK{Profile<br/>Specified?}
    CHECK -->|java -Dquarkus.profile=prod| PROD[Production Mode]
    CHECK -->|./mvnw quarkus:dev| DEV[Dev Mode]
    CHECK -->|./mvnw test| TEST[Test Mode]
    
    PROD --> EULOGIN[Use EU Login OIDC]
    DEV --> DEVSERVICES[Use Keycloak DevServices]
    TEST --> DEVSERVICES
    
    EULOGIN --> SECRETS[Read OIDC_CLIENT_SECRET env var]
    DEVSERVICES --> REALM[Import eulogin-realm.json]
```

### Property Resolution

```mermaid
graph LR
    subgraph "application.properties"
        SHARED[Shared Settings]
        PROD[%prod.* Settings]
        DEV[%dev.* Settings]
    end
    
    subgraph "Active in Dev"
        SHARED --> D_ACTIVE[Active]
        DEV --> D_ACTIVE
        PROD --> D_INACTIVE[Inactive]
    end
    
    subgraph "Active in Prod"
        SHARED --> P_ACTIVE[Active]
        PROD --> P_ACTIVE
        DEV --> P_INACTIVE[Inactive]
    end
```
