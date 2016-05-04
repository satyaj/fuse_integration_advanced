package org.jboss.fuse.security.camel.tls;

import org.apache.camel.CamelExecutionException;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.netty4.http.*;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.camel.util.jsse.KeyManagersParameters;
import org.apache.camel.util.jsse.KeyStoreParameters;
import org.apache.camel.util.jsse.SSLContextParameters;
import org.jboss.fuse.security.camel.common.BaseNetty4Test;
import org.junit.Test;

import java.net.URL;

public class TLSRestDSLNetty4HttpTest extends BaseNetty4Test {

    private static String SCHEME_HTTPS = "https";
    private static int PORT = getPort1();
    protected String pwd = "secUr1t8";

    @Override
    protected JndiRegistry createRegistry() throws Exception {
        JndiRegistry jndi = super.createRegistry();

        // Netty with HTTPS scheme, JAAS Auth & Security Path Constraint & Role
        jndi.bind("nettyConf", getNettyHttpSslConfiguration());
        jndi.bind("mySecurityConfig", getJAASSecurityHttpConfiguration());
        return jndi;
    }

    @Override
    public void setUp() throws Exception {
        // Realm included within the common file myrealm-jaas.cfg to avoid that the test fails when done with 'mvn clean test'
        // The object javax.security.auth.login.Configuration is instaiated one time / maven surefire session
        // and the object is not recreated with the System Prop
        URL jaasURL =  this.getClass().getResource("/org/jboss/fuse/security/basic/myrealm-jaas.cfg");
        setSystemProp("java.security.auth.login.config", jaasURL.toExternalForm());

        URL trustStoreUrl = this.getClass().getResource("serverstore.jks");
        setSystemProp("javax.net.ssl.trustStore", trustStoreUrl.toURI().getPath());
        //setSystemProp("javax.net.debug","ssl,handshake,data");
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        restoreSystemProperties();
        super.tearDown();
    }

    @Test
    public void testBasicAuth() {
        String result;

        try {
            template.requestBody("netty4-http://https://localhost:" + PORT + "/say/hello/noauthheader","", String.class);
            fail("Should send back 401");
        } catch (CamelExecutionException e) {
            NettyHttpOperationFailedException cause = assertIsInstanceOf(NettyHttpOperationFailedException.class, e.getCause());
            assertEquals(401, cause.getStatusCode());
        }

        // username:password is mickey:mouse
        String auth = "Basic bWlja2V5Om1vdXNl";
        result = template.requestBodyAndHeader("netty4-http://https://localhost:" + PORT + "/say/hello/Mickey", "", "Authorization", auth, String.class);
        assertEquals("\"Hello World Mickey\"", result);
    }

    @Test
    public void testBasicAuthAndSecConstraint() {
        String result;
        // username:password is donald:duck
        String auth = "Basic ZG9uYWxkOmR1Y2s=";

        // User without Admin Role
        try {
            result = template.requestBodyAndHeader("netty4-http://https://localhost:" + PORT + "/say/hello/Donald", "", "Authorization", auth, String.class);
            fail("Should send back 401");
        } catch (CamelExecutionException e) {
            NettyHttpOperationFailedException cause = assertIsInstanceOf(NettyHttpOperationFailedException.class, e.getCause());
            assertEquals(401, cause.getStatusCode());
        }

        // username:password is mickey:mouse
        auth = "Basic bWlja2V5Om1vdXNl";

        // User with Role Admin
        result = template.requestBodyAndHeader("netty4-http://https://localhost:" + PORT + "/say/hello/Mickey", "", "Authorization", auth, String.class);
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
                    .endpointProperty("securityConfiguration", "#mySecurityConfig")
                    .endpointProperty("nettyHttpConfiguration","#nettyConf")
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
        conf.setSsl(true);
        conf.setKeyStoreResource("org/jboss/fuse/security/camel/tls/serverstore.jks");
        conf.setTrustStoreResource("org/jboss/fuse/security/camel/tls/serverstore.jks");
        conf.setPassphrase(pwd);
        conf.setSslContextParameters(getSSLContextParameters());
        return conf;
    }

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
    // EXCLUDE-END

}
