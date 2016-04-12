package org.jboss.fuse.security.encryption;

import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.local.LocalTransportFactory;
import org.jboss.fuse.security.SecurityTestUtil;
import org.jboss.fuse.security.Server;
import org.jboss.fuse.security.service.Echo;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class EncryptDecryptPayloadTest extends CamelSpringTestSupport {

    TestHelper testHelper = new TestHelper();

    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext(
                "org/jboss/fuse/security/encryption/EncryptDecryptPayloadTest-context.xml");
    }

    @Test
    public void testPayloadEncrypt() throws Exception {
        testHelper.testEncryption(TestHelper.XML_REQUEST, context());
    }

   @Test
    public void testPayloadDecryption() throws Exception {
        testHelper.testDecryption(TestHelper.XML_REQUEST, context());
    }

    @Test
    public void testEncryptedResponseFromWebService() {
        JaxWsProxyFactoryBean fact = new JaxWsProxyFactoryBean();
        fact.setServiceClass(CheeseProcess.class);
        fact.setAddress("http://localhost:9001/camel/CheeseService");

        CheeseProcess cheese = (CheeseProcess) fact.create();
        Client client = ClientProxy.getClient(cheese);
        Country country = cheese.processCheese("parmezan");

        assertNotNull(country);

    }


}
