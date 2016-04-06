package org.jboss.fuse.security;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.jboss.helloworld.Greeter;
import org.jboss.fuse.security.service.Echo;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.URL;

public class WSSecurityPolicyTest extends AbstractBusClientServerTestBase {

    private static final String PORT = allocatePort(Server.class);
    private static final String NAMESPACE = "http://jboss.org/HelloWorld";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "HelloService");

    protected WSS4JInInterceptor wsIn;
    protected WSS4JOutInterceptor wsOut;
    private Echo echo;
    private Client client;
    private JaxWsServerFactoryBean factory;

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("Server failed to launch",
                // run the server in the same process
                // set this to false to fork
                launchServer(Server.class, true));
    }

    @AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
        stopAllServers();
    }


    /**
     * Create a SOAP Message where the SOAP Header includes a wsse section with a username and timestamp to authenticate the JAXWS Client
     */
    @Test public void testUsernameToken() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = WSSecurityPolicyTest.class.getResource("/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = WSSecurityPolicyTest.class.getResource("/hello_world.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "HelloWorldPort");
        Greeter greeter =
                service.getPort(portQName, Greeter.class);

        String response = greeter.greetMe("Hello");
        assertEquals(response,"hello");

        ((java.io.Closeable)greeter).close();
        bus.shutdown(true);
    }

}
