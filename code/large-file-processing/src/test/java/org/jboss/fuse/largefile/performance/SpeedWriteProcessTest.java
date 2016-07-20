package org.jboss.fuse.largefile.performance;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.camel.util.StopWatch;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class SpeedWriteProcessTest extends CamelTestSupport {

    private static final Logger LOG = LoggerFactory.getLogger(CamelTestSupport.class);

    private int files = 10;
    private int rows = 50000;
    private int total = (files * rows) + files;

    @Override
    public void setUp() throws Exception {
        deleteDirectory("target/data/out");
        super.setUp();
    }

    @Test
    public void testWithoutAggregation() throws Exception {
        // EXCLUDE-BEGIN
        NotifyBuilder notify = new NotifyBuilder(context).whenDone(total).create();

        // Start the route
        StopWatch watch = new StopWatch();
        context.startRoute("noaggregate");

        // Check that the splitted items have been processed during this period of time
        notify.matches(2, TimeUnit.MINUTES);
        log.info("Took : " + watch.taken() + " millis to process " + files * rows + " records with stream.");
        // EXCLUDE-END
    }

    @Test
    public void testWithAggregation() throws Exception {
        // EXCLUDE-BEGIN
        NotifyBuilder notify = new NotifyBuilder(context).whenDone(total).create();

        // Start the route
        StopWatch watch = new StopWatch();
        context.startRoute("aggregate");

        // Check that the splitted items have been processed during this period of time
        notify.matches(20, TimeUnit.SECONDS);
        log.info("Took : " + watch.taken() + " millis to process " + files * rows + " records with stream.");
        // EXCLUDE-END
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        // EXCLUDE-BEGIN
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {

                from("file://target/data?noop=true").id("aggregate").noAutoStartup()
                    .split().tokenize("\n").streaming().aggregate(header(Exchange.FILE_NAME_ONLY),new MyAggregationStrategy()).completionSize(10000)
                    .log(LoggingLevel.DEBUG,"Record : ${body}")
                    .to("file://target/data/out?fileExist=Append&fileName=bigfile.txt");

                from("file://target/data?noop=true").id("noaggregate").noAutoStartup()
                        .split().tokenize("\n").streaming()
                        .log(LoggingLevel.DEBUG,"Record : ${body}")
                        .to("file://target/data/out?fileExist=Append&fileName=bigfile.txt");
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
                    .append("\\n")
                    .append(newExchange.getIn().getBody(String.class));

            if (Boolean.valueOf(newExchange.getProperty("CamelSplitComplete",Boolean.class))) {
                oldExchange.getIn().getBody(StringBuilder.class).append("\\n");
            }

            return oldExchange;
        }
        // EXCLUDE-END
    }

}

