package eu.europa.ec.digit.eulogin.resource;

import eu.europa.ec.digit.eulogin.security.EcasSessionContext;
import eu.europa.ec.digit.eulogin.vo.UserVO;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;

/**
 * Quarkus equivalent of eCertis {@code StartUpController}.
 * <p>
 * Maps the same endpoints:
 * <ul>
 *   <li>{@code /secure/ecas} — post-login redirect (was container-managed ECAS redirect)</li>
 *   <li>{@code /rest/user} — returns user from SecurityIdentity (was from Principal + UserService DB lookup)</li>
 *   <li>{@code /rest/me} — returns user from request-scoped context (was from EcasSessionContext)</li>
 *   <li>{@code /admin/logout} — OIDC RP-initiated logout (was ECAS logout + session invalidation)</li>
 * </ul>
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    SecurityIdentity identity;

    @Inject
    EcasSessionContext ecasSessionContext;

    /**
     * Post-login redirect. In eCertis this is {@code /secure/ecas} which redirects
     * to the Angular app after ECAS authentication. Here we just confirm login.
     */
    @GET
    @Path("/secure/ecas")
    public Response postLogin() {
        // In real eCertis: redirect to applicationUrl (Angular editor home page)
        return Response.seeOther(URI.create("/rest/me")).build();
    }

    /**
     * Returns user details built from the OIDC SecurityIdentity.
     * In eCertis this calls {@code userService.findUser(principal.getName())} to load from DB.
     */
    @GET
    @Path("/rest/user")
    public UserVO getUserDetails() {
        if (identity.isAnonymous()) {
            return null;
        }
        UserVO vo = new UserVO();
        vo.setUsername(identity.getPrincipal().getName());
        vo.setEmailAddress(identity.getAttribute("email"));
        vo.setRoles(identity.getRoles());
        return vo;
    }

    /**
     * Returns the current user from the request-scoped context.
     * Mirrors eCertis {@code ecasSessionContext.getCurrentUser()}.
     * Populated by {@link eu.europa.ec.digit.eulogin.security.EuLoginIdentityAugmentor}.
     */
    @GET
    @Path("/rest/me")
    public UserVO getCurrentUser() {
        return ecasSessionContext.getCurrentUser();
    }

    /**
     * OIDC RP-initiated logout. Redirects to the Quarkus OIDC logout endpoint
     * which handles session invalidation and redirect to the EU Login logout URL.
     * <p>
     * In eCertis this manually invalidates the HTTP session, clears
     * SecurityContextHolder, and redirects to the ECAS logout URL.
     * With quarkus-oidc, configure {@code quarkus.oidc.logout.path} and
     * {@code quarkus.oidc.logout.post-logout-path} in application.properties.
     */
    @GET
    @Path("/admin/logout")
    public Response logout() {
        // Redirect to the OIDC logout endpoint configured in application.properties
        return Response.seeOther(URI.create("/logout")).build();
    }
}
