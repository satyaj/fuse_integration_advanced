package org.jboss.fuse.security.encryption;

import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.*;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;

public class EncryptDecryptPayloadTest extends CamelSpringTestSupport {

    private static final Logger LOG = LoggerFactory.getLogger(EncryptDecryptPayloadTest.class);

    Helper helper = new Helper();

    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext(
                "org/jboss/fuse/security/encryption/EncryptDecryptPayloadTest-context.xml");
    }

    @Test
    public void testXMLPayloadEncryption() throws Exception {
        helper.encryptXMLPayload(Helper.XML_REQUEST, context());
    }

    @Test
    public void testXMLPayloadDecryption() throws Exception {
        helper.encryptXMLPayloadAndDecrypt(Helper.XML_REQUEST, context());
    }

    @Test
    public void testEncryptedSOAPBody() {

        String strURL = "http://localhost:9001/camel/CheeseService";
        PostMethod post = new PostMethod(strURL);

        // Execute request
        try {
            // Request content will be retrieved directly
            // from the input stream
            RequestEntity entity = new StringRequestEntity(Helper.XML_REQUEST, "text/xml; charset=ISO-8859-1",
                    "UTF-8");
            post.setRequestEntity(entity);

            post.setRequestHeader("SOAPAction", "");
            // Get HTTP client
            HttpClient httpclient = new HttpClient();
            int result = httpclient.executeMethod(post);

            assertEquals(200, result);

            // Convert HTTP InputStream to Document
            InputStream is = post.getResponseBodyAsStream();
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            builderFactory.setNamespaceAware(true);
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document xmlSOAPBodyDocument = builder.parse(is);

            // Extract SOAP Body content from SOAP Body Encrypted Message using XPath
            String expression = "/soap:Envelope/soap:Body/xenc:EncryptedData";
            XPath xPath = XPathFactory.newInstance().newXPath();

            HashMap<String, String> prefMap = new HashMap<String, String>() {{
                put("soap", "http://schemas.xmlsoap.org/soap/envelope/");
                put("xenc", "http://www.w3.org/2001/04/xmlenc#");
            }};
            SimpleNameSpaceContext namespaces = new SimpleNameSpaceContext(prefMap);
            xPath.setNamespaceContext(namespaces);

            Node n = (Node) xPath.compile(expression).evaluate(xmlSOAPBodyDocument, XPathConstants.NODE);

            // Convert the content to a String & omit the XML declaration
            StringWriter writer = new StringWriter();
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(new DOMSource(n), new StreamResult(writer));
            String xmlBodyEncrypted = writer.toString();
            if(log.isDebugEnabled()) {
                LOG.debug(">> SOAP BODY ENCRYPTED : " + xmlBodyEncrypted);
            }

            // Verify that the decrypted message matches the response created by the Service
            helper.decryptXMLPayload(xmlBodyEncrypted, context());

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Release current connection to the connection pool once you are done
            post.releaseConnection();
        }

    }

}
