package com.transcend.compute.worker;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.network.IPVersion;
import org.dasein.cloud.network.IpAddress;
import org.dasein.cloud.network.IpAddressSupport;
import org.dasein.cloud.network.NetworkServices;
import org.dasein.cloud.openstack.nova.os.NovaException;
import org.dasein.cloud.openstack.nova.os.NovaException.ExceptionItems;
import org.slf4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import com.msi.tough.core.Appctx;
import com.msi.tough.dasein.DaseinHelper;
import com.msi.tough.model.AccountBean;
import com.msi.tough.query.ServiceRequestContext;
import com.msi.tough.workflow.core.AbstractWorker;
import com.transcend.compute.message.AssociateAddressMessage.AssociateAddressRequest;
import com.transcend.compute.message.AssociateAddressMessage.AssociateAddressResponse;
import com.transcend.compute.utils.ComputeFaults;

public class AssociateAddressWorker extends
        AbstractWorker<AssociateAddressRequest,
        AssociateAddressResponse> {
    private final Logger logger = Appctx.getLogger(AssociateAddressWorker.class
            .getName());

    /**
     * We need a local copy of this doWork to provide the transactional
     * annotation. Transaction management is handled by the annotation, which
     * can only be on a concrete class.
     * @param req
     * @return
     * @throws Exception
     */
    @Transactional
    public AssociateAddressResponse doWork(
            AssociateAddressRequest req) throws Exception {
        logger.debug("Performing work for AssociateAddress.");
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
    protected AssociateAddressResponse doWork0(AssociateAddressRequest req,
            ServiceRequestContext context) throws Exception {

        final AssociateAddressResponse.Builder result =
        		AssociateAddressResponse.newBuilder();
    	final AccountBean account = context.getAccountBean();

        final CloudProvider cloudProvider = DaseinHelper.getProvider(
                account.getDefZone(), account.getTenant(),
                account.getAccessKey(), account.getSecretKey());

        final String publicIp = req.getPublicIp();
    	final String instanceId = req.getInstanceId();

		// Check if instance id refers to existing instance
		if (cloudProvider.getComputeServices().getVirtualMachineSupport()
				.getVirtualMachine(instanceId) == null) {
			throw ComputeFaults.instanceDoesNotExist(instanceId);
		}

        final NetworkServices network = cloudProvider.getNetworkServices();
        final IpAddressSupport ipsupport = network.getIpAddressSupport();

		// check if specified address exists in the pool
		IpAddress address = null;
		for (final IpAddress i : ipsupport.listIpPool(IPVersion.IPV4, false)) {
			if (i.getRawAddress().getIpAddress().equals(publicIp)) {
				address = i;
				break;
			}
		}
		if (address == null
				|| "".equals(address.getRawAddress().getIpAddress())) {
			throw ComputeFaults.IpAddressDoesNotExist(publicIp);
		}

		logger.debug("Address info - BEGIN: \n" + address.toString() + "\n - END");
		logger.debug("Address ID: " + address.getProviderIpAddressId());
		
		// Currently Dasein gets for the actual string "null" rather than the
		// null object for address.getServerId() if there is no assigned
		// instance
		// According to AWS docs, if address is associated with another
		// instance, disassociate it and reassociate to the instance specified
		// in the request.
		if (address.getServerId() != null && !address.getServerId().equals("null")) {
			logger.info("The address " + publicIp
					+ " is currently associated with an instance.");
			logger.info("Diassociating address...");
			ipsupport.releaseFromServer(address.getProviderIpAddressId());
		}

		logger.info("Associating address "
				+ address.getRawAddress().getIpAddress() + " to instance "
				+ instanceId);

		/*
         * TODO: Add VPC Support.
         *   THIS IMPLEMENTATION SUPPORTS EC2-CLASSIC ONLY!
         */
        try {
        	ipsupport.assign(address.getProviderIpAddressId(), instanceId);
        } catch (final CloudException e) {
            final ExceptionItems eitms = NovaException.parseException(
                    e.getHttpCode(), e.getMessage());
            throw new Exception ("Error assigning IP Address: error type = "
            		+ eitms.type.toString());
        }

        /* If execution arrives here, no exceptions occurred */
        result.setReturn(true);
        return result.buildPartial();
    }
}