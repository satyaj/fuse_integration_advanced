package org.jboss.fuse.security.apiman;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.jboss.fuse.security.apiman.junit.Configuration;
import org.jboss.fuse.security.apiman.junit.GatewayTestSupport;
import org.jboss.fuse.security.apiman.junit.GatewayTester;
import org.jboss.fuse.security.apiman.junit.GatewayTesterSystemProperties;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RunWith(GatewayTester.class)
@GatewayTesterSystemProperties({
        "apiman-gateway-test.endpoint", "http://localhost:9191"
})
@Configuration(cpConfigFiles = {
        "test-plan-data/policies/basic-auth-static/001-publish-api.resttest",
        "test-plan-data/policies/basic-auth-static/002-register-client.resttest" })
public class BasicAuthenticationTest extends GatewayTestSupport {

    Map<String,String> headers = new HashMap<String,String>();

    @Test
    public void testBasicAuthenticationAgainstBackendApi() throws IOException {
        // EXCLUDE-BEGIN
        String user = "Charles";
        String expectedResponse = "\"Hello World " + user + "\"";
        headers.put("X-API-Key","12345");

        runAndValidate(expectedResponse,200,"http://localhost:6060/gateway/Policy_BasicAuthStatic/message/1.0/say/hello/" + user,"","GET",headers,"bwayne","bwayne");
        // EXCLUDE-END
    }

    @Test
    public void testFailBasicAuthenticationAgainstBackendApi() throws IOException {
        // EXCLUDE-BEGIN
        String user = "Charles";
        String expectedResponse = "";
        headers.put("X-API-Key","12345");

        runAndValidate(expectedResponse,401,"http://localhost:6060/gateway/Policy_BasicAuthStatic/message/1.0/say/hello/" + user,"","GET",headers,"bwayne","wrongpassword");
        // EXCLUDE-END
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {

        return new RouteBuilder() {
            @Override public void configure() throws Exception {

                restConfiguration()
                    .component("jetty").scheme("http")
                    .host("0.0.0.0").port(9191)
                    .bindingMode(RestBindingMode.json);

                rest("/say").produces("json")
                    .get("/hello/{id}")
                    .to("direct:hello");

                from("direct:hello").transform().simple("Hello World ${header.id}");

            }
        };
    }

}
