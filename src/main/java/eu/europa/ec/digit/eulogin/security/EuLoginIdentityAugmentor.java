package eu.europa.ec.digit.eulogin.security;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

/**
 * Quarkus equivalent of eCertis {@code EcasAuthenticationSuccessListener}.
 * <p>
 * Logs successful OIDC authentications. User state is built lazily by
 * {@link EcasSessionContext} during request processing, since the request
 * scope is not active when this augmentor runs.
 */
@ApplicationScoped
public class EuLoginIdentityAugmentor implements SecurityIdentityAugmentor {

    private static final Logger LOG = Logger.getLogger(EuLoginIdentityAugmentor.class);

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity,
                                         AuthenticationRequestContext context) {
        if (!identity.isAnonymous()) {
            LOG.infof("OIDC user authenticated: %s (%s)",
                    identity.getPrincipal().getName(),
                    identity.<String>getAttribute("email"));
        }
        return Uni.createFrom().item(identity);
    }
}
