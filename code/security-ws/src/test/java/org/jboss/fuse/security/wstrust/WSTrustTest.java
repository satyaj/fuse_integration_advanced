package org.jboss.fuse.security.wstrust;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.jboss.fuse.security.SecurityTestUtil;
import org.jboss.fuse.security.Server;
import org.apache.hello_world_soap_http.Greeter;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.URL;

public class WSTrustTest extends AbstractBusClientServerTestBase {

    private static final QName SERVICE_NAME = new QName("http://apache.org/hello_world_soap_http",
            "SOAPService");

    private static final QName PORT_NAME = new QName("http://apache.org/hello_world_soap_http", "SoapPort");

    @BeforeClass public static void startServers() throws Exception {
        assertTrue("STS Server failed to launch",
                // run the server in the same process
                // set this to false to fork
                launchServer(Server.class, null,
                        new String[] { "/org/jboss/fuse/security/wstrust/wssec-sts.xml" }, true));
        assertTrue("Server failed to launch",
                // run the server in the same process
                // set this to false to fork
                launchServer(Server.class, null,
                        new String[] { "/org/jboss/fuse/security/wstrust/wssec-server.xml" }, true));
    }

    @AfterClass public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
        stopAllServers();
    }

    @Test public void testSimpleClient() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = WSTrustTest.class.getResource("wssec-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdlURL = WSTrustTest.class.getResource("hello_world.wsdl");

        Service service = Service.create(wsdlURL, SERVICE_NAME);
        Greeter port = service.getPort(PORT_NAME, Greeter.class);

        String resp = port.greetMe(System.getProperty("user.name"));
        assertEquals("Hello " + System.getProperty("user.name"), resp);

        ((java.io.Closeable) port).close();
        bus.shutdown(true);
    }
}

