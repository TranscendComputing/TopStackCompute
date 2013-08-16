package com.transcend.compute.worker;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.identity.IdentityServices;
import org.dasein.cloud.identity.ShellKeySupport;
import org.slf4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import com.msi.tough.core.Appctx;
import com.msi.tough.dasein.DaseinHelper;
import com.msi.tough.model.AccountBean;
import com.msi.tough.query.QueryFaults;
import com.msi.tough.query.ServiceRequestContext;
import com.msi.tough.workflow.core.AbstractWorker;
import com.transcend.compute.message.DefaultMessage.DefaultResponseMessage;
import com.transcend.compute.message.DeleteKeyPairMessage.DeleteKeyPairRequestMessage;

public class DeleteKeyPairWorker extends
        AbstractWorker<DeleteKeyPairRequestMessage,
        DefaultResponseMessage> {
    private final Logger logger = Appctx.getLogger(DeleteKeyPairWorker.class
            .getName());

    /**
     * We need a local copy of this doWork to provide the transactional
     * annotation.  Transaction management is handled by the annotation, which
     * can only be on a concrete class.
     * @param req
     * @return
     * @throws Exception
     */
    @Transactional
    public DefaultResponseMessage doWork(
            DeleteKeyPairRequestMessage req) throws Exception {
        logger.debug("Performing work for DeleteKeyPair.");
        return super.doWork(req, getSession());
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.msi.tough.workflow.core.AbstractWorker#doWork0(com.google.protobuf
     * .Message, com.msi.tough.query.ServiceRequestContext)
     */
    @Override
    protected DefaultResponseMessage doWork0(DeleteKeyPairRequestMessage req,
            ServiceRequestContext context) throws Exception {

    	final String keyName = req.getKeyName();

		final AccountBean account = context.getAccountBean();
		logger.debug("Attempting to delete keyname: " + keyName + " defZone: " + account.getDefZone() + " tenant: " + account.getTenant() );

		// Availability zone should be irrelevant; passing default.
		final CloudProvider cloudProvider = DaseinHelper.getProvider(
				account.getDefZone(), account.getTenant(),
				account.getAccessKey(), account.getSecretKey());

		final IdentityServices identity = cloudProvider.getIdentityServices();
		final ShellKeySupport shellKeySupport = identity.getShellKeySupport();

		try {
			shellKeySupport.deleteKeypair(keyName);
		} catch (InternalException ie) {
			throw QueryFaults.InvalidParameterCombination();
		} catch (CloudException ce) {
			throw QueryFaults.internalFailure();
		}

        final DefaultResponseMessage.Builder result =
                DefaultResponseMessage.newBuilder();
        return result.buildPartial();
    }
}