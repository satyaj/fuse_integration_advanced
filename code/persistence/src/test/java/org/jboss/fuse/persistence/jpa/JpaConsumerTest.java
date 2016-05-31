package org.jboss.fuse.persistence.jpa;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.jboss.fuse.persistence.AbstractJpaTest;
import org.jboss.fuse.persistence.model.SendEmail;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.wildfly.extension.camel.CamelAware;

public class JpaConsumerTest extends CamelSpringTestSupport {

    @EndpointInject(uri = "mock:result")
    MockEndpoint mock;

    protected static final String SELECT_ALL_STRING = "select x from " + SendEmail.class.getName() + " x";

    @Test
    public void testInsertAndReceive() throws Exception {
        // EXCLUDE-BEGIN
        mock.expectedMessageCount(3);
        mock.expectedPropertyReceived(Exchange.BATCH_SIZE, 3);

        template.sendBody("direct:start", new SendEmail("alpha"));
        template.sendBody("direct:start", new SendEmail("beta"));
        template.sendBody("direct:start", new SendEmail("dummy"));

        assertMockEndpointsSatisfied();

        SendEmail email = mock.getReceivedExchanges().get(2).getIn().getBody(SendEmail.class);
        assertEquals("dummy@somewhere.org", email.getAddress());
        // EXCLUDE-END
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("org/jboss/fuse/persistence/jpa/springJpaRouteTest.xml");
    }
}