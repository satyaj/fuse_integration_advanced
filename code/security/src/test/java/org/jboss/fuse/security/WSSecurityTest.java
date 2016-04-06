package org.jboss.fuse.security;

import org.apache.cxf.binding.soap.saaj.SAAJInInterceptor;
import org.apache.cxf.binding.soap.saaj.SAAJOutInterceptor;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.service.Service;
import org.apache.cxf.transport.local.LocalTransportFactory;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.handler.WSHandlerConstants;

import org.apache.wss4j.stax.ext.WSSConstants;
import org.jboss.fuse.security.service.Echo;
import org.jboss.fuse.security.service.EchoImpl;
import org.jboss.fuse.security.service.PwdCallback;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WSSecurityTest extends AbstractSecurityTest {

    protected WSS4JInInterceptor wsIn;
    protected WSS4JOutInterceptor wsOut;
    private Echo echo;
    private Client client;
    private JaxWsServerFactoryBean factory;

    public void setUpWSEndpoint(String PORT) {
        factory = new JaxWsServerFactoryBean();
        factory.setServiceBean(new EchoImpl());
        if(PORT!= null) {
            factory.setAddress("http://localhost:" + PORT + "/Echo");
        } else {
            factory.setAddress("local://Echo");
            factory.setTransportId(LocalTransportFactory.TRANSPORT_ID);
        }
        Server server = factory.create();
        Service service = server.getEndpoint().getService();

        service.getInInterceptors().add(new SAAJInInterceptor());
        service.getInInterceptors().add(new LoggingInInterceptor());
        service.getOutInterceptors().add(new SAAJOutInterceptor());
        service.getOutInterceptors().add(new LoggingOutInterceptor());

        wsIn = new WSS4JInInterceptor();
        wsIn.setProperty(WSHandlerConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        wsIn.setProperty(WSHandlerConstants.DEC_PROP_FILE, "insecurity.properties");
        wsIn.setProperty(WSHandlerConstants.PW_CALLBACK_CLASS, PwdCallback.class.getName());

        service.getInInterceptors().add(wsIn);

        wsOut = new WSS4JOutInterceptor();
        wsOut.setProperty(WSHandlerConstants.SIG_PROP_FILE, "outsecurity.properties");
        wsOut.setProperty(WSHandlerConstants.ENC_PROP_FILE, "outsecurity.properties");
        service.getOutInterceptors().add(wsOut);
    }

    public void setupClient() {
        JaxWsProxyFactoryBean proxyFac = new JaxWsProxyFactoryBean();
        proxyFac.setServiceClass(Echo.class);
        proxyFac.setAddress("local://Echo");
        proxyFac.getClientFactoryBean().setTransportId(LocalTransportFactory.TRANSPORT_ID);

        echo = (Echo) proxyFac.create();

        client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getInInterceptors().add(wsIn);
        client.getInInterceptors().add(new SAAJInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());
        client.getOutInterceptors().add(wsOut);
        client.getOutInterceptors().add(new SAAJOutInterceptor());
    }

    @Before public void setUpService() throws Exception {
        // Create the Web Service endpoint exposing the Echo Service locally without HTTP Transport
        setUpWSEndpoint(null);

        // Create the client
        setupClient();
    }

    /**
     * Create a SOAP Message where the SOAP Header includes a wsse section with a username and timestamp to authenticate the JAXWS Client
     * Use a wrong password
     */
    @Test public void testUsernameTokenWrongPassword() throws Exception {
        String actions = WSHandlerConstants.TIMESTAMP + " " + WSHandlerConstants.USERNAME_TOKEN;

        // Server Side
        wsIn.setProperty(WSHandlerConstants.ACTION, actions);

        // Client Side
        wsOut.setProperty(WSHandlerConstants.ACTION, actions);
        wsOut.setProperty(WSHandlerConstants.USER, "cmoulliard");
        wsOut.setProperty("password", "password");

        try {
            assertEquals("test", echo.echo("test"));
            fail("Exception expected");
        } catch(Exception ex) {
            assertEquals("Security processing failed.", ex.getMessage());
        }
    }

    /**
     * Create a SOAP Message where the SOAP Header includes a wsse section with a username and timestamp to authenticate the JAXWS Client
     */
    @Test public void testUsernameToken() throws Exception {
        String actions = WSHandlerConstants.TIMESTAMP + " " + WSHandlerConstants.USERNAME_TOKEN;

        // Server Side
        wsIn.setProperty(WSHandlerConstants.ACTION, actions);

        // Client Side
        wsOut.setProperty(WSHandlerConstants.ACTION, actions);
        wsOut.setProperty(WSHandlerConstants.USER, "cmoulliard");
        wsOut.setProperty(WSHandlerConstants.PW_CALLBACK_CLASS, PwdCallback.class.getName());

        assertEquals("test", echo.echo("test"));
    }

    /**
     * Create a SOAP Message where the SOAP Header includes a wsse section to sign the message generated by the JAXWS Client
     * The Algorithm to be used to sign the body of the message is SHA256 while the Digest Algo should be RSA SHA1
     * The parts to be signed are the Body and the TimeStamp
     */
    @Test public void testSignature() throws Exception {
        String actions = WSHandlerConstants.TIMESTAMP + " " + WSHandlerConstants.SIGNATURE;
        String BODY_PART = "{}{" + WSSConstants.NS_SOAP11 + "}Body;";
        String TIMESTAMP_PART = "{}{" + WSSConstants.NS_WSU10 + "}Timestamp;";

        // Server Side
        wsIn.setProperty(WSHandlerConstants.ACTION, actions);
        wsIn.setProperty(WSHandlerConstants.SIG_DIGEST_ALGO, WSConstants.SHA256);
        wsIn.setProperty(WSHandlerConstants.SIG_ALGO, WSConstants.RSA_SHA1);

        // Client Side
        wsOut.setProperty(WSHandlerConstants.ACTION, actions);
        wsOut.setProperty(WSHandlerConstants.USER, "integration"); // Alias of the user = certificate to be used to sign the Body/Timestamp using the Key
        wsOut.setProperty(WSHandlerConstants.PW_CALLBACK_CLASS, PwdCallback.class.getName());
        wsOut.setProperty(WSHandlerConstants.SIG_DIGEST_ALGO, WSConstants.SHA256);
        wsOut.setProperty(WSHandlerConstants.SIG_ALGO, WSConstants.RSA_SHA1);
        wsOut.setProperty(WSHandlerConstants.SIGNATURE_PARTS, BODY_PART + TIMESTAMP_PART);

        assertEquals("test", echo.echo("test"));
    }

    /**
     *
     * Create a SOAP Message where the SOAP Header includes a wsse section to sign/encrypt the message generated by the JAXWS Client
     */
    @Test public void testEncryptionPlusSig() throws Exception {

        // Server side
        wsIn.setProperty(WSHandlerConstants.ACTION,
                WSHandlerConstants.ENCRYPT + " " + WSHandlerConstants.SIGNATURE);

        // Client Side
        wsOut.setProperty(WSHandlerConstants.USER, "integration");
        wsOut.setProperty(WSHandlerConstants.PW_CALLBACK_CLASS, PwdCallback.class.getName());
        wsOut.setProperty(WSHandlerConstants.ACTION,
                WSHandlerConstants.ENCRYPT + " " + WSHandlerConstants.SIGNATURE);

        assertEquals("test", echo.echo("test"));
    }

}
