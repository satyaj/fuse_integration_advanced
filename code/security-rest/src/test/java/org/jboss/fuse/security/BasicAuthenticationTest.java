package org.jboss.fuse.security;

import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Test;

public class BasicAuthenticationTest extends BaseJettyTest {

    // EXCLUDE-BEGIN
    @Test
    public void UsernameTest() {
        String user = "Charles";
        String strURL = "http://localhost:" + getPort() + "/say/hello/" + user;
        GetMethod get = new GetMethod(strURL);

        // Execute request
        try {
            // Get HTTP client
            HttpClient httpclient = new HttpClient();
            int result = httpclient.executeMethod(get);
            assertEquals(200, result);

            String response = get.getResponseBodyAsString();
            assertEquals("We should get a Hello World", "Hello World Charles", response);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Release current connection to the connection pool once you are done
           get.releaseConnection();
        }
    }
    // EXCLUDE-END

    // EXCLUDE-BEGIN
    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {

                restConfiguration()
                    .component("jetty")
                    .scheme("http")
                    .host("0.0.0.0")
                    .port(getPort());

                rest("/say")
                    .get("/hello/{id}").to("direct:hello")
                    .get("/bye").consumes("application/json").to("direct:bye")
                    .post("/bye").to("mock:update");

                from("direct:hello")
                        .transform().simple("Hello World ${header.id}");
                from("direct:bye")
                        .transform().constant("Bye World");
            }
        };
    }
    // EXCLUDE-END



}
