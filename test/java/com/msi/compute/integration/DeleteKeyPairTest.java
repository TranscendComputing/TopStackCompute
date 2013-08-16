package com.msi.compute.integration;

import static org.junit.Assert.assertTrue;

import java.util.UUID;

import javax.annotation.Resource;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import com.amazonaws.services.ec2.model.CreateKeyPairRequest;
import com.amazonaws.services.ec2.model.DeleteKeyPairRequest;
import com.msi.tough.core.Appctx;
import com.msi.tough.query.AsyncServiceImpl;

public class DeleteKeyPairTest extends AbstractBaseComputeTest {

    private final static Logger logger = Appctx
            .getLogger(DeleteKeyPairTest.class.getName());
    private final String keyName = "deleteKP-"+
            UUID.randomUUID().toString().substring(0, 8);

    private ResponseListener listener = null;

    @Resource
    AsyncServiceImpl asyncService = null;

    @Before
    public void setUp() throws Exception {
        final CreateKeyPairRequest request = new CreateKeyPairRequest();
        request.setKeyName(keyName);
        getComputeClientV2().createKeyPair(request);
        listener = new ResponseListener(logger);
        asyncService.addResponseListener(listener);
    }

    @Test
    public void testGoodDelete() {

    	try
    	{
	    	final DeleteKeyPairRequest dkpr = new DeleteKeyPairRequest(keyName);
	    	getComputeClientV2().deleteKeyPair(dkpr);
    	}
    	catch (Exception e)
    	{
    		logger.error("Unexpected Exception seen.",e);
    		assertTrue("Unexpected Exception seen.",false);
    	}

    }

}
