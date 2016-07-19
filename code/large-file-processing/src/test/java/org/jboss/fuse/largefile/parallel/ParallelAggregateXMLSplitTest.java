package org.jboss.fuse.largefile.parallel;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.jboss.fuse.largefile.Utility;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ParallelAggregateXMLSplitTest extends CamelTestSupport {

    @EndpointInject(uri = "mock:result")
    MockEndpoint result;

    @Override
    public void setUp() throws Exception {
        deleteDirectory("target/split");
        super.setUp();
    }

    @Test
    public void testAggregate() {
        // EXCLUDE-BEGIN
        String response = "a1,b2,c3,d4,e5,f6";
        result.expectedMessageCount(1);
        result.expectedBodiesReceived(response);
        // EXCLUDE-END
    }

    @Test
    public void testParallel() throws Exception {
        // EXCLUDE-BEGIN
        template.sendBody("direct:start-parallel", "a,b,c,d,e,f,g,h,i,j");
        int count = new File("target/split/parallel").list().length;
        assertEquals(10,count);
        // EXCLUDE-END
    }

    @Test
    public void testParallelThreadPool() throws Exception {
        // EXCLUDE-BEGIN
        template.sendBody("direct:start-parallel-threadpool", "a,b,c,d,e,f,g,h,i,j");
        int count = new File("target/split/parallel-threadpool").list().length;
        assertEquals(5,count);
        // EXCLUDE-END
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        // EXCLUDE-BEGIN
        return new RouteBuilder() {

            private ExecutorService myThreadPool;
            private Utility utility = new Utility();

            public void configure() {

                myThreadPool = Executors.newFixedThreadPool(5);

                from("direct:start-aggregate")
                        .split().tokenize(",")
                          .aggregationStrategy(new MyAggregationStrategy())
                          .to("mock:result");

                from("direct:start-parallel")
                        .split(body().tokenize(","))
                          .parallelProcessing()
                          .setProperty("threadNumber").method(utility,"getNumberFromCamelThreadName()")
                          .to("file:target/split/parallel?fileExist=Append&fileName=result-${property.threadNumber}.txt");

                from("direct:start-parallel-threadpool")
                        .split(body().tokenize(","))
                          .parallelProcessing().executorService(myThreadPool)
                          .setProperty("threadNumber").method(utility,"getNumberFromThreadName()")
                          .to("file:target/split/parallel-threadpool?fileExist=Append&fileName=result-${property.threadNumber}.txt");

            }
        };
        // EXCLUDE-END
    }

    private class MyAggregationStrategy implements AggregationStrategy {
        // EXCLUDE-BEGIN

        @Override public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
            if (oldExchange == null) {
                newExchange.getIn().setBody(new StringBuilder(newExchange.getIn().getBody(String.class)));
                return newExchange;
            }
            oldExchange.getIn().getBody(StringBuilder.class)
                    .append(",")
                    .append(newExchange.getIn().getBody(String.class));
            return oldExchange;
        }
        // EXCLUDE-END
    }
}
