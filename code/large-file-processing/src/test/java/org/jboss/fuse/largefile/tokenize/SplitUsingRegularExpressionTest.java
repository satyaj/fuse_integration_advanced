package org.jboss.fuse.largefile.tokenize;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import java.util.List;

public class SplitUsingRegularExpressionTest extends CamelTestSupport {

    @EndpointInject(uri="mock:result")
    MockEndpoint result;

    @Test
    public void testRegExpression() throws Exception {

        // EXCLUDE-BEGIN
        String record = "Charles jeff bernard   Nandan Satya";

        template.sendBody("direct:start-regexp", record);

        result.expectedMessageCount(5);

        result.assertIsSatisfied();
        // EXCLUDE-END
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        // EXCLUDE-BEGIN
        return new RouteBuilder() {
            public void configure() {
                from("direct:start-regexp").split(body().tokenize("(\\W+)\\s*")).to("mock:result");
            }
        };
        // EXCLUDE-END
    }
}