package org.jboss.fuse.security.camel.basic;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestPropertyDefinition;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.eclipse.jetty.jaas.JAASLoginService;
import org.eclipse.jetty.security.*;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.util.security.Constraint;
import org.jboss.fuse.security.camel.common.BaseJettyTest;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BasicAuthRESTCamelDSLJettyJaasTest extends BaseJettyTest {

    private static String HOST = "localhost";
    private static int PORT = getPort1();

    @Override protected JndiRegistry createRegistry() throws Exception {
        JndiRegistry jndi = super.createRegistry();
        jndi.bind("myAuthHandler", getSecurityHandler());
        return jndi;
    }

    @Before public void init() throws IOException {
        URL jaasURL = BasicAuthRESTCamelDSLJettyJaasTest.class.getResource("/org/jboss/fuse/security/basic/myrealm-jaas.cfg");
        System.setProperty("java.security.auth.login.config", jaasURL.toExternalForm());
    }

    // EXCLUDE-BEGIN
    @Test public void UsernameTest() {
        String user = "Charles";
        String strURL = "http://" + HOST + ":" + PORT + "/say/hello/" + user;

        HttpResult result = runAndValidate("localhost", strURL, "donald", "duck", "MyRealm");
        assertEquals(200, result.getCode());
        assertEquals("We should get a Hello World", "Hello World " + user,
                result.getMessage().replaceAll("^\"|\"$", ""));
    }
    // EXCLUDE-END

    // EXCLUDE-BEGIN
    @Test public void UsernameWrongPasswordTest() {
        String user = "Charles";
        String strURL = "http://" + HOST + ":" + PORT + "/say/hello/" + user;

        HttpResult result = runAndValidate("localhost", strURL, "donald", "mouse", "MyRealm");
        assertEquals(401, result.getCode());
    }
    // EXCLUDE-END

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

                from("direct:hello").transform().simple("Hello World ${header.id}");

            }
        };
    }
    // EXCLUDE-END

    private HttpResult runAndValidate(String host, String url, String user, String password, String realm) {

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

    private SecurityHandler getSecurityHandler() throws IOException {
        // Describe the Authentication Constraint to be applied (BASIC, DISGEST, NEGOTIATE, ...)
        Constraint constraint = new Constraint(Constraint.__BASIC_AUTH, "user");
        constraint.setAuthenticate(true);

        // Define Constraint mapping
        ConstraintMapping cm = new ConstraintMapping();
        cm.setPathSpec("/*");
        cm.setConstraint(constraint);

        /* A security handler is a jetty handler that secures content behind a
         *  particular portion of a url space. The ConstraintSecurityHandler is a
         *  more specialized handler that allows matching of urls to different
         *  constraints. The server sets this as the first handler in the chain,
         *  effectively applying these constraints to all subsequent handlers in
         *  the chain.
         *  The BasicAuthenticator instance is the object that actually checks the credentials
         */
        ConstraintSecurityHandler sh = new ConstraintSecurityHandler();
        sh.setAuthenticator(new BasicAuthenticator());
        sh.setConstraintMappings(Arrays.asList(new ConstraintMapping[] { cm }));

        /*
         * The DefaultIdentityService service handles only role reference maps passed in an
         * associated org.eclipse.jetty.server.UserIdentity.Scope.  If there are roles
         * refs present, then associate will wrap the UserIdentity with one that uses the role references in the
         * org.eclipse.jetty.server.UserIdentity#isUserInRole(String, org.eclipse.jetty.server.UserIdentity.Scope)}
         * implementation.
         *
        */
        DefaultIdentityService dis = new DefaultIdentityService();

        // Service which create a UserRealm suitable for use with JAAS
        JAASLoginService loginService = new JAASLoginService();
        loginService.setName("myrealm");
        loginService.setLoginModuleName("propsFileModule");
        loginService.setIdentityService(dis);

        sh.setLoginService(loginService);
        sh.setConstraintMappings(Arrays.asList(new ConstraintMapping[] { cm }));

        return sh;
    }


}