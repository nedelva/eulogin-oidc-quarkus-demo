package eu.europa.ec.digit.eulogin;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Public endpoint — no authentication required.
 * Mirrors eCertis public endpoints like {@code /criteria/**}, {@code /search/**}.
 */
@Path("/hello")
@PermitAll
public class GreetingResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from Quarkus REST";
    }
}
