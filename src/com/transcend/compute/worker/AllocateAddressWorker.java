package com.transcend.compute.worker;

import org.dasein.cloud.CloudErrorType;
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
import com.msi.tough.query.QueryFaults;
import com.msi.tough.query.ServiceRequestContext;
import com.msi.tough.workflow.core.AbstractWorker;
import com.transcend.compute.message.AllocateAddressMessage.AllocateAddressRequestMessage;
import com.transcend.compute.message.AllocateAddressMessage.AllocateAddressResponseMessage;

public class AllocateAddressWorker extends
        AbstractWorker<AllocateAddressRequestMessage,
        AllocateAddressResponseMessage> {
    private final Logger logger = Appctx.getLogger(AllocateAddressWorker.class
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
    public AllocateAddressResponseMessage doWork(
            AllocateAddressRequestMessage req) throws Exception {
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
    protected AllocateAddressResponseMessage doWork0(AllocateAddressRequestMessage req,
            ServiceRequestContext context) throws Exception {
        final AccountBean account = context.getAccountBean();

        final CloudProvider cloudProvider = DaseinHelper.getProvider(
                account.getDefZone(), account.getTenant(),
                account.getAccessKey(), account.getSecretKey());
        final NetworkServices network = cloudProvider.getNetworkServices();
        final IpAddressSupport ipsupport = network.getIpAddressSupport();
        String publicIp = null;
        String publicIpId = null;
        try {
            publicIpId = ipsupport.request(IPVersion.IPV4);
            final IpAddress addrs = ipsupport.getIpAddress(publicIpId);
            publicIp = addrs.getRawAddress().getIpAddress();
        } catch (final CloudException e) {
            final ExceptionItems eitms = NovaException.parseException(
                    e.getHttpCode(), e.getMessage());
            final CloudErrorType type = eitms.type;
            if (type == CloudErrorType.QUOTA) {
                throw QueryFaults.quotaError("Quota for IP Addresses exceeded.");
            }
        }
        final AllocateAddressResponseMessage.Builder result =
                AllocateAddressResponseMessage.newBuilder();
        result.setAllocationId(publicIpId);
        result.setPublicIp(publicIp);
        result.setDomain("standard");
        logger.debug("Returning the result from AllocateAddressWorker.doWork0() method!");
        return result.buildPartial();
    }
}