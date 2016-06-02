package org.jboss.fuse.transaction.route;

import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class JTATxRouteTest extends CamelSpringTestSupport {

    @Override protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("org/jboss/fuse/transaction/camelContext.xml");
    }

    @Before
    public void cfgSystemProperties() {
        System.setProperty("org.apache.activemq.SERIALIZABLE_PACKAGES","*");
    }

    @Test
    public void testRollbackRecord() throws Exception {
        // EXCLUDE-BEGIN
        MockEndpoint mockError = getMockEndpoint("mock:error");
        mockError.expectedMessageCount(1);

        // Send a CSV Record to insert it within the DB
        try {
            template.sendBody("direct:data-insert-rb",
                    "111,22-04-2016,Claus,Ibsen,incident camel-111,this is a report incident for camel-111,cibsen@gmail.com,+111 10 20 300");
            fail("Exception expected");
        } catch(Exception e) {
            // Record should be rollbacked
            RuntimeCamelException ex = (RuntimeCamelException) e.getCause();
            Assert.assertTrue(ex.getCause().getMessage().contains("###### Sorry, we can't insert your record and place it on the queue !."));
        }

        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMessageCount(1);

        // Perform a Select query to find the record
        template.sendBody("direct:select",null);

        // We expect an exchange with an empty string (= no record found) from the direct-select route
        Assert.assertEquals(true,((String)mock.getExchanges().get(0).getIn().getBody()).isEmpty());

        // We will check that we don't have a message within the queue
        MockEndpoint mockQueue = getMockEndpoint("mock:result-queue");
        mockQueue.expectedMessageCount(0);

        assertMockEndpointsSatisfied();
        // EXCLUDE-END
    }

}
