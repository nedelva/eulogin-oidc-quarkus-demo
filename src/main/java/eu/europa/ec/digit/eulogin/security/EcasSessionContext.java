package eu.europa.ec.digit.eulogin.security;

import eu.europa.ec.digit.eulogin.vo.UserVO;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

/**
 * Quarkus equivalent of eCertis {@code eu.europa.ec.grow.ecertis.http.EcasSessionContext}.
 * <p>
 * Builds the {@link UserVO} lazily from the current {@link SecurityIdentity}
 * rather than being populated during authentication, since the request scope
 * is not yet active when {@code SecurityIdentityAugmentor} runs.
 */
@RequestScoped
public class EcasSessionContext {

    @Inject
    SecurityIdentity identity;

    private UserVO currentUser;

    public UserVO getCurrentUser() {
        if (currentUser == null && !identity.isAnonymous()) {
            UserVO vo = new UserVO();
            vo.setUsername(identity.getPrincipal().getName());
            vo.setEmailAddress(identity.getAttribute("email"));
            String firstName = identity.getAttribute("given_name");
            String lastName = identity.getAttribute("family_name");
            if (firstName != null && lastName != null) {
                vo.setName(firstName + " " + lastName);
            }
            vo.setRoles(identity.getRoles());
            currentUser = vo;
        }
        return currentUser;
    }
}
