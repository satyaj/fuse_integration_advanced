package org.jboss.fuse.transaction.client;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.SpringCamelContext;
import org.jboss.fuse.transaction.model.Project;
import org.junit.Test;

public class JpaTxRollbackTest extends AbstractJpaTest {

    @Override
    protected CamelContext createCamelContext() throws Exception {
        applicationContext = new org.springframework.context.support.ClassPathXmlApplicationContext(
                "org/jboss/fuse/transaction/springJpaRouteTest.xml");
        return SpringCamelContext.springCamelContext(applicationContext);
    }

    @Test
    public void testRollBack() throws Exception {
        // EXCLUDE-BEGIN
        // First create 4 records
        template.sendBody("jpa://" + Project.class.getName(),new Project(1, "AMQ", "ASF"));
        template.sendBody("jpa://" + Project.class.getName(),new Project(2, "Linux", "XXX"));
        template.sendBody("jpa://" + Project.class.getName(),new Project(3, "Karaf", "YYY"));
        template.sendBody("jpa://" + Project.class.getName(),new Project(4, "Camel", "ASF"));

        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMessageCount(3);

        // start route
        context.startRoute("jpa");

        assertMockEndpointsSatisfied();

        // Wait till Camel Route ends
        Thread.sleep(2000);
        assertEntityInDB(1);
        // EXCLUDE-BEGIN
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        // EXCLUDE-BEGIN
        return new RouteBuilder() {

            @Override public void configure() throws Exception {

                from("jpa://" + Project.class.getName() + "?consumer.transacted=false&consumer.delay=1000&consumeDelete=true").routeId("jpa").noAutoStartup()
                 .process(new Processor() {
                           @Override
                           public void process(Exchange exchange) throws Exception {
                               Project project = exchange.getIn().getBody(Project.class);
                               if ("Camel".equals(project.getProject())) {
                                  throw new IllegalArgumentException("Camel Forced");
                               }
                           }
                  })
                 .log("### Processed Project: ${body.project}, ID: ${body.id}")
                 .to("mock:result");

                from("direct:insert")
                    .to("jpa://" + Project.class.getName())
                    .log("### Processed Project: ${body.project}, ID: ${body.id}")
                    .to("mock:result");
            }
        };
        // EXCLUDE-END
    }
}
