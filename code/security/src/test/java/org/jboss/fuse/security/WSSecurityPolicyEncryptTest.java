package org.jboss.fuse.security;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.jboss.fuse.security.service.Echo;
import org.jboss.helloworld.Greeter;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.io.IOException;
import java.net.URL;

public class WSSecurityPolicyEncryptTest extends AbstractBusClientServerTestBase {

    private static final String PORT = allocatePort(Server.class);
    private static final String NAMESPACE = "http://jboss.org/HelloWorld";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "GreeterService");

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
                launchServer(Server.class, null, new String[] { "server-sign-encrypt.xml" }, true));
    }

    @AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
        stopAllServers();
    }


    @Test public void testEncryptionPlusSig() throws Exception {
        URL busFile = WSSecurityPolicyEncryptTest.class.getResource("/client-signencrypt.xml");
        runandValidate(busFile,"GreeterSignEncryptPort","Hello Charles", "hello_world_sign_encrypt.wsdl");
    }

    private void runandValidate(URL busFile, String portName, String assertString, String wsdlFile) throws IOException {
        SpringBusFactory bf = new SpringBusFactory();

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = WSSecurityPolicyEncryptTest.class.getResource("/" + wsdlFile);
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, portName);
        Greeter greeter =
                service.getPort(portQName, Greeter.class);

        String response = greeter.greetMe("Charles");
        assertEquals(response,assertString);

        ((java.io.Closeable)greeter).close();
        bus.shutdown(true);
    }
}
