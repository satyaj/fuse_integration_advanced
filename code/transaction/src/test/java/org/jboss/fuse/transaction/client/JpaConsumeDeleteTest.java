package org.jboss.fuse.transaction.client;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.jboss.fuse.transaction.model.Project;
import org.junit.*;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class JpaConsumeDeleteTest extends CamelTestSupport {

    protected ApplicationContext applicationContext;
    protected TransactionTemplate transactionTemplate;
    protected EntityManager entityManager;

    protected static final String SELECT_ALL_STRING = "select x from " + Project.class.getName() + " x";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        EntityManagerFactory entityManagerFactory = applicationContext
                .getBean("entityManagerFactory", EntityManagerFactory.class);
        transactionTemplate = applicationContext.getBean("transactionTemplate", TransactionTemplate.class);
        entityManager = entityManagerFactory.createEntityManager();
        cleanupRepository();
    }

    @After
    public void tearDown() throws Exception {
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

    @Override
    protected CamelContext createCamelContext() throws Exception {
        applicationContext = new org.springframework.context.support.ClassPathXmlApplicationContext(
                "org/jboss/fuse/transaction/springJpaRouteTest.xml");
        return SpringCamelContext.springCamelContext(applicationContext);
    }

    @Test
    public void testConsumeNoDelete() throws Exception {
        // EXCLUDE-BEGIN
        // First create 4 records
        template.sendBody("direct:insert",new Project(1, "AMQ", "ASF"));
        template.sendBody("direct:insert",new Project(2, "Linux", "XXX"));
        template.sendBody("direct:insert",new Project(3, "Karaf", "YYY"));
        template.sendBody("direct:insert",new Project(4, "Camel", "ASF"));

        MockEndpoint insert = getMockEndpoint("mock:insert");
        insert.expectedMessageCount(4);
        MockEndpoint result = getMockEndpoint("mock:result");
        insert.expectedMessageCount(4);

        // Start Jpa route
        context.startRoute("jpa-nodelete");

        //assertEntityInDB(0);
        assertMockEndpointsSatisfied();

        Thread.sleep(2000);
        assertEntityInDB(4);
        // EXCLUDE-BEGIN
    }

    @Test
    public void testConsumeDelete() throws Exception {
        // EXCLUDE-BEGIN
        // First create 4 records
        template.sendBody("direct:insert",new Project(1, "AMQ", "ASF"));
        template.sendBody("direct:insert",new Project(2, "Linux", "XXX"));
        template.sendBody("direct:insert",new Project(3, "Karaf", "YYY"));
        template.sendBody("direct:insert",new Project(4, "Camel", "ASF"));

        MockEndpoint insert = getMockEndpoint("mock:insert");
        insert.expectedMessageCount(4);
        MockEndpoint result = getMockEndpoint("mock:result");
        insert.expectedMessageCount(4);

        // Start Jpa route
        context.startRoute("jpa-delete");

        //assertEntityInDB(0);
        assertMockEndpointsSatisfied();

        Thread.sleep(2000);
        assertEntityInDB(0);
        // EXCLUDE-BEGIN
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        // EXCLUDE-BEGIN
        return new RouteBuilder() {

            @Override public void configure() throws Exception {

                from("direct:insert")
                  .to("jpa://" + Project.class.getName())
                  .log("### Processed Project: ${body.project}, ID: ${body.id}")
                  .to("mock:insert");

                from("jpa://" + Project.class.getName() + "?consumer.delay=1000&consumeDelete=false").routeId("jpa-nodelete").noAutoStartup()
                  .log("### Processed Project: ${body.project}, ID: ${body.id}")
                  .to("mock:result");

                from("jpa://" + Project.class.getName() + "?consumer.delay=1000&consumeDelete=true").routeId("jpa-delete").noAutoStartup()
                        .log("### Processed Project: ${body.project}, ID: ${body.id}")
                        .to("mock:result");
            }
        };
        // EXCLUDE-END
    }

    protected void assertEntityInDB(int size) throws Exception {
        List<?> list = entityManager.createQuery(selectAllString()).getResultList();
        assertEquals(size, list.size());
        if(!list.isEmpty()) {
            assertIsInstanceOf(Project.class, list.get(0));
        }
    }

    protected String selectAllString() {
        return SELECT_ALL_STRING;
    }
}
