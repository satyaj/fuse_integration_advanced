package org.jboss.fuse.largefile.performance;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.builder.xml.XPathBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.camel.util.StopWatch;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class StaxDomTest extends CamelTestSupport {

    private int files = 10;
    private int rows = 100000;
    private int total = (files * rows) + files;
    private Runtime runtime;

    private static final Logger LOG = LoggerFactory.getLogger(CamelTestSupport.class);
    private static final long MEGABYTE = 1024L * 1024L;

    public static long bytesToMegabytes(long bytes) {
        return bytes / MEGABYTE;
    }

    @Override
    public void setUp() throws Exception {
        runtime = Runtime.getRuntime();
        runtime.gc();
        super.setUp();
    }

    @Test
    public void testWithXTokenizer() throws Exception {
        // EXCLUDE-BEGIN

        // Calculate the used memory before to start the Camel route
        long memBefore = runtime.totalMemory() - runtime.freeMemory();
        LOG.info("BEFORE - Used memory : " + bytesToMegabytes(memBefore) + " MB");

        NotifyBuilder notify = new NotifyBuilder(context).whenDone(total).create();

        StopWatch watch = new StopWatch();
        context.startRoute("xtokenize");

        // Check that the splitted items have been processed during this period of time
        notify.matches(1, TimeUnit.MINUTES);
        log.info("Took : " + watch.taken() + " millis to process " + files * rows + " records using stream.");

        long memAfter = runtime.totalMemory() - runtime.freeMemory();
        LOG.info("AFTER - Used memory : " + bytesToMegabytes(memAfter) + " MB");
        LOG.info("Difference : " + bytesToMegabytes(memAfter - memBefore) + " MB");

        // EXCLUDE-END
    }

    @Test
    public void testWithXPath() throws Exception {
        // EXCLUDE-BEGIN
        // Calculate the used memory before to start the Camel route
        long memBefore = runtime.totalMemory() - runtime.freeMemory();
        LOG.info("BEFORE - Used memory : " + bytesToMegabytes(memBefore) + " MB");

        NotifyBuilder notify = new NotifyBuilder(context).whenDone(total).create();

        StopWatch watch = new StopWatch();
        context.startRoute("dom");

        // Check that the splitted items have been processed during this period of time
        notify.matches(1, TimeUnit.MINUTES);
        log.info("Took : " + watch.taken() + " millis to process " + files * rows + " records without stream.");

        long memAfter = runtime.totalMemory() - runtime.freeMemory();
        LOG.info("AFTER - Used memory : " + bytesToMegabytes(memAfter) + " MB");
        LOG.info("Difference : " + bytesToMegabytes(memAfter - memBefore) + " MB");
        // EXCLUDE-END
    }

    @Test @Ignore
    public void testDummy() throws Exception {
        // EXCLUDE-BEGIN
        MockEndpoint result = getMockEndpoint("mock:result");
        result.expectedBodiesReceived("<record/>");

        String records = "<records xmlns=\"http:acme.com\">\n"
                + "<record><id>0</id><firstname>Rachel</firstname><lastname>Bride</lastname><email>rachel.bride@acme.com</email><ip>191.147.244.120</ip></record>\n"
                + "<record><id>1</id><firstname>Ken</firstname><lastname>Bailly</lastname><email>ken.bailly@acme.com</email><ip>48.146.242.43</ip></record>\n</records>";

        template.sendBody("direct:test",records);

        assertMockEndpointsSatisfied();
        // EXCLUDE-END
    }

    @Override
    protected RouteBuilder createRouteBuilder() {

        return new RouteBuilder() {

            Namespaces ns = new Namespaces("acme", "http:acme.com");

            public void configure() {
                from("file:target/data?noop=true").id("xtokenize").noAutoStartup()
                    .split().xtokenize("//acme:record",ns).streaming()
                    .log(LoggingLevel.DEBUG,"Record : ${body}");

                // Use DOM and load all XML structure in memory
                from("file:target/data?noop=true").id("dom").noAutoStartup()
                    .convertBodyTo(String.class)
                    .split().xpath("//acme:record",ns)
                        .log(LoggingLevel.DEBUG,"Record : ${body}");

                // Use DOM and load all XML structure in memory
                from("direct:test")
                        .log(LoggingLevel.INFO,"Records : ${body}")
                        .filter()
                          .xpath("//acme:record",ns)
                          .log(LoggingLevel.INFO,"Record : ${body}")
                          .to("mock:result")
                        .end();
            }
        };
    }

}
