package org.jboss.fuse.security.basic;

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
import org.jboss.fuse.security.common.BaseJettyTest;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class BasicAuthenticationRESTCamelDSLJettyJaasTest extends BaseJettyTest {

    private static String HOST = "localhost";
    private static int PORT = getPort1();

    @Override protected JndiRegistry createRegistry() throws Exception {
        JndiRegistry jndi = super.createRegistry();
        jndi.bind("myAuthHandler", getSecurityHandler());
        return jndi;
    }

    @Before
    public void init() throws IOException {
        URL jaasURL = BasicAuthenticationRESTCamelDSLJettyJaasTest.class.getResource("myrealm-jaas.cfg");
        System.setProperty("java.security.auth.login.config", jaasURL.toExternalForm());
    }

    private SecurityHandler getSecurityHandler() throws IOException {
        Constraint constraint = new Constraint(Constraint.__BASIC_AUTH, "user");
        constraint.setAuthenticate(true);

        ConstraintMapping cm = new ConstraintMapping();
        cm.setPathSpec("/*");
        cm.setConstraint(constraint);

        ConstraintSecurityHandler sh = new ConstraintSecurityHandler();
        sh.setAuthenticator(new BasicAuthenticator());
        sh.setConstraintMappings(Arrays.asList(new ConstraintMapping[] { cm }));

        DefaultIdentityService dis = new DefaultIdentityService();

        JAASLoginService loginService = new JAASLoginService();
        loginService.setName("myrealm");
        loginService.setLoginModuleName("propsFileModule");
        loginService.setIdentityService(dis);

        sh.setLoginService(loginService);
        sh.setConstraintMappings(Arrays.asList(new ConstraintMapping[] { cm }));

        return sh;
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

    protected HttpResult runAndValidate(String host, String url, String user, String password, String realm) {

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

                from("direct:hello").transform().simple("Hello World ${header.id}");

            }
        };
    }
    // EXCLUDE-END

    protected String inputStreamToString(InputStream is) {
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    protected class HttpResult {
        int code;
        String message;

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

    }

}