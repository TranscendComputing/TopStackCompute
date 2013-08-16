package com.transcend.compute.worker;

import java.util.Date;

import org.dasein.cloud.CloudErrorType;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.Architecture;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.compute.VMLaunchOptions;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineProduct;
import org.dasein.cloud.compute.VirtualMachineSupport;
import org.dasein.cloud.openstack.nova.os.NovaException;
import org.dasein.cloud.openstack.nova.os.NovaException.ExceptionItems;
import org.slf4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import com.msi.tough.core.Appctx;
import com.msi.tough.core.StringHelper;
import com.msi.tough.dasein.DaseinHelper;
import com.msi.tough.model.AccountBean;
import com.msi.tough.model.InstanceBean;
import com.msi.tough.query.QueryFaults;
import com.msi.tough.query.ServiceRequestContext;
import com.msi.tough.utils.InstanceUtil;
import com.msi.tough.workflow.core.AbstractWorker;
import com.transcend.compute.message.InstanceMessage.Instance;
import com.transcend.compute.message.InstanceMessage.Instance.InstanceState;
import com.transcend.compute.message.ReservationMessage;
import com.transcend.compute.message.RunInstancesMessage.RunInstancesRequestMessage;
import com.transcend.compute.message.RunInstancesMessage.RunInstancesResponseMessage;

public class RunInstancesWorker extends
        AbstractWorker<RunInstancesRequestMessage, RunInstancesResponseMessage> {
    private final Logger logger = Appctx.getLogger(RunInstancesWorker.class
            .getName());

    /**
     * We need a local copy of this doWork to provide the transactional
     * annotation. Transaction management is handled by the annotation, which
     * can only be on a concrete class.
     *
     * @param req
     * @return
     * @throws Exception
     */
    @Transactional
    public RunInstancesResponseMessage doWork(RunInstancesRequestMessage req)
            throws Exception {
        logger.debug("Performing work for RunInstances.");
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
    protected RunInstancesResponseMessage doWork0(RunInstancesRequestMessage req,
            ServiceRequestContext context) throws Exception {
        final AccountBean account = context.getAccountBean();

        final CloudProvider cloudProvider = DaseinHelper.getProvider(req
                .getPlacement().getAvailabilityZone(), account.getTenant(),
                account.getAccessKey(), account.getSecretKey());

        final ComputeServices compute = cloudProvider.getComputeServices();
        final VirtualMachineSupport vmSupport = compute
                .getVirtualMachineSupport();

        int min = req.getMinCount();
        int max = req.getMaxCount();

        if (max < min) {
            throw QueryFaults
                    .InvalidParameterCombination("RunInstances was called with both minCount and maxCount set to 0, or minCount > maxCount.");
        }

        final ReservationMessage.Reservation.Builder reserve = ReservationMessage.Reservation
                .newBuilder();
        for (int count = 0; count < 1; count++) {
            final String installId = Appctx.getBean("INSTALL_ID");
            final String hostname = InstanceUtil.getHostName(req.getPlacement()
                    .getAvailabilityZone(), installId, "compute");
            String instanceType = req.getInstanceType();
            if (cloudProvider.getProviderName().equals("OpenStack")) {
                instanceType = toFlavorId(vmSupport, instanceType);
            }

            final VMLaunchOptions opts = VMLaunchOptions.getInstance(
                    instanceType, req.getImageId(), hostname,
                    "FriendlyName", "Instance spun up from compute services.");
            opts.withBoostrapKey(account.getDefKeyName());
            if (StringHelper.emptyToNull(req.getKernelId()) != null) {
                opts.getMetaData().put("kernelId", req.getKernelId());
            }
            if (StringHelper.emptyToNull(req.getRamdiskId()) != null) {
                opts.getMetaData().put("ramdiskId", req.getRamdiskId());
            }
            if (StringHelper.emptyToNull(req.getUserData()) != null) {
                opts.withUserData(req.getUserData());
            }
            VirtualMachine vm = null;
            try {
                vm = vmSupport.launch(opts);
            } catch (final CloudException e) {
                final ExceptionItems eitms = NovaException.parseException(
                        e.getHttpCode(), e.getMessage());
                final CloudErrorType type = eitms.type;
                if (type == CloudErrorType.QUOTA) {
                    throw QueryFaults
                            .quotaError("Quota for Instances exceeded");
                }
            }
            reserve.setReservationId("r"+vm.getProviderVirtualMachineId());  // Using VM ID as res ID.
            reserve.setOwnerId(vm.getProviderOwnerId());
            reserve.setRequesterId(vm.getProviderOwnerId());
            logger.info("instance launched " + vm.getProviderVirtualMachineId());
            final InstanceBean ib = new InstanceBean();
            // ib.setUserId(ac.getId());
            ib.setAvzone(req.getPlacement().getAvailabilityZone());
            ib.setHostname(hostname);
            ib.setStatus(vm.getCurrentState().toString().toLowerCase());
            ib.setInstanceId(vm.getProviderVirtualMachineId());
            if (vm.getPrivateAddresses() != null
                    && vm.getPrivateAddresses().length != 0) {
                ib.setPrivateIp(vm.getPrivateAddresses()[0].getIpAddress());
            }
            if (vm.getPublicAddresses() != null
                    && vm.getPublicAddresses().length != 0) {
                ib.setPublicIp(vm.getPublicAddresses()[0].getIpAddress());
            }

            getSession().save(ib);
            final Instance.Builder i = Instance.newBuilder();
            i.setInstanceId(vm.getProviderVirtualMachineId());
            i.setImageId(opts.getMachineImageId());
            i.setLaunchTime(new Date(vm.getCreationTimestamp() * 1000)
                    .getTime());
            i.setType(fromFlavorId(vmSupport, opts.getStandardProductId()));
            i.setPlacement(req.getPlacement());
            final InstanceState.Builder state = InstanceState.newBuilder();
            String stateName = vm.getCurrentState().toString().toLowerCase();
            state.setName(stateName);
            int code = 0;
            if (stateName.equals("running")) {
                code = 16;
            } else if (stateName.equals("terminated")) {
                code = 48;
            } else if (stateName.equals("stopping")) {
                code = 64;
            } else if (stateName.equals("stopped")) {
                code = 80;
            }
            state.setCode(code);
            i.setState(state);
            if (vm.getPrivateAddresses() != null
                    && vm.getPrivateAddresses().length != 0) {
                i.setPrivateIp(vm.getPrivateAddresses()[0].getIpAddress());
            }
            if (vm.getPublicAddresses() != null
                    && vm.getPublicAddresses().length != 0) {
                i.setPublicIp(vm.getPublicAddresses()[0].getIpAddress());
            }
            String platform = vm.getPlatform().toString();
            if (StringHelper.isBlank(platform)) {
                platform = "Unknown";
            }
            i.setPlatform(platform);
            String arch = vm.getArchitecture().toString();
            if (StringHelper.isBlank(arch)) {
                arch = Architecture.I32.toString();
            }
            i.setArchitecture(arch);
            reserve.addInstance(i.build());
        }
        final RunInstancesResponseMessage.Builder result = RunInstancesResponseMessage
                .newBuilder();
        result.setReservation(reserve);

        return result.buildPartial();
    }

	private String fromFlavorId(VirtualMachineSupport vmSupport, String flavorId)
			throws InternalException, CloudException {
		String flavorName = null;

		Iterable<Architecture> listArchitectures = vmSupport
				.listSupportedArchitectures();
		for (Architecture arch : listArchitectures) {
			Iterable<VirtualMachineProduct> products = vmSupport.listProducts(arch);
			for (VirtualMachineProduct product : products) {
				if (product.getProviderProductId().equals(flavorId)) {
					flavorName = product.getName();
					break;
				}
			}

			if (flavorName != null) {
				break;
			}
		}

		return flavorName;
    }

	private String toFlavorId(VirtualMachineSupport vmSupport, String flavorName)
			throws InternalException, CloudException {
		String flavorId = null;

		Iterable<Architecture> listArchitectures = vmSupport
				.listSupportedArchitectures();
		for (Architecture arch : listArchitectures) {
			Iterable<VirtualMachineProduct> products = vmSupport.listProducts(arch);
			for (VirtualMachineProduct product : products) {
				if (product.getName().equals(flavorName)) {
					flavorId = product.getProviderProductId();
					break;
				}
			}

			if (flavorId != null) {
				break;
			}
		}

		return flavorId;
	}


}