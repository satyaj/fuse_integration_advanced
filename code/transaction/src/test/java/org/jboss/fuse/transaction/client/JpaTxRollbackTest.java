package org.jboss.fuse.transaction.client;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jpa.JpaComponent;
import org.apache.camel.component.jpa.JpaEndpoint;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.jboss.fuse.transaction.model.Project;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import java.util.List;

public class JpaTxRollbackTest extends CamelTestSupport {

    protected ApplicationContext applicationContext;
    protected TransactionTemplate transactionTemplate;
    protected EntityManager entityManager;

    protected static final String SELECT_ALL_STRING = "select x from " + Project.class.getName() + " x";

/*
    @Override
    protected JndiRegistry createRegistry() throws Exception {
        JndiRegistry reg = super.createRegistry();

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("project");
        // reg.bind("emf",emf);

        JpaTransactionManager jpaTx = new JpaTransactionManager(emf);
        jpaTx.afterPropertiesSet();

        TransactionTemplate tt = new TransactionTemplate();
        tt.setTransactionManager(jpaTx);

        JpaComponent comp = new JpaComponent();
        comp.setTransactionManager(jpaTx);
        reg.bind("jpa",comp);
        return reg;
    }
*/

    @Before
    public void setUp() throws Exception {
        super.setUp();
        EntityManagerFactory entityManagerFactory = applicationContext.getBean("entityManagerFactory",
                EntityManagerFactory.class);
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
        applicationContext = new org.springframework.context.support.ClassPathXmlApplicationContext("org/jboss/fuse/transaction/springJpaRouteTest.xml");
        return SpringCamelContext.springCamelContext(applicationContext);
    }

    @Test
    public void testDelete() throws Exception {
        // EXCLUDE-BEGIN
/*
        em = ((JpaEndpoint)context().getEndpoint(getEndpointUri())).getEntityManagerFactory().createEntityManager();
        et = em.getTransaction();

        et.begin();
        em.createNativeQuery("insert into PROJECT values(1, 'Camel', 'ASF')");
        em.createNativeQuery("insert into PROJECT values(2, 'AMQ', 'ASF')");
        em.createNativeQuery("insert into PROJECT values(3, 'Linux', 'XXX')");
        et.commit();*/

        // First create three records
        template.sendBody("jpa://" + Project.class.getName(), new Project(1, "Camel", "ASF"));
        template.sendBody("jpa://" + Project.class.getName(), new Project(2, "AMQ", "ASF"));
        template.sendBody("jpa://" + Project.class.getName(), new Project(3, "Linux", "XXX"));
        assertEntityInDB(3);

        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMessageCount(0);

        //MockEndpoint error = getMockEndpoint("mock:error");
        //error.expectedMessageCount(1);

        context.startRoute("foo");
        assertMockEndpointsSatisfied();

/*        HashMap<String,Object> headers = new HashMap<String,Object>();
        headers.put("id",2);
        headers.put(SqlConstants.SQL_QUERY,"delete from projects where id = :?id");
        template.sendBodyAndHeaders("direct:delete",null,headers);*/

        //assertEquals("Should have 2 rows", new Integer(2), jdbcTemplate.queryForObject("select count(*) from project", Integer.class));
        // EXCLUDE-BEGIN
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        // EXCLUDE-BEGIN
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {

                onException(Exception.class)
                        .handled(true).to("mock:error").rollback("%%%%% ****");

                  from("jpa://" + Project.class.getName() + "?consumer.transacted=true&delay=1000").routeId("foo").noAutoStartup()
/*                  .process(new Processor() {
                      @Override
                      public void process(Exchange exchange) throws Exception {
                          throw new Exception("forced Exception");
                      }
                  })*/
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
