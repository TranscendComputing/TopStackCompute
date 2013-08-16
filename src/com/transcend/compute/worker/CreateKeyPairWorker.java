package com.transcend.compute.worker;

import org.dasein.cloud.CloudErrorType;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.identity.IdentityServices;
import org.dasein.cloud.identity.SSHKeypair;
import org.dasein.cloud.identity.ShellKeySupport;
import org.dasein.cloud.openstack.nova.os.NovaException;
import org.dasein.cloud.openstack.nova.os.NovaException.ExceptionItems;
import org.slf4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import com.msi.tough.core.Appctx;
import com.msi.tough.dasein.DaseinHelper;
import com.msi.tough.model.AccountBean;
import com.msi.tough.query.QueryFaults;
import com.msi.tough.query.ServiceRequestContext;
import com.msi.tough.workflow.core.AbstractWorker;
import com.transcend.compute.message.CreateKeyPairMessage.CreateKeyPairRequestMessage;
import com.transcend.compute.message.CreateKeyPairMessage.CreateKeyPairResponseMessage;

public class CreateKeyPairWorker extends
        AbstractWorker<CreateKeyPairRequestMessage,
        CreateKeyPairResponseMessage> {
    private final Logger logger = Appctx.getLogger(CreateKeyPairWorker.class
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
    public CreateKeyPairResponseMessage doWork(
            CreateKeyPairRequestMessage req) throws Exception {
        logger.debug("Performing work for CreateKeyPair.");
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
    protected CreateKeyPairResponseMessage doWork0(CreateKeyPairRequestMessage req,
            ServiceRequestContext context) throws Exception {

    	final String keyName = req.getKeyName();

		final AccountBean account = context.getAccountBean();
		logger.debug("Attempting to Create keyname: " + keyName + " defZone: " + account.getDefZone() + " tenant: " + account.getTenant() );

		try {
		    account.getSecretKey();
		} catch (IllegalStateException e) {
		    throw QueryFaults.AuthorizationNotFound();
		}

		// Availability zone should be irrelevant; passing default.
		final CloudProvider cloudProvider = DaseinHelper.getProvider(
				account.getDefZone(), account.getTenant(),
				account.getAccessKey(), account.getSecretKey());

		final IdentityServices identity = cloudProvider.getIdentityServices();
		final ShellKeySupport shellKeySupport = identity.getShellKeySupport();
		SSHKeypair newKeyPair = null;

		try {
			newKeyPair = shellKeySupport.createKeypair(keyName);
		} catch (InternalException ie) {
			throw QueryFaults.InvalidParameterCombination();
		} catch (CloudException e) {
			final ExceptionItems eitms = NovaException.parseException(
					e.getHttpCode(), e.getMessage());
			final CloudErrorType type = eitms.type;
			if (type == CloudErrorType.QUOTA) {
				throw QueryFaults.quotaError("Quota for KeyPairs exceeded");
			}
			throw QueryFaults.internalFailure();
		}


        final CreateKeyPairResponseMessage.Builder result =
                CreateKeyPairResponseMessage.newBuilder();
        result.setKeyFingerprint(newKeyPair.getFingerprint());
        result.setKeyName(newKeyPair.getName());
        result.setKeyMaterial(new String(newKeyPair.getPrivateKey()));

		logger.debug("Provider keypair ID is "
				+ newKeyPair.getProviderKeypairId());
        return result.buildPartial();
    }
}