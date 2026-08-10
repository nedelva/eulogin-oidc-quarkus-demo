# Interfaces

## REST API Overview

```mermaid
graph LR
    subgraph "Public"
        P1[GET /hello]
        P2[GET /rest/user]
        P3[GET /rest/me]
    end
    
    subgraph "Authenticated"
        A1[GET /secure/ecas]
        A2[GET /admin/logout]
        A3[GET /api/protected]
        A4[GET /api/protected/editor]
    end
    
    subgraph "Dev Only"
        D1[/q/*]
    end
```

---

## Public Endpoints

### GET /hello

**Resource:** `GreetingResource`  
**Authentication:** None required  
**Purpose:** Health check and post-logout landing page

**Response:**
```
Content-Type: text/plain

Hello from Quarkus REST
```

---

### GET /rest/user

**Resource:** `UserResource.getUserDetails()`  
**Authentication:** None required (returns null if anonymous)  
**Purpose:** Get current user from SecurityIdentity

**Response (authenticated):**
```json
{
  "username": "test1",
  "name": null,
  "emailAddress": "test1@ec.europa.eu",
  "roles": ["administrator"]
}
```

**Response (anonymous):**
```json
null
```

---

### GET /rest/me

**Resource:** `UserResource.getCurrentUser()`  
**Authentication:** None required (returns null if anonymous)  
**Purpose:** Get current user from EcasSessionContext (includes name)

**Response (authenticated):**
```json
{
  "username": "test1",
  "name": "Test Administrator",
  "emailAddress": "test1@ec.europa.eu",
  "roles": ["administrator"]
}
```

**Response (anonymous):**
```json
null
```

---

## Authenticated Endpoints

### GET /secure/ecas

**Resource:** `UserResource.postLogin()`  
**Authentication:** Required (triggers OIDC flow if not authenticated)  
**Purpose:** OIDC callback handler, post-login redirect

**Response:**
```
HTTP/1.1 303 See Other
Location: /rest/me
```

**Flow:**
1. Browser accesses `/secure/ecas`
2. If not authenticated → OIDC authorization redirect
3. After successful authentication → 303 redirect to `/rest/me`

---

### GET /admin/logout

**Resource:** `UserResource.logout()`  
**Authentication:** Required  
**Purpose:** Initiate RP-initiated logout

**Response:**
```
HTTP/1.1 303 See Other
Location: /logout
```

**Flow:**
1. Redirects to Quarkus OIDC logout endpoint (`/logout`)
2. Quarkus invalidates session, redirects to OIDC provider logout
3. After provider logout → redirects to `/hello`

---

### GET /api/protected

**Resource:** `ProtectedResource.adminOnly()`  
**Authentication:** Required + `administrator` role  
**Purpose:** Demonstrate role-based access control

**Response (with administrator role):**
```json
{
  "message": "You have administrator access",
  "user": "test1",
  "roles": ["administrator"]
}
```

**Response (without administrator role):**
```
HTTP/1.1 403 Forbidden
```

---

### GET /api/protected/editor

**Resource:** `ProtectedResource.editorOnly()`  
**Authentication:** Required + `editor` role  
**Purpose:** Demonstrate editor-specific access

**Response (with editor role):**
```json
{
  "message": "You have editor access",
  "user": "editor1",
  "roles": ["editor"]
}
```

**Response (without editor role):**
```
HTTP/1.1 403 Forbidden
```

---

## OIDC Endpoints (Framework-Managed)

These endpoints are managed by `quarkus-oidc` and configured in `application.properties`:

| Endpoint | Purpose | Configuration |
|----------|---------|---------------|
| `/secure/ecas/callback` | OIDC authorization code callback | `quarkus.oidc.authentication.redirect-path` |
| `/logout` | RP-initiated logout | `quarkus.oidc.logout.path` |

---

## Dev UI Endpoints

Available only in dev mode (`./mvnw quarkus:dev`):

| Endpoint | Purpose |
|----------|---------|
| `/q/dev-ui/` | Quarkus Dev UI dashboard |
| `/q/dev-ui/io.quarkus.quarkus-oidc/` | OIDC Dev UI - test tokens, inspect configuration |
| `/q/health` | Health check (if extension added) |
| `/q/openapi` | OpenAPI spec (if extension added) |

---

## HTTP Auth Permissions

Configured in `application.properties`:

```properties
# Public - no authentication
quarkus.http.auth.permission.public.paths=/hello,/rest/user,/rest/me,/q/*
quarkus.http.auth.permission.public.policy=permit

# Secured - requires authentication
quarkus.http.auth.permission.secured.paths=/secure/*
quarkus.http.auth.permission.secured.policy=authenticated

# Admin - requires authentication
quarkus.http.auth.permission.admin.paths=/admin/*
quarkus.http.auth.permission.admin.policy=authenticated

# Protected API - requires authentication (roles checked by @RolesAllowed)
quarkus.http.auth.permission.protected.paths=/api/protected
quarkus.http.auth.permission.protected.policy=authenticated
```

---

## Error Responses

### 401 Unauthorized
Returned when accessing authenticated endpoints without a valid session.
In browser: triggers OIDC redirect.
In API call: returns 401 status.

### 403 Forbidden
Returned when authenticated user lacks required role.

```json
{
  "error": "Forbidden"
}
```

---

## CORS Configuration

For local Angular development:

```properties
%dev.quarkus.http.cors.origins=http://localhost:4200
%dev.quarkus.http.cors.access-control-allow-credentials=true
```

Allows Angular apps on `localhost:4200` to call the API with credentials (session cookies).
