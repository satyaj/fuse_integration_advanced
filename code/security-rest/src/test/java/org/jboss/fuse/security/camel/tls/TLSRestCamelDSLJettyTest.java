package org.jboss.fuse.security.camel.tls;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestPropertyDefinition;
import org.apache.camel.util.jsse.KeyManagersParameters;
import org.apache.camel.util.jsse.KeyStoreParameters;
import org.apache.camel.util.jsse.SSLContextParameters;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.eclipse.jetty.jaas.JAASLoginService;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.util.security.Constraint;
import org.jboss.fuse.security.camel.common.BaseJettyTest;
import org.jboss.fuse.security.camel.role.BasicAuthRESTCamelDSLJettyJaasRoleConstraintTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TLSRestCamelDSLJettyTest extends BaseJettyTest {

    private static String HOST = "localhost";
    private static int PORT = getPort1();
    protected String pwd = "secUr1t8";

    @Override
    protected JndiRegistry createRegistry() throws Exception {
        JndiRegistry jndi = super.createRegistry();
        jndi.bind("myAuthHandler", getSecurityHandler());
        jndi.bind("scp", getSSLContextParameters());
        return jndi;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        URL jaasURL = this.getClass().getResource("/org/jboss/fuse/security/basic/myrealm-jaas.cfg");
        setSystemProp("java.security.auth.login.config", jaasURL.toExternalForm());

        URL trustStoreUrl = this.getClass().getResource("serverstore.jks");
        setSystemProp("javax.net.ssl.trustStore", trustStoreUrl.toURI().getPath());
        setSystemProp("javax.net.ssl.trustStorePassword", pwd);
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        restoreSystemProperties();
    }

    // EXCLUDE-BEGIN
    @Test public void shouldSayHelloTest() {
        String user = "Charles";
        String strURL = "https://" + HOST + ":" + PORT + "/say/hello/" + user;

        BaseJettyTest.HttpResult result = callRestEndpoint("localhost", strURL, "donald", "duck", "MyRealm");
        assertEquals(200, result.getCode());
        assertEquals("We should get a Hello World", "Hello World " + user,
                result.getMessage().replaceAll("^\"|\"$", ""));
    }
    // EXCLUDE-END

    // EXCLUDE-BEGIN
    @Test @Ignore public void sayByeNotAllowedForUserRoleTest() {
        String user = "Charles";
        String strURL = "https://" + HOST + ":" + PORT + "/say/bye/" + user;

        HttpResult result = callRestEndpoint("localhost", strURL, "donald", "duck", "MyRealm");
        assertEquals(403, result.getCode());
    }
    // EXCLUDE-END

    // EXCLUDE-BEGIN
    @Test @Ignore public void sayByeAllowedForAdminRoleTest() {
        String user = "Mickey";
        String strURL = "https://" + HOST + ":" + PORT + "/say/bye/" + user;

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

        // Configure the Jetty Properties of the endpoint
        final List<RestPropertyDefinition> jettyEndpointProps = new ArrayList<>();
        RestPropertyDefinition rpd = new RestPropertyDefinition();

        // Add key fof the Security Constrainr
        rpd.setKey("handlers");
        rpd.setValue("myAuthHandler");
        jettyEndpointProps.add(rpd);

        // Add key of the SSL Context Parameters
        rpd = new RestPropertyDefinition();
        rpd.setKey("sslContextParametersRef");
        rpd.setValue("scp");
        jettyEndpointProps.add(rpd);

        // Add keys for the Jetty Component
        //List<RestPropertyDefinition> jettyComponentProps = new ArrayList<>();
        //rpd = new RestPropertyDefinition();
        //jettyComponentProps.add(rpd);


        return new RouteBuilder() {
            @Override public void configure() throws Exception {

                restConfiguration().component("jetty").scheme("https").host("0.0.0.0").port(getPort1())
                        .bindingMode(RestBindingMode.json).setEndpointProperties(jettyEndpointProps);

                rest("/say").produces("json").get("/hello/{id}").to("direct:hello");
                rest("/say").produces("json").get("/bye/{id}").to("direct:bye");

                from("direct:hello").transform().simple("Hello World ${header.id}");
                from("direct:bye").transform().simple("Bye ${header.id}");

            }
        };
    }
    // EXCLUDE-END

    private SSLContextParameters getSSLContextParameters() {
        // TLS
        KeyStoreParameters ksp = new KeyStoreParameters();
        ksp.setResource("org/jboss/fuse/security/camel/tls/serverstore.jks");
        ksp.setPassword(pwd);

        KeyManagersParameters kmp = new KeyManagersParameters();
        kmp.setKeyStore(ksp);
        kmp.setKeyPassword(pwd);

        SSLContextParameters scp = new SSLContextParameters();
        scp.setKeyManagers(kmp);
        return scp;
    }

    private SecurityHandler getSecurityHandler() throws IOException {

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
        sh.setConstraintMappings(getConstraintMappings());

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
        sh.setConstraintMappings(getConstraintMappings());

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