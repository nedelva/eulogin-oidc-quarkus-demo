# Documentation Index

> **For AI Assistants:** This file serves as your primary reference for navigating this codebase. Read the summaries below to identify which files contain detailed information for your task. Most questions can be answered by reading this index plus one or two specific files.

## Quick Reference

| Task | Primary File | Secondary Files |
|------|-------------|-----------------|
| Understand project purpose | [codebase_info.md](codebase_info.md) | [architecture.md](architecture.md) |
| Add new REST endpoint | [components.md](components.md) | [interfaces.md](interfaces.md) |
| Modify authentication flow | [workflows.md](workflows.md) | [architecture.md](architecture.md) |
| Add role-based access | [components.md](components.md#protectedresource) | [interfaces.md](interfaces.md) |
| Configure OIDC settings | [architecture.md](architecture.md) | `docs/oidc-configuration.md` |
| Add new dependency | [dependencies.md](dependencies.md) | `pom.xml` |
| Understand data structures | [data_models.md](data_models.md) | - |

## Documentation Files

### [codebase_info.md](codebase_info.md)
**Purpose:** Project overview and technology stack  
**Contains:** Project coordinates, Java/Quarkus versions, directory structure, package organization, configuration files summary, authentication profiles, test users  
**Use when:** Starting work on this project, understanding the tech stack, locating source files

### [architecture.md](architecture.md)
**Purpose:** System architecture and design patterns  
**Contains:** High-level architecture diagram, OIDC authentication flow, profile-based configuration strategy, request processing pipeline, security layers  
**Use when:** Understanding how authentication works, modifying security configuration, adding new protected endpoints

### [components.md](components.md)
**Purpose:** Detailed documentation of Java classes  
**Contains:** Each class with its purpose, methods, dependencies, and relationship to eCertis equivalents  
**Use when:** Modifying existing code, adding new features to existing classes, understanding class responsibilities

### [interfaces.md](interfaces.md)
**Purpose:** REST API endpoints and contracts  
**Contains:** All HTTP endpoints, their paths, methods, authentication requirements, request/response formats  
**Use when:** Integrating with the API, adding new endpoints, understanding endpoint security requirements

### [data_models.md](data_models.md)
**Purpose:** Data structures and value objects  
**Contains:** UserVO structure, OIDC token claims mapping, Keycloak realm configuration schema  
**Use when:** Working with user data, modifying claims extraction, updating the test realm

### [workflows.md](workflows.md)
**Purpose:** Key processes and user flows  
**Contains:** Login flow, logout flow, role-based authorization flow, DevServices startup sequence  
**Use when:** Debugging authentication issues, understanding the complete request lifecycle, modifying auth behavior

### [dependencies.md](dependencies.md)
**Purpose:** External dependencies and their usage  
**Contains:** Maven dependencies, their purposes, version management strategy, test dependencies  
**Use when:** Adding new dependencies, understanding what libraries are available, troubleshooting classpath issues

## Key Patterns in This Codebase

1. **Profile-Based Configuration:** Production uses EU Login OIDC; dev/test uses Keycloak DevServices. The absence of `quarkus.oidc.auth-server-url` in non-prod profiles triggers DevServices.

2. **eCertis Equivalents:** Classes contain Javadoc references to their eCertis counterparts, helping developers familiar with eCertis understand the Quarkus implementation.

3. **Request-Scoped User Context:** `EcasSessionContext` provides lazy-loaded user information, similar to eCertis patterns but adapted for Quarkus CDI.

4. **Declarative Security:** Uses `@RolesAllowed` annotations rather than programmatic security checks.

## File Locations

```
Source Code:     src/main/java/eu/europa/ec/digit/eulogin/
Configuration:   src/main/resources/application.properties
Test Realm:      src/main/resources/eulogin-realm.json
Tests:           src/test/java/eu/europa/ec/digit/eulogin/
Documentation:   docs/oidc-configuration.md
```
