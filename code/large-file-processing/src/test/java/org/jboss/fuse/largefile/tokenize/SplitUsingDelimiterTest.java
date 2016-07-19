package org.jboss.fuse.largefile.tokenize;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class SplitUsingDelimiterTest extends CamelTestSupport {

    @EndpointInject(uri="mock:result")
    MockEndpoint result;

    @Test
    public void testUsingCarriageReturn() throws Exception {

        // EXCLUDE-BEGIN
        String CR = System.getProperty("line.separator");
        StringBuilder builder = new StringBuilder();
        builder.append("Bob").append(CR);
        builder.append("James").append(CR);
        builder.append("Charles").append(CR);
        builder.append("Jeff").append(CR);
        builder.append("Chad").append(CR);
        String records = builder.toString();

        template.sendBody("direct:start-cr", records);

        result.expectedMessageCount(5);
        List<Exchange> exchanges = result.getExchanges();
        assertEquals(Long.valueOf(5),Long.valueOf(exchanges.get(4).getProperty("CamelSplitSize",String.class)));
        assertEquals(true,Boolean.valueOf(exchanges.get(4).getProperty("CamelSplitComplete",String.class)));

        result.assertIsSatisfied();
        // EXCLUDE-END
    }

    @Test
    public void testUsingTokenPairs() throws Exception {
        // EXCLUDE-BEGIN
        char PREFIX_TOKEN = '[';
        char SUFFIX_TOKEN = ']';
        StringBuilder builder = new StringBuilder();
        builder.append(PREFIX_TOKEN).append("Bob").append(SUFFIX_TOKEN);
        builder.append(PREFIX_TOKEN).append("James").append(SUFFIX_TOKEN);
        builder.append(PREFIX_TOKEN).append("Charles").append(SUFFIX_TOKEN);
        builder.append(PREFIX_TOKEN).append("Jeff").append(SUFFIX_TOKEN);
        builder.append(PREFIX_TOKEN).append("Chad").append(SUFFIX_TOKEN);
        String record = builder.toString();

        template.sendBody("direct:start-tokens", record);

        result.expectedMessageCount(5);
        List<Exchange> exchanges = result.getExchanges();
        assertEquals(Long.valueOf(5),Long.valueOf(exchanges.get(4).getProperty("CamelSplitSize",String.class)));
        assertEquals(true,Boolean.valueOf(exchanges.get(4).getProperty("CamelSplitComplete",String.class)));

        result.assertIsSatisfied();
        // EXCLUDE-END
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        // EXCLUDE-BEGIN
        return new RouteBuilder() {
            public void configure() {
                from("direct:start-cr").split(body().tokenize("\n")).to("mock:result");
                from("direct:start-tokens").split(body().tokenizePair("[","]",false)).to("mock:result");
            }
        };
        // EXCLUDE-END
    }
}