package org.jboss.fuse.bam.detour;

import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.apache.camel.EndpointInject;
import org.apache.camel.component.mock.MockEndpoint;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.JndiRegistry;

public class DetourTest extends CamelTestSupport {
    
    private static final String BODY = "<order custId=\"123\"/>";
    private ControlBean controlBean;

    @Test
    public void testDetourSet() throws Exception {
	// EXCLUDE-BEGIN 
        controlBean.setDetour(true);
        
        MockEndpoint resultEndpoint = getMockEndpoint("mock:result");
        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.message(0).body().equals(BODY);

        MockEndpoint detourEndpoint = getMockEndpoint("mock:detour");
        detourEndpoint.expectedMessageCount(1);
        detourEndpoint.message(0).body().equals(BODY);
        
        template.sendBody("direct:start", BODY);
                
        assertMockEndpointsSatisfied();        
	// EXCLUDE-END
    }

    @Test
    public void testDetourNotSet() throws Exception {      
	// EXCLUDE-BEGIN
        controlBean.setDetour(false);
        
        MockEndpoint resultEndpoint = getMockEndpoint("mock:result");
        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.message(0).body().equals(BODY);

        MockEndpoint detourEndpoint = getMockEndpoint("mock:detour");
        detourEndpoint.expectedMessageCount(0);
        
        template.sendBody("direct:start", BODY);
                
        assertMockEndpointsSatisfied();        
	// EXCLUDE-END
    }    
    
    @Override
    protected JndiRegistry createRegistry() throws Exception {
        JndiRegistry jndi = super.createRegistry();
        controlBean = new ControlBean();
        jndi.bind("controlBean", controlBean);
        return jndi;
    }    

    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            public void configure() {
	// EXCLUDE-BEGIN
                from("direct:start").choice()
                    .when().method("controlBean", "isDetour").to("mock:detour").end()
                    .to("mock:result");                
		// EXCLUDE-END
            }
        };
    }
    
    public final class ControlBean {
        private boolean detour;  

        public void setDetour(boolean detour) {
            this.detour = detour;
        }

        public boolean isDetour() {
            return detour;
        }
    }    
}
