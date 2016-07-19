package org.jboss.fuse.largefile.tokenize;

import org.apache.camel.EndpointInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class SplitJavaCollectionTest extends CamelTestSupport {

    @EndpointInject(uri="mock:result")
    MockEndpoint result;

    @Test
    public void testArrayOfString() throws Exception {
        // EXCLUDE-BEGIN
        String[] names = {"Bob","James","Charles","Chad","Jeff"};

        result.expectedMessageCount(5);

        template.sendBody("direct:start-body", names);

        result.assertIsSatisfied();
        // EXCLUDE-END
    }

    @Test
    public void testArrayOfPrimitive() throws Exception {
        // EXCLUDE-BEGIN
        char[] chars = {'c','h','a','r','l','e','s'};

        result.expectedMessageCount(7);

        template.sendBody("direct:start-body", chars);

        result.assertIsSatisfied();
        // EXCLUDE-END
    }

    @Test
    public void testCollectionAndHeaderExpression() throws Exception {
        // EXCLUDE-BEGIN
        List<String> names = new ArrayList<String>(5);
        names.add("Bob");
        names.add("James");
        names.add("Charles");
        names.add("Jeff");
        names.add("Chad");

        result.expectedMessageCount(5);

        template.sendBodyAndHeader("direct:start-header",null,"foo",names);

        result.assertIsSatisfied();
        // EXCLUDE-END
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        // EXCLUDE-BEGIN
        return new RouteBuilder() {
            public void configure() {
                from("direct:start-body").split().body().to("mock:result");
                from("direct:start-header").split().header("foo").to("mock:result");
            }
        };
        // EXCLUDE-END
    }
}