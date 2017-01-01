package org.jboss.fuse.bam.wiretap;

import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.junit.Test;
import org.apache.camel.EndpointInject;
import org.apache.camel.component.mock.MockEndpoint;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.List;
import java.util.Map;

public class WiretapTest extends CamelSpringTestSupport {

    @EndpointInject(uri = "mock:wiretap-output") private MockEndpoint wiretapEndpoint;

    @Test
    public void testWiretapWithCopy() throws Exception {
        // EXCLUDE-BEGIN
        wiretapEndpoint.expectedMessageCount(1);
        wiretapEndpoint.expectedBodiesReceived("my test message to be tapped");

        // send an Exchange to the wiretap with copy route
	template.sendBody("direct://wiretap-copy", "my test message to be tapped");

        // verify results
        wiretapEndpoint.assertIsSatisfied();
        // assertions of the response
	/*
        assertNotNull(data);
        assertEquals(4, data.size());

        Map<String, Object> row = data.get(1);
        assertEquals("2", row.get("INCIDENT_ID").toString());
        assertEquals("002", row.get("INCIDENT_REF"));
        assertEquals("Jeff", row.get("GIVEN_NAME"));
        assertEquals("Delong", row.get("FAMILY_NAME"));
        assertEquals("jdelong@redhat.com", row.get("EMAIL"));
	*/
        // EXCLUDE-END
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("org/jboss/fuse/bam/wiretap/camelContext.xml");
    }
}