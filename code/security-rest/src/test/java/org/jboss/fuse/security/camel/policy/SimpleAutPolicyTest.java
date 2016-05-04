package org.jboss.fuse.security.camel.policy;

import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.netty4.http.NettyHttpOperationFailedException;
import org.apache.camel.model.rest.RestBindingMode;
import org.jboss.fuse.security.camel.common.BaseNetty4Test;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class SimpleAutPolicyTest extends BaseNetty4Test {

    private static String HOST = "localhost";
    private static int PORT = getPort1();

    @Test public void testBasicAuth() {
        String result;

        // Unauthorized
        try {
            template.requestBodyAndHeader("netty4-http://http://localhost:" + PORT + "/say/hello/noauthheader", "", Exchange.HTTP_METHOD,"GET",String.class);
            fail("Should send back 500");
        } catch (CamelExecutionException e) {
            NettyHttpOperationFailedException cause = assertIsInstanceOf(
                    NettyHttpOperationFailedException.class, e.getCause());
            assertEquals(500, cause.getStatusCode());
        }

        // Authorized with username:password is mickey:mouse
        String auth = "Basic bWlja2V5Om1vdXNl";
        Map<String,Object> headers = new HashMap<String, Object>();
        headers.put("Authorization", auth);
        headers.put(Exchange.HTTP_METHOD,"GET");
        result = template.requestBodyAndHeaders("netty4-http://http://localhost:" + PORT + "/say/hello/Donald", "",headers, String.class);
        assertEquals("\"Hello World Donald\"", result);
    }

    // EXCLUDE-BEGIN
    @Override protected RouteBuilder createRouteBuilder() throws Exception {

        final SimpleAuthenticationPolicy auth = new SimpleAuthenticationPolicy();

        return new RouteBuilder() {

            @Override public void configure() throws Exception {

                onException(AuthenticationException.class)
                   .process(new Processor() {
                       @Override public void process(Exchange exchange) throws Exception {
                           Exception ae = exchange.getException();
                           Response resp = Response.status(Response.Status.NOT_FOUND).entity(ae.getMessage()).build();
                           exchange.getIn().setBody(resp);
                       }
                   });

                restConfiguration().component("netty4-http").scheme("http").host("0.0.0.0").port(getPort1())
                        .bindingMode(RestBindingMode.json);

                rest("/say").produces("json").get("/hello/{id}").to("direct:hello");

                from("direct:hello")
                   .policy(auth)
                   .transform()
                   .simple("Hello World ${header.id}");

            }
        };
    }
    // EXCLUDE-END
}
