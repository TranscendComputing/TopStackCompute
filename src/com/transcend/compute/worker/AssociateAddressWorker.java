package com.transcend.compute.worker;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineSupport;
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
import com.transcend.compute.message.AssociateAddressMessage.AssociateAddressRequest;
import com.transcend.compute.message.AssociateAddressMessage.AssociateAddressResponse;
import com.transcend.compute.utils.ComputeFaults;

public class AssociateAddressWorker extends
        AbstractWorker<AssociateAddressRequest,
        AssociateAddressResponse> {
    private final Logger logger = Appctx.getLogger(AssociateAddressWorker.class
            .getName());

    private static final int RETRY_MAX = 5;
    private static final int RETRY_SECS = 1;

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
        final ComputeServices compute = cloudProvider.getComputeServices();
        final VirtualMachineSupport vmSupport = compute
                .getVirtualMachineSupport();

        final String publicIp = req.getPublicIp();
        final String instanceId = req.getInstanceId();

        VirtualMachine vm = vmSupport.getVirtualMachine(instanceId);
        // Check if instance id refers to existing instance
        if (vm == null) {
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

        if ("OpenStack".equals(cloudProvider.getProviderName())) {
            String privateIp = null;
            int retryCount = 0;
            while (privateIp == null && retryCount++ < RETRY_MAX) {
                // Must avoid associating too early; instance should have a fixed IP.
                if (vm.getPrivateAddresses() != null
                        && vm.getPrivateAddresses().length > 0) {
                    privateIp = vm.getPrivateAddresses()[0].getIpAddress();
                }
                if (privateIp == null
                        || privateIp.length() == 0
                        || privateIp.equals("0.0.0.0")) {
                    logger.debug("Instance does not have private IP, waiting for network ready.");
                    privateIp = null;
                    Thread.sleep(RETRY_SECS * 1000);
                    vm = vmSupport.getVirtualMachine(instanceId);
                }
            }
            if (retryCount >= RETRY_MAX) {
                logger.error("Error assigning IP Address: instance doesn't " +
                        "have a private IP.");
                throw QueryFaults.invalidState();
            }
        }

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
