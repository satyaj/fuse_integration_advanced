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
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.util.security.Constraint;
import org.jboss.fuse.security.camel.common.BaseJettyTest;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class BasicAuthRESTCamelDSLJettyHashLoginTest extends BaseJettyTest {

    private static String HOST = "localhost";
    private static int PORT = getPort1();

    @Override
    protected JndiRegistry createRegistry() throws Exception {
        JndiRegistry jndi = super.createRegistry();
    // EXCLUDE-BEGIN
        jndi.bind("myAuthHandler", getSecurityHandler());
        return jndi;
    // EXCLUDE-END
    }

    private SecurityHandler getSecurityHandler() throws IOException {
   // EXCLUDE-BEGIN
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
        sh.setConstraintMappings(Arrays.asList(new ConstraintMapping[] { cm }));

        //  The HashLogin is an implementation of a UserRealm that stores users and roles in-memory in HashMaps.
        HashLoginService loginService = new HashLoginService("MyRealm",
                "src/test/resources/org/jboss/fuse/security/basic/myrealm.props");
        sh.setLoginService(loginService);
        sh.setConstraintMappings(Arrays.asList(new ConstraintMapping[] { cm }));

        return sh;
    // EXCLUDE-END
    }

    @Test
    public void UsernameTest() {
    // EXCLUDE-BEGIN
        String user = "Charles";
        String strURL = "http://" + HOST + ":" + PORT + "/say/hello/" + user;

        HttpResult result = callRestEndpoint("localhost", strURL, "donald", "duck", "MyRealm");
        assertEquals(200, result.getCode());
        assertEquals("We should get a Hello World", "Hello World " + user,
                result.getMessage().replaceAll("^\"|\"$", ""));
     // EXCLUDE-END
     }

    @Test
    public void UsernameWrongPasswordTest() {
    // EXCLUDE-BEGIN
        String user = "Charles";
        String strURL = "http://" + HOST + ":" + PORT + "/say/hello/" + user;

        HttpResult result = callRestEndpoint("localhost", strURL, "donald", "mouse", "MyRealm");
        assertEquals(401, result.getCode());
    // EXCLUDE-END
    }

    // EXCLUDE-BEGIN
    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {

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


    protected HttpResult callRestEndpoint(String host, String url, String user, String password, String realm) {

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
            Scanner s = new Scanner(is).useDelimiter("\\A");
            response.setMessage(s.hasNext() ? s.next() : "");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Release current connection to the connection pool once you are done
            get.releaseConnection();
        }

        return response;
    }

}
