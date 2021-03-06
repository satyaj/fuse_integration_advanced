package org.jboss.fuse.security.wssecuritypolicy;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.jboss.fuse.security.SecurityTestUtil;
import org.jboss.fuse.security.Server;
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

    @BeforeClass
    public static void startServers() throws Exception {
        // EXCLUDE-BEGIN
        assertTrue("Server failed to launch",
                // run the server in the same process
                // set this to false to fork
                launchServer(Server.class, null, new String[] { "/org/jboss/fuse/security/wssecuritypolicy/server-sign-encrypt.xml" }, true));
        // EXCLUDE-END
    }

    @AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
        stopAllServers();
    }


    @Test
    public void testEncryptionPlusSig() throws Exception {
        // EXCLUDE-BEGIN
        URL busFile = WSSecurityPolicyEncryptTest.class.getResource("client-signencrypt.xml");
        runandValidate(busFile,"GreeterSignEncryptPort","Hello Charles", "org/jboss/fuse/security/wssecuritypolicy/hello_world.wsdl");
        // EXCLUDE-END
    }

    private void runandValidate(URL busFile, String portName, String assertString, String wsdlFile) throws IOException {
        // EXCLUDE-BEGIN
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
        // EXCLUDE-END
    }
}
