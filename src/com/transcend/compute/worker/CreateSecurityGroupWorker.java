package com.transcend.compute.worker;

import org.dasein.cloud.CloudErrorType;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.network.Firewall;
import org.dasein.cloud.network.FirewallSupport;
import org.dasein.cloud.network.NetworkServices;
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
import com.transcend.compute.message.CreateSecurityGroupMessage.CreateSecurityGroupRequest;
import com.transcend.compute.message.CreateSecurityGroupMessage.CreateSecurityGroupResponse;
import com.transcend.compute.utils.ComputeFaults;

public class CreateSecurityGroupWorker extends
        AbstractWorker<CreateSecurityGroupRequest,
        CreateSecurityGroupResponse> {
    private final Logger logger = Appctx.getLogger(CreateSecurityGroupWorker.class
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
    public CreateSecurityGroupResponse doWork(
    		CreateSecurityGroupRequest req) throws Exception {
        logger.debug("Performing work for AllocateAddress.");
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
    protected CreateSecurityGroupResponse doWork0(CreateSecurityGroupRequest req,
            ServiceRequestContext context) throws Exception {
        final CreateSecurityGroupResponse.Builder result =
        		CreateSecurityGroupResponse.newBuilder();
        final AccountBean account = context.getAccountBean();
        final CloudProvider cloudProvider = DaseinHelper.getProvider(
                account.getDefZone(), account.getTenant(),
                account.getAccessKey(), account.getSecretKey());
        final NetworkServices network = cloudProvider.getNetworkServices();
        final FirewallSupport fwSupport = network.getFirewallSupport();


        String firewallId = null;

		for (Firewall f : fwSupport.list()) {
			if (f.getName().equals(req.getGroupName())) {
				throw ComputeFaults.GroupAlreadyExists(req.getGroupName());
			}
		}

        try {
            firewallId = fwSupport.create(req.getGroupName(),
            		req.getGroupDescription());
            result.setGroupId(firewallId);
        } catch (final CloudException e) {
			final ExceptionItems eitms = NovaException.parseException(
					e.getHttpCode(), e.getMessage());
			final CloudErrorType type = eitms.type;
			if (type == CloudErrorType.QUOTA) {
				throw QueryFaults.quotaError(
						"Quota for Security Group exceeded");
			}
		} catch (Exception e) {
			throw QueryFaults.internalFailure();
        }

        return result.buildPartial();
    }
}