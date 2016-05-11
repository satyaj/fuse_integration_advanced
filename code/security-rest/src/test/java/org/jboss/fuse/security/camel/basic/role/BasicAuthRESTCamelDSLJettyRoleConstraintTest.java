package org.jboss.fuse.security.camel.basic.role;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestPropertyDefinition;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.eclipse.jetty.security.*;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.util.security.Constraint;
import org.jboss.fuse.security.camel.common.BaseJettyTest;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BasicAuthRESTCamelDSLJettyRoleConstraintTest extends BaseJettyTest {

    private static String HOST = "localhost";
    private static int PORT = getPort1();

    @Override
    protected JndiRegistry createRegistry() throws Exception {
        JndiRegistry jndi = super.createRegistry();
        jndi.bind("myAuthHandler", getSecurityHandler());
        return jndi;
    }

    @Before
    public void init() throws IOException {
        URL jaasURL = BasicAuthRESTCamelDSLJettyRoleConstraintTest.class.getResource("/org/jboss/fuse/security/basic/myrealm-jaas.cfg");
        System.setProperty("java.security.auth.login.config", jaasURL.toExternalForm());
    }

    // EXCLUDE-BEGIN
    @Test
    public void shouldSayHelloTest() {
        String user = "Charles";
        String strURL = "http://" + HOST + ":" + PORT + "/say/hello/" + user;

        BaseJettyTest.HttpResult result = callRestEndpoint("localhost", strURL, "donald", "duck", "MyRealm");
        assertEquals(200, result.getCode());
        assertEquals("We should get a Hello World", "Hello World " + user,
                result.getMessage().replaceAll("^\"|\"$", ""));
    }
    // EXCLUDE-END

    // EXCLUDE-BEGIN
    @Test
    public void sayByeNotAllowedForUserRoleTest() {
        String user = "Charles";
        String strURL = "http://" + HOST + ":" + PORT + "/say/bye/" + user;

        HttpResult result = callRestEndpoint("localhost", strURL, "donald", "duck", "MyRealm");
        assertEquals(403, result.getCode());
    }
    // EXCLUDE-END

    // EXCLUDE-BEGIN
    @Test
    public void sayByeAllowedForAdminRoleTest() {
        String user = "Mickey";
        String strURL = "http://" + HOST + ":" + PORT + "/say/bye/" + user;

        HttpResult result = callRestEndpoint("localhost", strURL, "mickey", "mouse", "MyRealm");
        assertEquals(200, result.getCode());
        assertEquals("We should get a Bye", "Bye " + user,
                result.getMessage().replaceAll("^\"|\"$", ""));

    }
    // EXCLUDE-END

    private HttpResult callRestEndpoint(String host, String url, String user, String password, String realm) {

        HttpResult response = new HttpResult();

        // Define the Get Method with the String of the url to access the HTTP Resource
        GetMethod get = new GetMethod(url);

        // Set Credentials
        Credentials creds = new UsernamePasswordCredentials(user, password);
        // Auth Scope
        AuthScope authScope = new AuthScope(host, PORT, realm);

        // Execute request
        try {
            // Get HTTP client
            HttpClient httpclient = new HttpClient();
            // Use preemptive to select BASIC Auth
            httpclient.getParams().setAuthenticationPreemptive(true);
            httpclient.getState().setCredentials(authScope, creds);
            response.setCode(httpclient.executeMethod(get));

            InputStream is = get.getResponseBodyAsStream();
            response.setMessage(inputStreamToString(is));

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Release current connection to the connection pool once you are done
            get.releaseConnection();
        }

        return response;
    }

    // EXCLUDE-BEGIN
    @Override protected RouteBuilder createRouteBuilder() throws Exception {

        final List<RestPropertyDefinition> jettyProperties = new ArrayList<>();
        RestPropertyDefinition rpd = new RestPropertyDefinition();
        rpd.setKey("handlers");
        rpd.setValue("myAuthHandler");
        jettyProperties.add(rpd);

        return new RouteBuilder() {
            @Override public void configure() throws Exception {

                restConfiguration().component("jetty").scheme("http").host("0.0.0.0").port(getPort1())
                        .bindingMode(RestBindingMode.json).setEndpointProperties(jettyProperties);

                rest("/say").produces("json").get("/hello/{id}").to("direct:hello");
                rest("/say").produces("json").get("/bye/{id}").to("direct:bye");

                from("direct:hello").transform().simple("Hello World ${header.id}");
                from("direct:bye").transform().simple("Bye ${header.id}");

            }
        };
    }
    // EXCLUDE-END

    private SecurityHandler getSecurityHandler() throws IOException {

        // Describe the Authentication Constraint to be applied (BASIC, DISGEST, NEGOTIATE, ...)
        Constraint constraint = new Constraint(Constraint.__BASIC_AUTH, "user");
        constraint.setAuthenticate(true);

        // Map the Auth Contrainst with a Path
        ConstraintMapping cm = new ConstraintMapping();
        cm.setPathSpec("/*");
        cm.setConstraint(constraint);

        /* A security handler is a jetty handler that secures content behind a
           particular portion of a url space. The ConstraintSecurityHandler is a
           more specialized handler that allows matching of urls to different
           constraints. The server sets this as the first handler in the chain,
           effectively applying these constraints to all subsequent handlers in
           the chain.
           The BasicAuthenticator instance is the object that actually checks the credentials
        */
        ConstraintSecurityHandler sh = new ConstraintSecurityHandler();
        sh.setAuthenticator(new BasicAuthenticator());
        sh.setConstraintMappings(getConstraintMappings());

        //  The HashLogin is an implementation of a UserRealm that stores users and roles in-memory in HashMaps.
        HashLoginService loginService = new HashLoginService("MyRealm",
                "src/test/resources/org/jboss/fuse/security/basic/myrealm.props");
        sh.setLoginService(loginService);

        return sh;

    }

    private List<ConstraintMapping> getConstraintMappings() {

        // Access allowed for roles User, Admin
        Constraint constraint0 = new Constraint(Constraint.__BASIC_AUTH, "user");
        constraint0.setAuthenticate(true);
        constraint0.setName("allowedForAll");
        constraint0.setRoles(new String[] { "user", "admin" });
        ConstraintMapping mapping0 = new ConstraintMapping();
        mapping0.setPathSpec("/say/hello/*");
        mapping0.setMethod("GET");
        mapping0.setConstraint(constraint0);

        // Access alowed only for Admin role
        Constraint constraint1 = new Constraint();
        constraint1.setAuthenticate(true);
        constraint1.setName("allowedForRoleAdmin");
        constraint1.setRoles(new String[]{ "admin" });
        ConstraintMapping mapping1 = new ConstraintMapping();
        mapping1.setPathSpec("/say/bye/*");
        mapping1.setMethod("GET");
        mapping1.setConstraint(constraint1);

        return Arrays.asList(mapping0, mapping1);
    }

}
