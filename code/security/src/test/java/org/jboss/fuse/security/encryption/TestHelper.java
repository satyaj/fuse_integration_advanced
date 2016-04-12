/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.fuse.security.encryption;

import com.sun.webkit.dom.DocumentImpl;
import org.apache.camel.*;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.converter.jaxp.XmlConverter;
import org.apache.camel.converter.stream.StreamCacheConverter;
import org.apache.cxf.helpers.IOUtils;
import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.encryption.XMLEncryptionException;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;

public class TestHelper {

    protected static final String XML_REQUEST = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:enc=\"http://encryption.security.fuse.jboss.org\">\n"
            + "   <soapenv:Header/>\n" + "   <soapenv:Body>\n" + "      <enc:processCheese>\n"
            + "         <arg0>parmezan</arg0>\n" + "      </enc:processCheese>\n" + "   </soapenv:Body>\n"
            + "</soapenv:Envelope>";

    static final boolean HAS_3DES;

    static {
        boolean ok = false;
        try {
            org.apache.xml.security.Init.init();
            XMLCipher.getInstance(XMLCipher.TRIPLEDES_KeyWrap);
            ok = true;
        } catch (XMLEncryptionException e) {
            e.printStackTrace();
        }
        HAS_3DES = ok;
    }

    static final boolean UNRESTRICTED_POLICIES_INSTALLED;

    static {
        boolean ok = false;
        try {
            byte[] data = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07 };

            SecretKey key192 = new SecretKeySpec(
                    new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c,
                            0x0d, 0x0e, 0x0f, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17 }, "AES");
            Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.ENCRYPT_MODE, key192);
            c.doFinal(data);
            ok = true;
        } catch (Exception e) {
            //
        }
        UNRESTRICTED_POLICIES_INSTALLED = ok;
    }

    private static Logger log = LoggerFactory.getLogger(TestHelper.class);

    protected void sendText(String URI, final Object msg, CamelContext context) throws Exception {
        ProducerTemplate template = context.createProducerTemplate();
        template.start();
        template.send(URI, new Processor() {
            public void process(Exchange exchange) throws Exception {
                // Set the property of the charset encoding
                exchange.setProperty(Exchange.CHARSET_NAME, "UTF-8");
                Message in = exchange.getIn();
                in.setBody(msg);
                log.info(">> CLEAR Message: {}", msg);
            }
        });
    }

    protected void testEncryption(String msg, CamelContext context) throws Exception {
        MockEndpoint resultEndpoint = context.getEndpoint("mock:encrypted", MockEndpoint.class);
        resultEndpoint.setExpectedMessageCount(1);
        context.start();
        sendText("direct:encrypt", msg, context);
        resultEndpoint.assertIsSatisfied(100);
        Exchange exchange = resultEndpoint.getExchanges().get(0);
        Document inDoc = getDocumentForInMessage(exchange);
        if (log.isDebugEnabled()) {
            logMessage(">> MESSAGE ENCRYPTED", exchange, inDoc);
        }
        Assert.assertTrue("The XML message has no encrypted data.", hasEncryptedData(inDoc));
    }

    protected void testDecryption(String msg, CamelContext context) throws Exception {

        MockEndpoint encrypted = context.getEndpoint("mock:encrypted", MockEndpoint.class);

        MockEndpoint decrypted = context.getEndpoint("mock:decrypted", MockEndpoint.class);
        decrypted.setExpectedMessageCount(1);

        // Send clear message and encrypt it
        context.start();
        sendText("direct:encrypt", msg, context);
        Exchange exchange = encrypted.getExchanges().get(0);
        Document inDoc = getDocumentForInMessage(exchange);

        sendText("direct:decrypt",inDoc,context);

        decrypted.assertIsSatisfied(100);
        exchange = decrypted.getExchanges().get(0);
        inDoc = getDocumentForInMessage(exchange);
        if (log.isDebugEnabled()) {
            logMessage(">> MESSAGE DECRYPTED",exchange, inDoc);
        }
        Assert.assertFalse("The XML message has encrypted data.", hasEncryptedData(inDoc));

        // verify that the decrypted message matches what was sent
        Document fragmentDoc = createDocumentfromInputStream(new ByteArrayInputStream(msg.getBytes()),
                exchange);
        Diff xmlDiff = XMLUnit.compareXML(fragmentDoc, inDoc);

        Assert.assertTrue("The decrypted document does not match the control document.", xmlDiff.identical());
    }

    private boolean hasEncryptedData(Document doc) throws Exception {
        NodeList nodeList = doc.getElementsByTagNameNS("http://www.w3.org/2001/04/xmlenc#", "EncryptedData");
        return nodeList.getLength() > 0;
    }

    private void logMessage(String info, Exchange exchange, Document inDoc) throws Exception {
        XmlConverter converter = new XmlConverter();
        String xmlStr = converter.toString(inDoc, exchange);
        log.debug(info + ": " + xmlStr);
    }

    private Document getDocumentFromByteArrayStream(Exchange exchange) {
        ByteArrayInputStream body = (ByteArrayInputStream) exchange.getIn().getBody();
        Document d = createDocumentfromInputStream(body, exchange);
        return d;
    }

    private Document getDocumentForInMessage(Exchange exchange) {
        byte[] body = exchange.getIn().getBody(byte[].class);
        Document d = createDocumentfromInputStream(new ByteArrayInputStream(body), exchange);
        return d;
    }

    private Document createDocumentfromInputStream(InputStream is, Exchange exchange) {
/*        StreamCache streamCache = null;
        String message = null;
        try {
            streamCache = StreamCacheConverter.convertToStreamCache(is,exchange);
            message = exchange.getContext().getTypeConverter().convertTo(String.class, streamCache);
            logger.info("MESSAGE : " + message);
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        return exchange.getContext().getTypeConverter().convertTo(Document.class, is);
    }

}
