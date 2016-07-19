package org.jboss.fuse.largefile.tokenize;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.builder.xml.XPathBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

public class SplitXMLTest extends CamelTestSupport {

    @EndpointInject(uri="mock:result")
    MockEndpoint result;

    @Test
    public void testXMLTokenize() throws Exception {
        // EXCLUDE-BEGIN
        result.expectedMessageCount(10);

        template.sendBody("direct:start-xml", createBody());

        result.assertIsSatisfied();
        // EXCLUDE-END
    }


    @Test
    public void testXMLTokenizeAndGroup() throws Exception {
        // EXCLUDE-BEGIN
        result.expectedMessageCount(2);

        template.sendBody("direct:start-xml-group", createBody());

        result.assertIsSatisfied();
        // EXCLUDE-END
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        // EXCLUDE-BEGIN

        return new RouteBuilder() {

            public void configure() {
                from("direct:start-xml").split().tokenizeXML("order").to("mock:result");
                from("direct:start-xml-group").split().tokenizeXML("order",5).to("mock:result");
            }
        };
        // EXCLUDE-END
    }

    protected String createBody() {
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\"?>\n");
        sb.append("<orders>\n");
        sb.append("  <order id=\"1\">Camel in Action</order>\n");
        sb.append("  <order id=\"2\">ActiveMQ in Action</order>\n");
        sb.append("  <order id=\"3\">DSL in Action</order>\n");
        sb.append("  <order id=\"4\">Vert.x in Action</order>\n");
        sb.append("  <order id=\"5\">Hibernate in Action</order>\n");
        sb.append("  <order id=\"6\">Karaf in Action</order>\n");
        sb.append("  <order id=\"7\">Wildfly in Action</order>\n");
        sb.append("  <order id=\"8\">Microservices in Action</order>\n");
        sb.append("  <order id=\"9\">Drools in Action</order>\n");
        sb.append("  <order id=\"10\">JForge in Action</order>\n");
        sb.append("</orders>");
        return sb.toString();
    }

}
