package com.msi.compute.integration;

import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionConfiguration;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.msi.tough.query.AsyncServiceImpl.ServiceResponseListener;
import com.msi.tough.query.ErrorResponse;
import com.msi.tough.query.ServiceResponse;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/test-computeContext.xml"})
@TransactionConfiguration(transactionManager = "transactionManager", defaultRollback = false)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class })
public class AbstractBaseComputeTest {

	@Autowired
	private AWSCredentials creds;

	@Autowired
	private AmazonEC2Client computeClient;

    @Autowired
    private AmazonEC2Client computeClientV2;

	@Autowired
	private String defaultAvailabilityZone;

//	@Autowired
//	private String tenantId;

	public AWSCredentials getCreds() {
		return creds;
	}

	/*public AccountType getAccountType(){
		AccountType result = new AccountType();
		result.setAccessKey(accessKey)
		return result;
	}*/

	public void setCreds(AWSCredentials creds) {
		this.creds = creds;
	}

//	public String getTenantId(){
//		return tenantId;
//	}
//	public void setTenantId(String tenant){
//		this.tenantId=tenant;
//	}

	public AmazonEC2Client getComputeClient() {
		return computeClient;
	}

	public void setComputeClient(AmazonEC2Client compute) {
		this.computeClient = compute;
	}

    public AmazonEC2Client getComputeClientV2() {
        return computeClientV2;
    }

    public void setComputeClientV2(AmazonEC2Client compute) {
        this.computeClientV2 = compute;
    }

	public String getDefaultAvailabilityZone() {
		return defaultAvailabilityZone;
	}

	public void setDefaultAvailabilityZone(String defaultAvailabilityZone) {
		this.defaultAvailabilityZone = defaultAvailabilityZone;
	}

	protected class ResponseListener implements ServiceResponseListener {

        ServiceResponse response = null;
        ErrorResponse error = null;
        private Logger logger;
        
        public ResponseListener(Logger logger)
        {
        	this.logger = logger;
        }

        /* (non-Javadoc)
         * @see com.transcend.compute.servlet.AsyncServiceImpl.ServiceResponseListener#handleResponse(com.msi.tough.query.ServiceResponse)
         */
        @Override
        public void handleResponse(ServiceResponse response) {
            synchronized (this) {
            	if (logger != null)
            		logger.debug("Got response:" + response);
                this.response = response;
            }
        }

        /* (non-Javadoc)
         * @see com.msi.tough.query.AsyncServiceImpl.ServiceResponseListener#handleError(com.msi.tough.query.ErrorResponse)
         */
        @Override
        public void handleError(ErrorResponse error) {
            synchronized (this) {
            	if (logger != null)
            		logger.debug("Got response:" + response);
                this.error = error;
            }
        }

    }
}