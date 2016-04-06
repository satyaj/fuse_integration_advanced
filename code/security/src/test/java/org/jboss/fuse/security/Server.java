package org.jboss.fuse.security;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

import java.net.URL;

public class Server extends AbstractBusTestServerBase {

    public Server() { }

    protected void run() {
        URL busFile = Server.class.getResource("/server.xml");
        Bus busLocal = new SpringBusFactory().createBus(busFile);
        BusFactory.setDefaultBus(busLocal);
        setBus(busLocal);

        try {
            new Server();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}