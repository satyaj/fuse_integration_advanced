package org.jboss.fuse.transaction.client;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.jboss.fuse.transaction.model.Project;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class JpaTxRollbackTest extends CamelTestSupport {

    protected ApplicationContext applicationContext;
    protected TransactionTemplate transactionTemplate;
    protected EntityManager entityManager;
    private static AtomicInteger amq = new AtomicInteger();
    private static AtomicInteger linux = new AtomicInteger();
    private static AtomicInteger kaboom = new AtomicInteger();

    protected static final String SELECT_ALL_STRING = "select x from " + Project.class.getName() + " x";

    @Before public void setUp() throws Exception {
        super.setUp();
        EntityManagerFactory entityManagerFactory = applicationContext
                .getBean("entityManagerFactory", EntityManagerFactory.class);
        transactionTemplate = applicationContext.getBean("transactionTemplate", TransactionTemplate.class);
        entityManager = entityManagerFactory.createEntityManager();
        cleanupRepository();
    }

    @After public void tearDown() throws Exception {
        super.tearDown();
        if (entityManager != null) {
            entityManager.close();
        }
    }

    protected void cleanupRepository() {
        transactionTemplate.execute(new TransactionCallback<Object>() {
            public Object doInTransaction(TransactionStatus arg0) {
                entityManager.joinTransaction();
                List<?> list = entityManager.createQuery(selectAllString()).getResultList();
                for (Object item : list) {
                    entityManager.remove(item);
                }
                entityManager.flush();
                return Boolean.TRUE;
            }
        });
    }

    @Override protected CamelContext createCamelContext() throws Exception {
        applicationContext = new org.springframework.context.support.ClassPathXmlApplicationContext(
                "org/jboss/fuse/transaction/springJpaRouteTest.xml");
        return SpringCamelContext.springCamelContext(applicationContext);
    }

    @Test public void testDelete() throws Exception {
        // EXCLUDE-BEGIN
        // First create three records
        template.sendBody("jpa://" + Project.class.getName(), new Project(2, "AMQ", "ASF"));
        template.sendBody("jpa://" + Project.class.getName(), new Project(3, "Linux", "XXX"));
        template.sendBody("jpa://" + Project.class.getName(), new Project(4, "Karaf", "XXX"));
        template.sendBody("jpa://" + Project.class.getName(), new Project(1, "Camel", "ASF"));

        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMessageCount(0);

        MockEndpoint error = getMockEndpoint("mock:error");
        error.expectedMessageCount(1);

        //context.startRoute("foo");

        assertMockEndpointsSatisfied();
        Project camel = (Project)error.getExchanges().get(0).getIn().getBody();
        Assert.assertEquals("Camel",camel.getProject());

        assertEntityInDB(3);
        // EXCLUDE-BEGIN
    }

    @Override protected RouteBuilder createRouteBuilder() throws Exception {
        // EXCLUDE-BEGIN
        return new RouteBuilder() {

            @Override public void configure() throws Exception {

                onException(IllegalArgumentException.class).handled(false).to("mock:error");

                from("jpa://" + Project.class.getName() + "?consumer.transacted=false&delay=1000").routeId("foo")
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
            }
        };
        // EXCLUDE-END
    }

    protected String getEndpointUri() {
        return "jpa://" + Project.class.getName() + "?consumer.transacted=true&delay=1000";
    }

    protected void assertEntityInDB(int size) throws Exception {
        List<?> list = entityManager.createQuery(selectAllString()).getResultList();
        assertEquals(size, list.size());
        assertIsInstanceOf(Project.class, list.get(0));
    }

    protected String selectAllString() {
        return SELECT_ALL_STRING;
    }
}
