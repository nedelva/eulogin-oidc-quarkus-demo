# OIDC Configuration Guide

This document explains how OpenID Connect (OIDC) is configured in this project to authenticate users against EU Login (
ECAS), and how Keycloak DevServices stand in during local development.

## The Problem

EU Login requires a registered OIDC client (client ID + client secret) obtained through DIGIT DISS Back Office. Until
that registration is complete, the application cannot authenticate against the real EU Login server. A local substitute
is needed to develop and test the full OIDC flow.

## Solution: Profile-Based Configuration with Keycloak DevServices

The configuration uses Quarkus profiles to separate production (EU Login) from development (Keycloak DevServices):

- **Production (`%prod`)**: connects to the real EU Login OIDC server.
- **Dev/Test (default)**: Quarkus automatically starts a Keycloak container via DevServices, providing a fully
  functional OIDC provider locally.

The key mechanism: Quarkus DevServices for Keycloak activates when `quarkus.oidc.auth-server-url` is **not set** for the
active profile. Since the URL is prefixed with `%prod`, it is absent in dev and test modes, triggering DevServices.

## Production Configuration (EU Login)

The following properties are active only in the `prod` profile:

| Property                              | Purpose                                                                    |
|---------------------------------------|----------------------------------------------------------------------------|
| `quarkus.oidc.auth-server-url`        | EU Login OIDC discovery endpoint                                           |
| `quarkus.oidc.client-id`              | Registered client name (`eulogin-oicd-quarkus-demo`)                       |
| `quarkus.oidc.credentials.jwt.secret` | Client secret for `client_secret_jwt` authentication (from env var)        |
| `quarkus.oidc.authentication.scopes`  | `openid,email,profile,hr` -- the `hr` scope provides all HR profile claims |

### EU Login Specifics

These characteristics of EU Login drove several configuration choices:

- **PKCE required** (`quarkus.oidc.authentication.pkce-required=true`): EU Login mandates RFC 7636 Proof Key for Code
  Exchange on all clients. Quarkus handles the `code_challenge`/`code_verifier` exchange automatically when this is
  enabled.

- **`client_secret_jwt` authentication** (`quarkus.oidc.credentials.jwt.secret`): EU Login does not accept the client
  secret in plain text. Instead, the client signs a JWT with the shared secret and sends it to the token endpoint. Using
  `credentials.jwt.secret` (rather than `credentials.secret`) tells Quarkus to use this method.

- **UserInfo endpoint disabled**: EU Login disables the UserInfo endpoint by default. All user claims are delivered in
  the ID token. Since Quarkus `web-app` type extracts identity from the ID token, this works without additional
  configuration.

- **Opaque access tokens**: EU Login issues reference tokens (`expires_in: 0`) that cannot be decoded as JWTs. The
  application relies on the ID token and session cookie for identity, so this has no impact on the current flow.

- **ID tokens signed with PS512**: EU Login signs ID tokens with the PS512 algorithm. Quarkus auto-discovers this from
  the OIDC provider metadata.

## Dev/Test Configuration (Keycloak DevServices)

A single property enables the local Keycloak substitute:

```properties
quarkus.keycloak.devservices.realm-path=eulogin-realm.json
```

When `./mvnw quarkus:dev` runs (with Docker available), Quarkus:

1. Starts a Keycloak container via Testcontainers.
2. Imports the realm defined in `eulogin-realm.json`.
3. Autoconfigures `quarkus.oidc.auth-server-url`, `client-id`, and `credentials.secret` to point at the container.

### Realm Configuration (`eulogin-realm.json`)

The realm mirrors the EU Login environment:

- **Client**: `quarkus-app` with PKCE enforced (`S256`) and protocol mappers for `email`, `given_name`, `family_name` in
  the ID token -- matching the claims EU Login returns with the `hr` scope.
- **Roles**: `administrator` and `editor`, matching the roles used in `@RolesAllowed` annotations.
- **Users**:
    - `test1` / `test` -- has the `administrator` role
    - `editor1` / `test` -- has the `editor` role

### Dev UI

In dev mode, navigate to http://localhost:8080/q/dev-ui/ to access the OIDC Dev UI, which lets you acquire tokens from
the local Keycloak and test protected endpoints interactively.

## Shared OIDC Settings

These apply to all profiles:

| Property                                             | Purpose                                                        |
|------------------------------------------------------|----------------------------------------------------------------|
| `application-type=web-app`                           | Authorization Code flow with server-side session               |
| `authentication.pkce-required=true`                  | PKCE for both EU Login (required) and Keycloak (good practice) |
| `authentication.redirect-path=/secure/ecas/callback` | Where the OIDC provider redirects after login                  |
| `authentication.restore-path-after-redirect=true`    | Returns the user to the originally requested page after login  |
| `token.principal-claim=sub`                          | Uses the `sub` claim as the principal name                     |
| `authentication.cookie-same-site=strict`             | CSRF protection for the session cookie                         |
| `logout.path=/logout`                                | RP-initiated logout endpoint                                   |
| `logout.post-logout-path=/hello`                     | Where to redirect after logout completes                       |

## Preparing for Production

When the EU Login client registration is approved, you will receive a client ID and client secret. To deploy:

1. Set the `OIDC_CLIENT_SECRET` environment variable with the received secret.
2. Run with the `prod` profile: `java -Dquarkus.profile=prod -jar target/quarkus-app/quarkus-run.jar`

### Client Metadata Considerations

Before submitting the registration, review your client metadata:

- **Scopes**: ensure `"scope": "openid hr"` (or `"openid email profile hr"`) so the ID token contains the claims your
  application reads (`email`, `given_name`, `family_name`).
- **Groups/roles**: EU Login does not return groups by default. If your application needs role-based access from EU
  Login groups, add them explicitly to the `claims` field:
  ```json
  "claims": {
    "id_token": {
      "https://ecas.ec.europa.eu/claims/groups": {
        "values": ["YOUR_GROUP_1", "YOUR_GROUP_2"]
      }
    }
  }
  ```
  Then map them in Quarkus with `quarkus.oidc.roles.role-claim-path`.
- **Redirect URIs**: update `redirect_uris` to include your production URL(s) alongside
  `http://localhost:8080/secure/ecas/callback`.
- **Back-channel logout**: if you need Single Logout, add `backchannel_logout_uri` and
  `backchannel_logout_session_required` to your metadata.
