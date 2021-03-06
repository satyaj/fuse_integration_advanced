package org.jboss.fuse.security.cxf.intercept;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.jboss.fuse.security.cxf.service.CustomerServiceImpl;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Scanner;

public class JAXRSContainerFilterTest extends AbstractBusClientServerTestBase {

    public static final String PORT = allocatePort(Server.class);

    @Ignore
    public static class Server extends AbstractBusTestServerBase {
        org.apache.cxf.endpoint.Server server;

        protected void run() {
            JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            // EXCLUDE-BEGIN
            sf.setProvider(new SimpleAuthenticationFilter());
            sf.setAddress("http://localhost:" + PORT + "/");
            sf.setResourceClasses(CustomerServiceImpl.class);
            sf.setResourceProvider(CustomerServiceImpl.class, new SingletonResourceProvider(new CustomerServiceImpl()));

            sf.setAddress("http://localhost:" + PORT + "/");
            server = sf.create();
            // EXCLUDE-END
        }

        @Override public void tearDown() {
            server.stop();
            server.destroy();
            server = null;
        }

        public static void main(String[] args) {
            try {
                Server s = new Server();
                s.start();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(-1);
            } finally {
                System.out.println("done!");
            }
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
        createStaticBus();
    }

    @Test
    public void testBasicAuth() throws Exception {
        // EXCLUDE-BEGIN
        getAndCompare("http://localhost:" + PORT + "/customerservice/customers/123", "application/xml", "Basic bWlja2V5Om1vdXNl", 200, "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Customer><id>123</id><name>John</name></Customer>");
        // EXCLUDE-END
    }

    @Test
    public void testFailAuth() throws Exception {
        // EXCLUDE-BEGIN
        getAndCompare("http://localhost:" + PORT + "/customerservice/customers/123", "application/xml", "Basic bWlja2V5Om1vdXNllll", 401, "<message>Authentication failed</message>");
        // EXCLUDE-END
    }

    private void getAndCompare(String address, String acceptType, String auth, int expectedStatus, String response)
            throws Exception {

        GetMethod get = new GetMethod(address);
        get.setRequestHeader("Accept", acceptType);
        get.setRequestHeader(HttpHeaders.AUTHORIZATION,auth);

        HttpClient httpClient = new HttpClient();
        try {
            int result = httpClient.executeMethod(get);
            assertEquals(expectedStatus, result);

            InputStream is = get.getResponseBodyAsStream();
            Scanner s = new Scanner(is).useDelimiter("\\A");
            assertEquals(response,s.hasNext() ? s.next() : "");

        } finally {
            get.releaseConnection();
        }
    }

    @PreMatching
    private static class SimpleAuthenticationFilter implements ContainerRequestFilter {

        public void filter(ContainerRequestContext requestContext) throws IOException {
            // EXCLUDE-BEGIN

            String auth = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

            // Remove 'Base ' text from the String
            String base64 = auth.substring(6,auth.length());

            // Decrypt the base64 text
            String decoded = new String(Base64.getDecoder().decode(base64), "UTF-8");

            String[] values = decoded.split(":");

            if("mouse".equals(values[1]) && "mickey".equals(values[0])) {
                System.out.println("User authenticated !");
            } else {
                requestContext
                    .abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("<message>Authentication failed</message>")
                    .build());
            }
            // EXCLUDE-END
        }

    }
}
