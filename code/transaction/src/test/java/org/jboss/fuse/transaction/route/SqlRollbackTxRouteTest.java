package org.jboss.fuse.transaction.route;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.sql.SqlComponent;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.spi.TransactedPolicy;
import org.apache.camel.spring.spi.SpringTransactionPolicy;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.After;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

public class SqlRollbackTxRouteTest extends CamelTestSupport {

    private EmbeddedDatabase db;
    private JdbcTemplate jdbcTemplate;

    @Override
    protected JndiRegistry createRegistry() throws Exception {
        JndiRegistry reg = super.createRegistry();
        // EXCLUDE-BEGIN
        db = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.DERBY)
                .addScript("sql/createAndPopulateDatabase.sql").build();
        reg.bind("testdb", db);

        jdbcTemplate = new JdbcTemplate(db);

        DataSourceTransactionManager dtm = new DataSourceTransactionManager(db);
        dtm.setDataSource(db);
        reg.bind("txManager", dtm);

        TransactedPolicy txPolicy = new SpringTransactionPolicy(dtm);
        reg.bind("txPolicy",txPolicy);

        SqlComponent sql = new SqlComponent();
        sql.setDataSource(db);
        reg.bind("sql", sql);

        return reg;
        // EXCLUDE-END
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        db.shutdown();
    }

    @Test
    public void testProduceWithRollback() throws Exception {
        // EXCLUDE-BEGIN
        MockEndpoint mock = getMockEndpoint("mock:delete");
        mock.expectedMessageCount(0);

        try {
            template.sendBodyAndHeader("direct:rollback", null, "id", "1");
            fail("forced Exception");
        } catch (Exception e) {
            assertMockEndpointsSatisfied();
            assertEquals("Should Not have deleted record for id 1", new Integer(3),
                    jdbcTemplate.queryForObject("select count(*) from projects", Integer.class));
        }
        // EXCLUDE-END
    }

    @Override protected RouteBuilder createRouteBuilder() throws Exception {
        // EXCLUDE-BEGIN
        return new RouteBuilder() {
            @Override public void configure() throws Exception {

                from("direct:rollback")
                    .transacted("txPolicy")
                    .to("sql:delete from projects where id = :#${header.id}")
                    .process(new Processor() {
                        @Override public void process(Exchange exchange) throws Exception {
                            throw new Exception("forced Exception");
                        }
                    })
                    .to("mock:delete");
            }
        };
        // EXCLUDE-END
    }
}
