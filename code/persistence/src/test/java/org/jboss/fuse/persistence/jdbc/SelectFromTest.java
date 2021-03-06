package org.jboss.fuse.persistence.jdbc;

import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.List;
import java.util.Map;

public class SelectFromTest  extends CamelSpringTestSupport {

    @Test
    public void testSelectFrom() throws Exception {
        // EXCLUDE-BEGIN
        // now we send the exchange to the endpoint, and receives the response from Camel
        List<Map<String, Object>> data = template.requestBody("direct://select", "SELECT * FROM REPORT.T_INCIDENT", List.class);

        // assertions of the response
        assertNotNull(data);
        assertEquals(4, data.size());

        Map<String, Object> row = data.get(1);
        assertEquals("2", row.get("INCIDENT_ID").toString());
        assertEquals("002", row.get("INCIDENT_REF"));
        assertEquals("Jeff", row.get("GIVEN_NAME"));
        assertEquals("Delong", row.get("FAMILY_NAME"));
        assertEquals("jdelong@redhat.com", row.get("EMAIL"));
        // EXCLUDE-END
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("org/jboss/fuse/persistence/sql/camelContext.xml");
    }
}