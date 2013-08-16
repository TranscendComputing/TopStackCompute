package com.msi.compute.integration.workflow;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import java.util.UUID;

import javax.annotation.Resource;

import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mule.api.MuleContext;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.module.client.MuleClient;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.msi.compute.integration.AbstractBaseComputeTest;
import com.msi.tough.core.Appctx;
import com.msi.tough.query.AsyncServiceImpl;
import com.msi.tough.query.AsyncServiceImpl.ServiceResponseListener;
import com.msi.tough.query.ErrorResponse;
import com.msi.tough.query.ServiceResponse;
import com.transcend.compute.message.DescribeInstancesMessage.DescribeInstancesRequestMessage;

@Component
@Ignore
public class DescribeInstancesZmqWorkerTest extends AbstractBaseComputeTest
 implements ApplicationContextAware {

    private final static Logger logger = Appctx
            .getLogger(DescribeInstancesZmqWorkerTest.class.getName());

    private static final int MAX_WAIT_SECS = 3;

    private final String requestId = UUID.randomUUID().toString()
            .substring(0, 8);

    @Resource
    private SessionFactory sessionFactory = null;

    @Resource
    AsyncServiceImpl asyncService = null;

    private ResponseListener listener = null;

    private ApplicationContext appContext;

    @Before
    public void registerForResponse() {
        listener = new ResponseListener();
        asyncService.addResponseListener(listener);
    }

    @Test
    @Transactional
    public void testValidJob() throws Exception
    {
        SpringXmlConfigurationBuilder builder =
                new SpringXmlConfigurationBuilder("mule-workflow-config.xml,mule-test-transports-config.xml");
        builder.setParentContext(appContext);

        MuleContext muleContext = new DefaultMuleContextFactory().createMuleContext(builder);
        muleContext.start();
        MuleClient client = new MuleClient(muleContext);
        DescribeInstancesRequestMessage.Builder req =
                DescribeInstancesRequestMessage.newBuilder();
        req.setTypeId(true);
        req.setCallerAccessKey(getCreds().getAWSAccessKeyId());
        req.setRequestId(requestId);

        client.dispatch("DirectToZMQIn", req.build(), null);
        for (int count = 0; count < MAX_WAIT_SECS; count++) {
            synchronized (listener) {
                if (listener.response != null) {
                    break;
                }
                if (listener.error != null) {
                    break;
                }
            }
            Thread.sleep(1000);
        }
        if (listener.error != null) {
            throw listener.error;
        }
        assertNotNull("Expect response.", listener.response);
        assertEquals("Expect response to my request.",
                requestId,
                listener.response.getRequestId());
        logger.debug("Got payload:"+listener.response.getPayload());
    }

    /* (non-Javadoc)
     * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
     */
    @Override
    public void setApplicationContext(ApplicationContext appContext)
            throws BeansException {
        this.appContext = appContext;
    }

    private class ResponseListener implements ServiceResponseListener {

        ServiceResponse response = null;
        ErrorResponse error = null;

        /* (non-Javadoc)
         * @see com.transcend.compute.servlet.AsyncServiceImpl.ServiceResponseListener#handleResponse(com.msi.tough.query.ServiceResponse)
         */
        @Override
        public void handleResponse(ServiceResponse response) {
            synchronized (this) {
                logger.debug("Got response:" + response);
                this.response = response;
            }
        }

        /* (non-Javadoc)
         * @see com.msi.tough.query.AsyncServiceImpl.ServiceResponseListener#handleError(com.msi.tough.query.ErrorResponse)
         */
        @Override
        public void handleError(ErrorResponse error) {
            this.error = error;
        }

    }
}
