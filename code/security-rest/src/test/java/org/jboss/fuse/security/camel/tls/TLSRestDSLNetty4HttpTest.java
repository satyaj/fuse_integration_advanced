package org.jboss.fuse.security.camel.tls;

import org.apache.camel.CamelExecutionException;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.netty4.http.*;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.util.jsse.*;
import org.jboss.fuse.security.camel.common.BaseNetty4Test;
import org.junit.Test;

import java.net.URL;

public class TLSRestDSLNetty4HttpTest extends BaseNetty4Test {

    private static String SCHEME_HTTPS = "https";
    private static int PORT = getPort1();

    @Override
    protected JndiRegistry createRegistry() throws Exception {
        JndiRegistry jndi = super.createRegistry();

        // Netty with HTTPS scheme, JAAS Auth & Security Path Constraint & Role
        jndi.bind("nettyServerConfiguration", getNettyHttpSslConfiguration());
        jndi.bind("nettyServerSecurityConfig", getJAASSecurityHttpConfiguration());

        // jndi.bind("nettyClientConf", getNettyHttpClientSslConfiguration()); // DOESN't WORK for the producer
        // Pass to the Netty Producer the SSL Context Parameters
        jndi.bind("sslClientParameters", getClientSSLContextParameters());
        return jndi;
    }

    @Override
    public void setUp() throws Exception {
        // Realm included within the common file myrealm-jaas.cfg to avoid that the test fails when done with 'mvn clean test'
        // The object javax.security.auth.login.Configuration is instaiated one time / maven surefire session
        // and the object is not recreated with the System Prop
        URL jaasURL =  this.getClass().getResource("/org/jboss/fuse/security/basic/myrealm-jaas.cfg");
        setSystemProp("java.security.auth.login.config", jaasURL.toExternalForm());

        //URL trustStoreUrl = this.getClass().getResource("clientKeystore.jks");
        //setSystemProp("javax.net.ssl.trustStore", trustStoreUrl.toURI().getPath());

        setSystemProp("javax.net.debug","all");
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        restoreSystemProperties();
        super.tearDown();
    }

    @Test
    public void testFailAuth() {
        try {
            template.requestBody("netty4-http://https://localhost:" + PORT + "/say/hello/noauthheader","", String.class);
            fail("Should send back 401");
        } catch (CamelExecutionException e) {
            NettyHttpOperationFailedException cause = assertIsInstanceOf(NettyHttpOperationFailedException.class, e.getCause());
            assertEquals(401, cause.getStatusCode());
        }
    }

    @Test
    public void testBasicAuth() {
        // username:password is mickey:mouse
        String auth = "Basic bWlja2V5Om1vdXNl";
        String result = template.requestBodyAndHeader("netty4-http://https://localhost:" + PORT + "/say/hello/Mickey?ssl=true&sslContextParameters=#sslClientParameters", "", "Authorization", auth, String.class);
        assertEquals("\"Hello World Mickey\"", result);
    }

    @Test
    public void testBasicAuthSecConstraintWithoutAdminRole() {
        // username:password is donald:duck
        String auth = "Basic ZG9uYWxkOmR1Y2s=";

        // User without Admin Role
        try {
            String result = template.requestBodyAndHeader("netty4-http://https://localhost:" + PORT + "/say/hello/Donald?ssl=true&sslContextParameters=#sslClientParameters", "", "Authorization", auth, String.class);
            fail("Should send back 401");
        } catch (CamelExecutionException e) {
            //NettyHttpOperationFailedException cause = assertIsInstanceOf(NettyHttpOperationFailedException.class, e.getCause());
            //assertEquals(401, cause.getStatusCode());
            e.printStackTrace();
        }
    }

    @Test
    public void testBasicAuthAndSecConstraint() {
        // username:password is mickey:mouse
        String auth = "Basic bWlja2V5Om1vdXNl";

        // User with Role Admin
        String result = template.requestBodyAndHeader("netty4-http://https://localhost:" + PORT + "/say/hello/Mickey?ssl=true&sslContextParameters=#sslClientParameters", "", "Authorization", auth, String.class);
        assertEquals("\"Hello World Mickey\"", result);
    }

    // EXCLUDE-BEGIN
    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {

        return new RouteBuilder() {
            @Override public void configure() throws Exception {

                restConfiguration()
                    .component("netty4-http")
                    .scheme(SCHEME_HTTPS)
                    .host("0.0.0.0")
                    .port(getPort1()).bindingMode(RestBindingMode.json)
                    .endpointProperty("securityConfiguration", "#nettyServerSecurityConfig")
                    .endpointProperty("nettyHttpConfiguration","#nettyServerConfiguration")
                    .endpointProperty("traceEnabled","true");

                rest("/say").produces("json").post("/hello/{id}").to("direct:hello");

                from("direct:hello").transform().simple("Hello World ${header.id}");
            }
        };
    }

    private NettyHttpSecurityConfiguration getJAASSecurityHttpConfiguration() {
        NettyHttpSecurityConfiguration sec = new NettyHttpSecurityConfiguration();
        sec.setRealm("myrealm");
        sec.setAuthenticate(true);

        // Configure JAAS : add realm
        SecurityAuthenticator auth = new JAASSecurityAuthenticator();
        auth.setName("myrealm");
        sec.setSecurityAuthenticator(auth);

        SecurityConstraintMapping matcher = new SecurityConstraintMapping();
        matcher.addInclusion("/say/hello/*", "admin");
        sec.setSecurityConstraint(matcher);

        return sec;
    }

    /*
     * NettyHttpConfiguration with SSL parameters
     */
    private NettyHttpConfiguration getNettyHttpSslConfiguration() {
        NettyHttpConfiguration conf = new NettyHttpConfiguration();
/*        conf.setSsl(true);
        conf.setKeyStoreResource("org/jboss/fuse/security/camel/tls/serviceKeystore.jks");
        conf.setTrustStoreResource("org/jboss/fuse/security/camel/tls/serviceKeystore.jks");*/
        conf.setSslContextParameters(getSSLContextParameters());
        return conf;
    }

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
        scsp.setClientAuthentication(ClientAuthentication.WANT.name());

        /* Test with TLSv1
            Camel Thread #33 - NettyClientTCPWorker, fatal error: 80: Inbound closed before receiving peer's close_notify: possible truncation attack?
            javax.net.ssl.SSLException: Inbound closed before receiving peer's close_notify: possible truncation attack?
            Camel Thread #33 - NettyClientTCPWorker, SEND TLSv1.2 ALERT:  fatal, description = internal_error
            Camel Thread #33 - NettyClientTCPWorker, Exception sending alert: java.io.IOException: writer side was already closed.
         */

        SecureSocketProtocolsParameters sspp = new SecureSocketProtocolsParameters();
        sspp.getSecureSocketProtocol().add("TLSv1.2");

        scsp.setSecureSocketProtocols(sspp);

        SSLContextParameters scp = new SSLContextParameters();
        scp.setKeyManagers(kmp);
        scp.setTrustManagers(tmp);
        scp.setServerParameters(scsp);
        return scp;
    }
    // EXCLUDE-END

    /*
     * NettyHttpConfiguration with SSL parameters
     */
    private NettyHttpConfiguration getNettyHttpClientSslConfiguration() {
        NettyHttpConfiguration conf = new NettyHttpConfiguration();
        conf.setSsl(true);
        //conf.setKeyStoreResource("org/jboss/fuse/security/camel/tls/clientKeystore.jks");
        //conf.setTrustStoreResource("org/jboss/fuse/security/camel/tls/clientKeystore.jks");
        conf.setSslContextParameters(getClientSSLContextParameters());
        return conf;
    }

    private SSLContextParameters getClientSSLContextParameters() {
        // TLS
        KeyStoreParameters ksp = new KeyStoreParameters();
        ksp.setResource("org/jboss/fuse/security/camel/tls/clientKeystore.jks");
        ksp.setPassword("cspass");

        TrustManagersParameters tmp = new TrustManagersParameters();
        tmp.setKeyStore(ksp);

        //KeyManagersParameters kmp = new KeyManagersParameters();
        //kmp.setKeyStore(ksp);
        //kmp.setKeyPassword("ckpass");

        SecureSocketProtocolsParameters sspp = new SecureSocketProtocolsParameters();
        sspp.getSecureSocketProtocol().add("TLSv1.2");

        SSLContextParameters scp = new SSLContextParameters();
        scp.setSecureSocketProtocols(sspp);
        //scp.setKeyManagers(kmp);
        scp.setTrustManagers(tmp);
        return scp;
    }

}
