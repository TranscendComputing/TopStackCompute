package com.transcend.compute.worker;

import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.network.IPVersion;
import org.dasein.cloud.network.IpAddress;
import org.dasein.cloud.network.IpAddressSupport;
import org.dasein.cloud.network.NetworkServices;
import org.slf4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import com.msi.tough.core.Appctx;
import com.msi.tough.dasein.DaseinHelper;
import com.msi.tough.model.AccountBean;
import com.msi.tough.query.QueryFaults;
import com.msi.tough.query.ServiceRequestContext;
import com.msi.tough.workflow.core.AbstractWorker;
import com.transcend.compute.message.DefaultMessage.DefaultResponseMessage;
import com.transcend.compute.message.ReleaseAddressMessage.ReleaseAddressRequestMessage;
import com.transcend.compute.utils.ComputeFaults;

public class ReleaseAddressWorker extends
        AbstractWorker<ReleaseAddressRequestMessage,
        DefaultResponseMessage> {
    private final Logger logger = Appctx.getLogger(ReleaseAddressWorker.class
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
            ReleaseAddressRequestMessage req) throws Exception {
        logger.debug("Performing work for ReleaseAddress.");
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
    protected DefaultResponseMessage doWork0(ReleaseAddressRequestMessage req,
            ServiceRequestContext context) throws Exception {

    	final String reqAddress = req.getPublicIp();

		final AccountBean account = context.getAccountBean();

		// Availability zone should be irrelevant; passing default.
		final CloudProvider cloudProvider = DaseinHelper.getProvider(
				account.getDefZone(), account.getTenant(),
				account.getAccessKey(), account.getSecretKey());

		final NetworkServices network = cloudProvider.getNetworkServices();
		final IpAddressSupport ipsupport = network.getIpAddressSupport();

		// check if specified address exists
		IpAddress address = null;
		for (final IpAddress i : ipsupport.listIpPool(IPVersion.IPV4, false)) {
			if (i.getRawAddress().getIpAddress().equals(reqAddress)) {
				address = i;
				break;
			}
		}
		if (address == null
				|| "".equals(address.getRawAddress().getIpAddress())) {
			throw ComputeFaults.IpAddressDoesNotExist(reqAddress);
		}
		// previously, getServerId returns the string "null" if it is not
		// associated with an instance
		if (!"null".equals(address.getServerId()) &&
		        ! (address.getServerId() == null) ) {
			logger.info("The address " + reqAddress
					+ " is currently associated with an instance.");
			logger.info("Diassociating address...");
			ipsupport.releaseFromServer(address.getProviderIpAddressId());
		}

		int count = 0;
		// Currently with dasein, address must be disassociated
		// Wait for 10 seconds max for address to be released
		while (count < 5) {
			Thread.sleep(2000);
			IpAddress ipAddress = ipsupport.getIpAddress(address.getProviderIpAddressId());
			if ("null".equals(ipAddress.getServerId())
					|| ipAddress.getServerId() == null) {
				break;
			}
			count++;
		}
		// InternalFailure if ip address wasn't released within 10 seconds.
		if (count == 5) {
			throw QueryFaults.internalFailure();
		}
		logger.info("Releasing address "
				+ address.getRawAddress().getIpAddress());
		ipsupport.releaseFromPool(address.getProviderIpAddressId());

        final DefaultResponseMessage.Builder result =
                DefaultResponseMessage.newBuilder();

        return result.buildPartial();
    }
}