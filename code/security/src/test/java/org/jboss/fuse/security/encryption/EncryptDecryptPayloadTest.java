package org.jboss.fuse.security.encryption;

import org.apache.camel.test.spring.CamelSpringTestSupport;
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
}
