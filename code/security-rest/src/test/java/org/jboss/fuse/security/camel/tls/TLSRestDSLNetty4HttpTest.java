package org.jboss.fuse.security.camel.tls;

import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.netty4.http.*;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.util.jsse.*;
import org.jboss.fuse.security.camel.common.BaseNetty4Test;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;

public class TLSRestDSLNetty4HttpTest extends BaseNetty4Test {

    private static String SCHEME_HTTPS = "https";
    private static int PORT = getPort1();

    @Override
    protected JndiRegistry createRegistry() throws Exception {
        JndiRegistry jndi = super.createRegistry();

        // EXCLUDE-BEGIN
        // Netty with HTTPS scheme, JAAS Auth & Security Path Constraint & Role
        jndi.bind("sslServerParameters",getSSLContextParameters());
        jndi.bind("nettyServerSecurityConfig", getJAASSecurityHttpConfiguration());

        // Pass to the Netty Producer the SSL Context Parameters
        jndi.bind("sslClientParameters", getClientSSLContextParameters());
        // EXCLUDE-END
        return jndi;

    }

    @Override
    public void setUp() throws Exception {
        // Realm included within the common file myrealm-jaas.cfg to avoid that the test fails when done with 'mvn clean test'
        // The object javax.security.auth.login.Configuration is instaiated one time / maven surefire session
        // and the object is not recreated with the System Prop
        URL jaasURL =  this.getClass().getResource("/org/jboss/fuse/security/basic/myrealm-jaas.cfg");
        setSystemProp("java.security.auth.login.config", jaasURL.toExternalForm());

        //setSystemProp("javax.net.debug","all");
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        restoreSystemProperties();
        super.tearDown();
    }

    @Test
    public void testFailAuth() {
    // EXCLUDE-BEGIN
        try {
            template.requestBody("netty4-http://https://localhost:" + PORT + "/say/hello/noauthheader?ssl=true&sslContextParameters=#sslClientParameters","", String.class);
            fail("Should send back 401");
        } catch (CamelExecutionException e) {
            NettyHttpOperationFailedException cause = assertIsInstanceOf(NettyHttpOperationFailedException.class, e.getCause());
            assertEquals(401, cause.getStatusCode());
        }
    // EXCLUDE-END
    }

    @Test
    public void testBasicAuth()  {
    // EXCLUDE-BEGIN
        // username:password is mickey:mouse
        String auth = "Basic bWlja2V5Om1vdXNl";
        String result = template.requestBodyAndHeader("netty4-http://https://localhost:" + PORT + "/say/hello/Mickey?ssl=true&sslContextParameters=#sslClientParameters", "", "Authorization", auth, String.class);
        assertEquals("\"Hello World Mickey\"", result);
        // EXCLUDE-END
    }

    @Test
    public void testBasicAuthSecConstraintWithoutAdminRole() {
    // EXCLUDE-BEGIN
        // username:password is donald:duck
        String auth = "Basic ZG9uYWxkOmR1Y2s=";

        // User without Admin Role
        try {
            String result = template.requestBodyAndHeader("netty4-http://https://localhost:" + PORT + "/say/hello/Donald?ssl=true&sslContextParameters=#sslClientParameters", "", "Authorization", auth, String.class);
            fail("Should send back 401");
        } catch (CamelExecutionException e) {
            NettyHttpOperationFailedException cause = assertIsInstanceOf(NettyHttpOperationFailedException.class, e.getCause());
            assertEquals(401, cause.getStatusCode());
        }
    }

    @Test
    public void testHttpRequest() {
    // EXCLUDE-BEGIN
        // username:password is mickey:mouse
        String auth = "Basic bWlja2V5Om1vdXNl";

        try {
            String result = template
                    .requestBodyAndHeader("netty4-http://http://localhost:" + PORT + "/say/hello/Mickey", "",
                            "Authorization", auth, String.class);
            assertEquals("\"Hello World Mickey\"", result);
        } catch (CamelExecutionException e) {
            Assert.assertEquals(true,e.getCause().getMessage().contains("No response received from remote server"));
        }
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {

        // EXCLUDE-BEGIN
        return new RouteBuilder() {
            @Override public void configure() throws Exception {

                onException(Exception.class)
                    .handled(true)
                    .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
                    .setBody().constant("HTTP protocol is not supported but HTTPS");

                 restConfiguration()
                     .component("netty4-http")
                     .scheme(SCHEME_HTTPS)
                     .host("0.0.0.0")
                     .port(getPort1()).bindingMode(RestBindingMode.json)
                     .endpointProperty("securityConfiguration", "#nettyServerSecurityConfig")
                     .endpointProperty("sslContextParameters","#sslServerParameters")
                     .endpointProperty("ssl","true")
                     .endpointProperty("traceEnabled","true");


                rest("/say").produces("json").post("/hello/{id}").to("direct:hello");

                from("direct:hello").transform().simple("Hello World ${header.id}");
            }
        };
        // EXCLUDE-END
    }

    // EXCLUDE-BEGIN
    private NettyHttpSecurityConfiguration getJAASSecurityHttpConfiguration() {
        NettyHttpSecurityConfiguration sec = new NettyHttpSecurityConfiguration();
        sec.setRealm("myrealm");
        sec.setAuthenticate(true);

        // Configure JAAS : add realm
        SecurityAuthenticator auth = new JAASSecurityAuthenticator();
        auth.setName("myrealm");
        sec.setSecurityAuthenticator(auth);

        // Include a Security constraint to restrict access to the /say/hello/* path for admin role users
        SecurityConstraintMapping matcher = new SecurityConstraintMapping();
        matcher.addInclusion("/say/hello/*", "admin");
        sec.setSecurityConstraint(matcher);

        return sec;
    }
    // EXCLUDE-END

    // EXCLUDE-BEGIN
    private SSLContextParameters getSSLContextParameters() {
        // TLS
        KeyStoreParameters ksp = new KeyStoreParameters();
        ksp.setResource("org/jboss/fuse/security/camel/tls/serviceKeystore.jks");
        ksp.setPassword("sspass");

        KeyManagersParameters kmp = new KeyManagersParameters();
        kmp.setKeyStore(ksp);
        kmp.setKeyPassword("skpass");

        TrustManagersParameters tmp = new TrustManagersParameters();
        tmp.setKeyStore(ksp);

        // NOTE: Needed since the client uses a loose trust configuration when no ssl context
        // is provided.  We turn on WANT client-auth to prefer using authentication
        SSLContextServerParameters scsp = new SSLContextServerParameters();
        scsp.setClientAuthentication(ClientAuthentication.REQUIRE.name());

        SSLContextParameters scp = new SSLContextParameters();
        scp.setKeyManagers(kmp);
        scp.setTrustManagers(tmp);
        scp.setServerParameters(scsp);
        return scp;
    }
    // EXCLUDE-END

    // EXCLUDE-BEGIN
    private SSLContextParameters getClientSSLContextParameters() {
        // TLS
        KeyStoreParameters ksp = new KeyStoreParameters();
        ksp.setResource("org/jboss/fuse/security/camel/tls/clientKeystore.jks");
        ksp.setPassword("cspass");

        TrustManagersParameters tmp = new TrustManagersParameters();
        tmp.setKeyStore(ksp);

        SSLContextParameters scp = new SSLContextParameters();
        scp.setTrustManagers(tmp);
        return scp;
    }
    // EXCLUDE-END

}
