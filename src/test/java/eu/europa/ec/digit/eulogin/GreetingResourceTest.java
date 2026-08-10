package eu.europa.ec.digit.eulogin;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasKey;

@QuarkusTest
class GreetingResourceTest {

    @Test
    void testPublicEndpoint() {
        given()
            .when().get("/hello")
            .then()
            .statusCode(200)
            .body(is("Hello from Quarkus REST"));
    }

    @Test
    @TestSecurity(user = "test1", roles = "administrator")
    void testRestUser_authenticated() {
        given()
            .when().get("/rest/user")
            .then()
            .statusCode(200)
            .body("username", is("test1"))
            .body("$", hasKey("roles"));
    }

    @Test
    @TestSecurity(user = "test1", roles = "administrator")
    void testProtected_withAdminRole() {
        given()
            .when().get("/api/protected")
            .then()
            .statusCode(200)
            .body("message", is("You have administrator access"));
    }

    @Test
    @TestSecurity(user = "editor1", roles = "editor")
    void testProtected_withEditorRole_forbidden() {
        given()
            .when().get("/api/protected")
            .then()
            .statusCode(403);
    }
}
