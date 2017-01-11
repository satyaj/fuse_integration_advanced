package org.jboss.fuse.bam.intercept;

import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.apache.camel.EndpointInject;
import org.apache.camel.component.mock.MockEndpoint;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.JndiRegistry;

public class InterceptTest extends CamelTestSupport {
    
    private static final String BODY1 = "You say Goodbye";
    private static final String BODY = "I say Hello";

    @Test
    public void testInterceptPred() throws Exception {
        
        MockEndpoint resultEndpoint = getMockEndpoint("mock:result");
        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.message(0).body().equals(BODY);

        MockEndpoint interceptEndpoint = getMockEndpoint("mock:intercept");
        interceptEndpoint.expectedMessageCount(1);
        interceptEndpoint.message(0).body().equals(BODY);
        
        template.sendBody("direct:start", BODY);
                
        assertMockEndpointsSatisfied();        
    }

    @Test
    public void testInterceptPredNoop() throws Exception {      
        
        MockEndpoint resultEndpoint = getMockEndpoint("mock:result");
        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.message(0).body().equals(BODY);

        MockEndpoint interceptEndpoint = getMockEndpoint("mock:intercept");
        interceptEndpoint.expectedMessageCount(0);
        
        template.sendBody("direct:start", BODY1);
                
        assertMockEndpointsSatisfied();        
    }    
    
    @Test
    public void testInterceptFromPred() throws Exception {      
        
        MockEndpoint resultEndpoint = getMockEndpoint("mock:result");
        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.message(0).body().equals(BODY);

        MockEndpoint interceptEndpoint = getMockEndpoint("mock:intercepted");
        interceptEndpoint.expectedMessageCount(1);
        
        template.sendBodyAndHeader("direct:start", BODY, "usertype", "test");
                
        assertMockEndpointsSatisfied();        
    }

    @Test
    public void testInterceptFromPredNoop() throws Exception {      
        
        MockEndpoint resultEndpoint = getMockEndpoint("mock:result");
        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.message(0).body().equals(BODY);

        MockEndpoint interceptEndpoint = getMockEndpoint("mock:intercepted");
        interceptEndpoint.expectedMessageCount(0);
        
        template.sendBodyAndHeader("direct:start1", BODY, "usertype", "test");
                
        assertMockEndpointsSatisfied();        
    }    

    @Override
    protected JndiRegistry createRegistry() throws Exception {
        JndiRegistry jndi = super.createRegistry();
        return jndi;
    }    

    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            public void configure() {
		intercept().when(body().contains("Hello")).to("log:hello").to("mock:intercept");
		interceptFrom("direct:start")
		    .when(header("usertype").isEqualTo("test"))
			    .to("mock:intercepted");
 
		from("direct:start").to("mock:result");
		from("direct:start1").to("mock:result");

            }
        };
    }
    
}
