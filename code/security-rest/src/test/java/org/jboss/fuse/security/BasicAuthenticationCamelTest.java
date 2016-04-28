package org.jboss.fuse.security;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.JndiRegistry;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.util.security.Constraint;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;

public class BasicAuthenticationCamelTest extends BaseJettyTest {

    @Override
    protected JndiRegistry createRegistry() throws Exception {
        JndiRegistry jndi = super.createRegistry();
        jndi.bind("myAuthHandler", getSecurityHandler());
        return jndi;
    }

    private SecurityHandler getSecurityHandler() throws IOException {
        Constraint constraint = new Constraint(Constraint.__BASIC_AUTH, "user");
        constraint.setAuthenticate(true);

        ConstraintMapping cm = new ConstraintMapping();
        cm.setPathSpec("/*");
        cm.setConstraint(constraint);

        ConstraintSecurityHandler sh = new ConstraintSecurityHandler();
        sh.setAuthenticator(new BasicAuthenticator());
        sh.setConstraintMappings(Arrays.asList(new ConstraintMapping[] {cm}));

        HashLoginService loginService = new HashLoginService("MyRealm", "src/test/resources/org/jboss/fuse/basic/myRealm.properties");
        sh.setLoginService(loginService);
        sh.setConstraintMappings(Arrays.asList(new ConstraintMapping[]{cm}));

        return sh;
    }

    @Test
    public void testHttpBasicAuthCamel() throws Exception {
        String out = template.requestBody("http://localhost:{{port1}}/test?authMethod=Basic&authUsername=donald&authPassword=duck", "Hello World", String.class);
        assertEquals("Bye World", out);
    }

    // EXCLUDE-BEGIN
    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {

        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {

                from("jetty://http://localhost:{{port1}}/test?handlers=myAuthHandler")
                        .process(new Processor() {
                            public void process(Exchange exchange) throws Exception {
                                HttpServletRequest req = exchange.getIn().getBody(HttpServletRequest.class);
                                assertNotNull(req);
                                Principal user = req.getUserPrincipal();
                                assertNotNull(user);
                                assertEquals("donald", user.getName());
                            }
                        })
                        .transform(constant("Bye World"));
            }
        };
    }
    // EXCLUDE-END



}
