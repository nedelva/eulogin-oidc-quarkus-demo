package eu.europa.ec.digit.eulogin.resource;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;

/**
 * Demonstrates role-based access control — the Quarkus equivalent of eCertis
 * {@code @PreAuthorize("hasAuthority('administrator')")} used on UserController,
 * FeedbackController, CoverageAreaController, and NotificationController.
 * <p>
 * In Quarkus, {@code @RolesAllowed} replaces Spring's {@code @PreAuthorize} for simple role checks.
 */
@Path("/api/protected")
@Produces(MediaType.APPLICATION_JSON)
public class ProtectedResource {

    @Inject
    SecurityIdentity identity;

    @GET
    @RolesAllowed("administrator")
    public Map<String, Object> adminOnly() {
        return Map.of(
            "message", "You have administrator access",
            "user", identity.getPrincipal().getName(),
            "roles", identity.getRoles()
        );
    }

    @GET
    @Path("/editor")
    @RolesAllowed("editor")
    public Map<String, Object> editorOnly() {
        return Map.of(
            "message", "You have editor access",
            "user", identity.getPrincipal().getName(),
            "roles", identity.getRoles()
        );
    }
}
