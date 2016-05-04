package org.jboss.fuse.security.camel.tls;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestConfigurationDefinition;
import org.apache.camel.model.rest.RestPropertyDefinition;
import org.apache.camel.spi.RestConfiguration;
import org.apache.camel.test.junit4.CamelTestSupport;
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
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

public class TLSRestCamelDSLJettyTest extends BaseJettyTest {

    private static final String NULL_VALUE_MARKER = TLSRestCamelDSLJettyTest.class.getCanonicalName();

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

    @Override
    public void setUp() throws Exception {
        URL jaasURL = this.getClass().getResource("/org/jboss/fuse/security/basic/myrealm-jaas.cfg");
        setSystemProp("java.security.auth.login.config", jaasURL.toExternalForm());

        URL trustStoreUrl = this.getClass().getResource("serverstore.jks");
        setSystemProp("javax.net.ssl.trustStore", trustStoreUrl.toURI().getPath());

        /*
        setSystemProp("javax.net.ssl.trustStorePassword", pwd);
        setSystemProp("org.eclipse.jetty.ssl.keystore",getKeyStore().toURI().getPath());
        setSystemProp("org.eclipse.jetty.ssl.keypassword",pwd);
        setSystemProp("org.eclipse.jetty.ssl.password",pwd);
        */

        // setSystemProp("javax.net.debug","ssl,handshake,data");

        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        restoreSystemProperties();
        super.tearDown();
    }

    public URL getKeyStore() {
        return this.getClass().getResource("serverstore.jks");
    }

    @Test public void simpleCamelHttpsCall() {
        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put(Exchange.HTTP_METHOD,"GET");
        InputStream result = (InputStream) template.sendBodyAndHeaders("https://localhost:" + PORT + "/say/hello/Charles?sslContextParametersRef=#scp",ExchangePattern.InOut,"",headers);
        assertEquals("\"Hello World Charles\"",inputStreamToString(result));
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
    @Test  public void sayByeNotAllowedForUserRoleTest() {
        String user = "Charles";
        String strURL = "https://" + HOST + ":" + PORT + "/say/bye/" + user;

        HttpResult result = callRestEndpoint("localhost", strURL, "donald", "duck", "MyRealm");
        assertEquals(403, result.getCode());
        // assertEquals(200, result.getCode());
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

        return new RouteBuilder() {
            @Override public void configure() throws Exception {

                final List<RestPropertyDefinition> jettyEndpointProps = new ArrayList<>();
                RestPropertyDefinition rpd = new RestPropertyDefinition();

                // Add key for the ConstraintSecurityHandler
                rpd.setKey("handlers");
                rpd.setValue("#myAuthHandler");
                jettyEndpointProps.add(rpd);

                // Add key of the SSL Context Parameter
                rpd = new RestPropertyDefinition();
                rpd.setKey("sslContextParametersRef");
                rpd.setValue("#scp");
                jettyEndpointProps.add(rpd);

                final List<RestPropertyDefinition> jettyComponentProps = new ArrayList<>();
                rpd = new RestPropertyDefinition();
                rpd.setKey("sslPassword");
                rpd.setValue(pwd);
                jettyComponentProps.add(rpd);

                rpd = new RestPropertyDefinition();
                rpd.setKey("sslKeyPassword");
                rpd.setValue(pwd);
                jettyComponentProps.add(rpd);

                rpd = new RestPropertyDefinition();
                rpd.setKey("keystore");
                rpd.setValue(getKeyStore().toURI().getPath());
                jettyComponentProps.add(rpd);

                RestConfigurationDefinition conf = restConfiguration().component("jetty")
                        .scheme("https")
                        .host("0.0.0.0")
                        .port(getPort1())
                        .bindingMode(RestBindingMode.json);

                conf.setEndpointProperties(jettyEndpointProps);
                conf.setComponentProperties(jettyComponentProps);
                        //
                        // 1) Test using : setEndpoint & setComponentProperties
                        // ISSUE : We can't combine Component & endpoint properties with DSL
                        // Discussed here : ENTESB-5432
                        //.setComponentProperties(jettyEndpointProps)
                        //.setEndpointProperties(jettyComponentProps);
                        //
                        // Workaround
                        //
                        // RestConfigurationDefinition conf = restConfiguration().component("jetty")
                        // .scheme("https").host("0.0.0.0").port(getPort1())
                        //         .bindingMode(RestBindingMode.json);
                        //
                        // conf.setEndpointProperties(jettyEndpointProps);
                        // conf.setComponentProperties(jettyComponentProps);
                        //
                        // 2) Using Endpoint Properties containing all the props
                        // ISSUE : java.io.EOFException: SSL peer shut down incorrectly at sun.security.ssl.InputRecord.read(InputRecord.java:505)
                        // .setEndpointProperties(jettyEndpointProps);
                        //
                        // 3) Without the List of the props but where we configure endpoint & component separately
                        // ISSUE : ConstraintSecurityHandler doesn't work when added at the endpoint or component
                        // ConstraintSecurityHandler doesn't work when added at the endpoint
                        // .endpointProperty("handlers","#myAuthHandler")
                        // ConstraintSecurityHandler doesn't work when added at the component
                        // .componentProperty("handlers","#myAuthHandler")
                        //
                        // .endpointProperty("sslContextParametersRef","#scp")
                        // .componentProperty("sslPassword",pwd)
                        // .componentProperty("sslKeyPassword",pwd)
                        // .componentProperty("keystore",getKeyStore().toURI().getPath());

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
        //mapping0.setMethod("GET");
        mapping0.setConstraint(constraint0);

        // Access alowed only for Admin role
        Constraint constraint1 = new Constraint();
        constraint1.setAuthenticate(true);
        constraint1.setName("allowedForRoleAdmin");
        constraint1.setRoles(new String[]{ "admin" });
        ConstraintMapping mapping1 = new ConstraintMapping();
        mapping1.setPathSpec("/say/bye/*");
        //mapping1.setMethod("GET");
        mapping1.setConstraint(constraint1);

        return Arrays.asList(mapping0, mapping1);
    }

}
