package eu.europa.ec.digit.eulogin.vo;

import java.util.Set;

/**
 * Simplified from eCertis {@code eu.europa.ec.grow.ecertis.vo.UserVO}.
 * <p>
 * In the real eCertis app this extends AuditableVO and carries ProfileVO, roleIds, etc.
 * Here we keep only the fields relevant to the OIDC authentication flow.
 */
public class UserVO {

    private String username;
    private String name;
    private String emailAddress;
    private Set<String> roles;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmailAddress() { return emailAddress; }
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }

    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }
}
