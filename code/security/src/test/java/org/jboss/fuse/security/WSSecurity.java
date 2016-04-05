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
import org.apache.wss4j.dom.handler.WSHandlerConstants;

import org.jboss.fuse.security.service.Echo;
import org.jboss.fuse.security.service.EchoImpl;
import org.jboss.fuse.security.service.TestPwdCallback;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WSSecurity {

    private WSS4JInInterceptor wsIn;
    private WSS4JOutInterceptor wsOut;
    private Echo echo;
    private Client client;

    @Before
    public void setUpService() throws Exception {
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setServiceBean(new EchoImpl());
        factory.setAddress("local://Echo");
        factory.setTransportId(LocalTransportFactory.TRANSPORT_ID);
        Server server = factory.create();
        Service service = server.getEndpoint().getService();

        service.getInInterceptors().add(new SAAJInInterceptor());
        service.getInInterceptors().add(new LoggingInInterceptor());
        service.getOutInterceptors().add(new SAAJOutInterceptor());
        service.getOutInterceptors().add(new LoggingOutInterceptor());

        wsIn = new WSS4JInInterceptor();
        wsIn.setProperty(WSHandlerConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        wsIn.setProperty(WSHandlerConstants.DEC_PROP_FILE, "insecurity.properties");
        wsIn.setProperty(WSHandlerConstants.PW_CALLBACK_CLASS, TestPwdCallback.class.getName());

        service.getInInterceptors().add(wsIn);

        wsOut = new WSS4JOutInterceptor();
        wsOut.setProperty(WSHandlerConstants.SIG_PROP_FILE, "outsecurity.properties");
        wsOut.setProperty(WSHandlerConstants.ENC_PROP_FILE, "outsecurity.properties");
        wsOut.setProperty(WSHandlerConstants.USER, "myalias");
        wsOut.setProperty("password", "myAliasPassword");
        wsOut.setProperty(WSHandlerConstants.PW_CALLBACK_CLASS, TestPwdCallback.class.getName());
        service.getOutInterceptors().add(wsOut);

        // Create the client
        JaxWsProxyFactoryBean proxyFac = new JaxWsProxyFactoryBean();
        proxyFac.setServiceClass(Echo.class);
        proxyFac.setAddress("local://Echo");
        proxyFac.getClientFactoryBean().setTransportId(LocalTransportFactory.TRANSPORT_ID);

        echo = (Echo)proxyFac.create();

        client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getInInterceptors().add(wsIn);
        client.getInInterceptors().add(new SAAJInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());
        client.getOutInterceptors().add(wsOut);
        client.getOutInterceptors().add(new SAAJOutInterceptor());
    }

    @Test
    public void testUsernameToken() throws Exception {
        String actions = WSHandlerConstants.ENCRYPT + " " + WSHandlerConstants.SIGNATURE + " "
                + WSHandlerConstants.TIMESTAMP + " " + WSHandlerConstants.USERNAME_TOKEN;

        wsIn.setProperty(WSHandlerConstants.ACTION, actions);
        wsOut.setProperty(WSHandlerConstants.ACTION, actions);

        assertEquals("test", echo.echo("test"));
    }

}