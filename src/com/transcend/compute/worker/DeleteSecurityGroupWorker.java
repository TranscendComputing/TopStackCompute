package com.transcend.compute.worker;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.network.Firewall;
import org.dasein.cloud.network.FirewallSupport;
import org.dasein.cloud.network.NetworkServices;
import org.slf4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import com.msi.tough.core.Appctx;
import com.msi.tough.dasein.DaseinHelper;
import com.msi.tough.model.AccountBean;
import com.msi.tough.query.ServiceRequestContext;
import com.msi.tough.workflow.core.AbstractWorker;
import com.transcend.compute.message.DeleteSecurityGroupMessage.DeleteSecurityGroupRequestMessage;
import com.transcend.compute.message.DeleteSecurityGroupMessage.DeleteSecurityGroupResponseMessage;
import com.transcend.compute.utils.ComputeFaults;

public class DeleteSecurityGroupWorker extends AbstractWorker<DeleteSecurityGroupRequestMessage, DeleteSecurityGroupResponseMessage>{
	private final Logger logger = Appctx.getLogger(DeleteSecurityGroupWorker.class
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
    public DeleteSecurityGroupResponseMessage doWork(
    		DeleteSecurityGroupRequestMessage req) throws Exception {
        logger.debug("Performing work for DeleteSecurityGroup.");
        return super.doWork(req, getSession());
    }

	@Override
	protected DeleteSecurityGroupResponseMessage doWork0(
			DeleteSecurityGroupRequestMessage request, ServiceRequestContext context)
			throws Exception {
		DeleteSecurityGroupResponseMessage.Builder result = DeleteSecurityGroupResponseMessage.newBuilder();
		final AccountBean account = context.getAccountBean();

		final CloudProvider cloudProvider = DaseinHelper.getProvider(
				account.getDefZone(), account.getTenant(),
				account.getAccessKey(), account.getSecretKey());
		final NetworkServices network = cloudProvider.getNetworkServices();
		final FirewallSupport fs = network.getFirewallSupport();

		try {
			if (request.getGroupId() != null
					&& !"".equals(request.getGroupId())) {
				final String group = request.getGroupId();
				if (fs.getFirewall(group) == null) {
					throw ComputeFaults.GroupIdDoesNotExist(group);
				}
				fs.delete(request.getGroupId());
				logger.info("Deleted group with ID " + request.getGroupId());
			} else if (request.getGroupName() != null
					&& !"".equals(request.getGroupName())) {
				final String group = request.getGroupName();
				boolean found = false;
				for (Firewall f : fs.list()) {
					if (f.getName().equals(group)) {
						fs.delete(f.getProviderFirewallId());
						logger.info("Deleted group with name " + group);
						found = true;
						break;
					}
				}
				if (!found) {
					throw ComputeFaults.GroupNameDoesNotExist(group);
				}
			}
		} catch (CloudException e) {
			throw ComputeFaults.GroupInUse();
		}
		// Not 100% sure what AWS means this value to represent, but it looks
		// like it will never be false since an error will be thrown otherwise.
		result.setReturn(true);
		return result.buildPartial();

	}


}
