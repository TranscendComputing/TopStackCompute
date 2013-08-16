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
import com.msi.tough.query.ServiceRequestContext;
import com.msi.tough.workflow.core.AbstractWorker;
import com.transcend.compute.message.DisassociateAddressMessage.DisassociateAddressRequestMessage;
import com.transcend.compute.message.DisassociateAddressMessage.DisassociateAddressResponseMessage;
import com.transcend.compute.utils.ComputeFaults;

public class DisassociateAddressWorker extends
AbstractWorker<DisassociateAddressRequestMessage,DisassociateAddressResponseMessage>{
	private final Logger logger = Appctx.getLogger(DisassociateAddressWorker.class
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
    public DisassociateAddressResponseMessage doWork(
            DisassociateAddressRequestMessage req) throws Exception {
        logger.debug("Performing work for AssociateAddress.");
        return super.doWork(req, getSession());
    }
	
	@Override
	protected DisassociateAddressResponseMessage doWork0(
			DisassociateAddressRequestMessage request, ServiceRequestContext context)
			throws Exception {
		
		final DisassociateAddressResponseMessage.Builder result =
				DisassociateAddressResponseMessage.newBuilder();
    	final AccountBean account = context.getAccountBean();

        final CloudProvider cloudProvider = DaseinHelper.getProvider(
                account.getDefZone(), account.getTenant(),
                account.getAccessKey(), account.getSecretKey());

		final NetworkServices network = cloudProvider.getNetworkServices();
		final IpAddressSupport ipsupport = network.getIpAddressSupport();

		final String reqAddress = request.getPublicIp();

		// check if specified address exists in the pool
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

		ipsupport.releaseFromServer(address.getProviderIpAddressId());
		return result.buildPartial();
	}	
}
