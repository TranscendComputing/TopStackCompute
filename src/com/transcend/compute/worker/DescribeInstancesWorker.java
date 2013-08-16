package com.transcend.compute.worker;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineSupport;
import org.dasein.cloud.network.Firewall;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.amazonaws.services.ec2.model.EbsInstanceBlockDevice;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.InstanceBlockDeviceMapping;
import com.msi.tough.core.Appctx;
import com.msi.tough.dasein.DaseinHelper;
import com.msi.tough.model.AccountBean;
import com.msi.tough.query.ServiceRequestContext;
import com.msi.tough.workflow.core.AbstractWorker;
import com.transcend.compute.message.DescribeInstancesMessage.DescribeInstancesRequestMessage;
import com.transcend.compute.message.DescribeInstancesMessage.DescribeInstancesResponseMessage;
import com.transcend.compute.message.InstanceMessage.Instance;
import com.transcend.compute.message.InstanceMessage.Instance.InstanceState;
import com.transcend.compute.message.InstanceMessage.Instance.Placement;
import com.transcend.compute.message.InstanceMessage.Instance.Tag;
import com.transcend.compute.message.ReservationMessage.Reservation;
import com.transcend.compute.message.SecurityGroupMessage.SecurityGroup;
import com.transcend.compute.utils.ComputeFaults;
import com.transcend.compute.utils.InstanceUtils;

@Component
public class DescribeInstancesWorker extends
    AbstractWorker<DescribeInstancesRequestMessage,
    DescribeInstancesResponseMessage> {

    private final static Logger logger = Appctx
            .getLogger(DescribeInstancesWorker.class.getName());

    /**
     * We need a local copy of this doWork to provide the transactional
     * annotation.  Transaction management is handled by the annotation, which
     * can only be on a concrete class.
     * @param req
     * @return
     * @throws Exception
     */
    @Transactional
    public DescribeInstancesResponseMessage doWork(
            DescribeInstancesRequestMessage req) throws Exception {
        logger.debug("Performing work for DescribeInstances.");
        return super.doWork(req, getSession());
    }

    /* (non-Javadoc)
     * @see com.transcend.compute.worker.AbstractWorker#doWork0(com.google.protobuf.Message)
     */
    @Override
    @Transactional
    public DescribeInstancesResponseMessage
        doWork0(DescribeInstancesRequestMessage request,
                ServiceRequestContext context) throws Exception {
        final DescribeInstancesResponseMessage.Builder result =
                DescribeInstancesResponseMessage.newBuilder();
        AccountBean account = context.getAccountBean();
        final CloudProvider cloudProvider = DaseinHelper.getProvider(
                account.getDefZone(), account.getTenant(),
                account.getAccessKey(), account.getSecretKey());
        final ComputeServices comp = cloudProvider.getComputeServices();

        final VirtualMachineSupport vmServ = comp.getVirtualMachineSupport();

        for (final String vmId : request.getInstanceIdsList()) {
            if (vmServ.getVirtualMachine(vmId) == null)
                throw ComputeFaults.instanceDoesNotExist(vmId);
        }
        // Don't see anything about Reservations in Dasein, so for now just
        // putting each instance in its own reservation set
        for (final VirtualMachine vm : vmServ.listVirtualMachines()) {
            final String vmId = vm.getProviderVirtualMachineId();
            if (request.getInstanceIdsList().size() > 0) {
                if (!request.getInstanceIdsList().contains(vmId)) {
                    continue;
                }
            }
            // check to see if vm matches filters, if not then move on to next
            // vm
            List<Filter> filters = new ArrayList<Filter>();
            for (com.transcend.compute.message.DescribeInstancesMessage.DescribeInstancesRequestMessage.Filter filterMessage : request.getFilterList()) {
                Filter filter = new Filter();
                filter.setName(filterMessage.getName());
                filter.setValues(filterMessage.getValueList());
                filters.add(filter);
            }
            if (!InstanceUtils.checkFilters(vmServ, vm,
                    filters,
                    createFilterNameMap())) {
                continue;
            }
            final Instance.Builder instance = Instance.newBuilder();
            final Reservation.Builder reserve = Reservation.newBuilder();
            reserve.setReservationId("r"+vmId);  // Using VM ID as res ID.
            reserve.setOwnerId(vm.getProviderOwnerId());
            reserve.setRequesterId(vm.getProviderOwnerId());
            instance.setInstanceId(vmId);
            instance.setArchitecture(vm.getArchitecture().toString());
            instance.setImageId(vm.getProviderMachineImageId());
            // instance.setKernelId(kernelId)
            // instance.setRamdiskId(ramdiskId
            // For some reason, list of security groups isn't populated, so the
            // security group defaults to 'default'.
            // Seems like the json generated from the request to the Openstack
            // server doesn't have a part for "security_groups".
            {
                for (String firewallId : vmServ.listFirewalls(vmId)) {
                    Firewall f = cloudProvider.getNetworkServices()
                            .getFirewallSupport().getFirewall(firewallId);
                    SecurityGroup.Builder group = SecurityGroup.newBuilder();
                    group.setGroupName(f.getName());
                    group.setGroupId(f.getProviderFirewallId());
                    instance.addGroup(group.build());
                }
            }
            {
                final InstanceState.Builder state = InstanceState.newBuilder();
                state.setName(vm.getCurrentState().toString().toLowerCase());
                state.setCode(InstanceUtils.getStateCode(vm.getCurrentState()
                        .toString().toLowerCase()));
                instance.setState(state.build());
            }
            {
                final Placement.Builder placement = Placement.newBuilder();
                placement.setAvailabilityZone(cloudProvider
                        .getDataCenterServices()
                        .getRegion(vm.getProviderRegionId()).getName());
                instance.setPlacement(placement);
            }

            if (vm.getPrivateDnsAddress() != null) {
                instance.setPrivateDnsName(vm.getPrivateDnsAddress());
            }
            if (vm.getPublicDnsAddress() != null) {
                instance.setPublicDnsName(vm.getPublicDnsAddress());
            }
            // uses productId() to get the actual name like "m1.medium"
            instance.setType(vmServ.getProduct(vm.getProductId())
                    .getName());
            instance.setLaunchTime(new Date(vm.getCreationTimestamp()).getTime());
            if (vm.getProviderSubnetId() != null) {
                instance.setSubnetId(vm.getProviderSubnetId());
            }
            // AWS Instance only supports a single string. For now, we'll just
            // return the first IP given.
            if (vm.getPrivateAddresses() != null
                    && vm.getPrivateAddresses().length != 0) {
                instance.setPrivateIp(vm.getPrivateAddresses()[0].getIpAddress());
            }
            if (vm.getPublicAddresses() != null
                    && vm.getPublicAddresses().length != 0) {
                instance.setPublicIp(vm.getPublicAddresses()[0].getIpAddress());
            }
            instance.setPlatform(vm.getPlatform().toString());

            {
                final Tag.Builder nameTag = Tag.newBuilder();
                nameTag.setKey("name");
                nameTag.setValue(vm.getName());
                instance.addTag(nameTag);
            }
            for (String key : vm.getTags().keySet()) {
                final Tag.Builder tag = Tag.newBuilder();
                tag.setKey(key);
                tag.setValue(vm.getTags().get(key));
                instance.addTag(tag);
            }
            // TODO: blockDeviceMapping: Can't see a good way to do through
            // Dasein other than looping through all the Volumes
            // and seeing if they have an attachment to the instance being
            // looked at. Seems inefficient.
            List<InstanceBlockDeviceMapping> blockDeviceMappings = new ArrayList<InstanceBlockDeviceMapping>();
            for (org.dasein.cloud.compute.Volume vol : comp.getVolumeSupport()
                    .listVolumes()) {
                if (vmId.equals(vol.getProviderVirtualMachineId())) {
                    final InstanceBlockDeviceMapping blockDevice = new InstanceBlockDeviceMapping();
                    final EbsInstanceBlockDevice volume = new EbsInstanceBlockDevice();
                    volume.setVolumeId(vol.getName());
                    // Only statuses available are 'AVAILABLE', 'PENDING', and
                    // 'DELETED'
                    volume.setStatus("in-use");
                    blockDevice.setDeviceName(vol.getDeviceId());
                    blockDevice.setEbs(volume);
                    blockDeviceMappings.add(blockDevice);
                }
            }

            //TODO JHG: protobuf block device mappings
            //instance.setBlockDeviceMappings(blockDeviceMappings);
            if (vm.getProviderKeypairId() != null) {
                instance.setKeyName(vm.getProviderKeypairId());
            }
            // TODO: instance.setStateReason(vm.)
            // TODO: rootDeviceType/Name
            // TODO: virtualizationType
            // TODO: Client token?
            // TODO: hypervisor

            reserve.addInstance(instance);
            result.addReservations(reserve);
        }
        return result.buildPartial();
    }

    // Creates a map to use with Java reflection in order to check filters
    private Map<String, String> createFilterNameMap() {
        final Map<String, String> filterNameMap = new HashMap<String, String>();
        filterNameMap.put("architecture", "architecture");
        filterNameMap.put("availability-zone", "providerRegionId");
        filterNameMap.put("dns-name", "publicDnsAddress");
        // TODO: filterNameMap.put("group-id", value)
        // TODO: filterNameMap.put("group-name", value)
        filterNameMap.put("image-id", "providerMachineImageId");
        filterNameMap.put("instance-id", "providerVirtualMachineId");
        filterNameMap.put("instance-state-name", "currentState");
        filterNameMap.put("instance-state-code", "currentState");
        filterNameMap.put("instance-type", "productId");
        filterNameMap.put("ip-address", "publicIpAddresses");
        filterNameMap.put("kernel-id", "kernelId");
        // TODO: filterNameMap.put("key-name", "keyName");
        filterNameMap.put("launch-time", "creationTimestamp");
        // TODO: filterNameMap.put("monitoring-state", "")
        filterNameMap.put("owner-id", "providerOwnerId");
        // filterNameMap.put("placement-group-name", );
        filterNameMap.put("platform", "platform");
        filterNameMap.put("private-dns-name", "privateDnsAddress");
        filterNameMap.put("private-ip-address", "privateIpAddresses");
        filterNameMap.put("ramdisk-id", "ramdiskId");
        // TODO: filterNameMap.put("reason", "state.reason"
        // TODO: filterNameMap.put("root-device-name", value)
        // TODO: filterNameMap.put("root-device-type", value);
        // TODO: filterNameMap.put("state-reason-code", value);
        // TODO: filterNameMap.put("state-reason-message");
        filterNameMap.put("tag-key", "tags");
        filterNameMap.put("tag-value", "tags");
        // TODO: filterNameMap.put("virtualization-type", value);
        // TODO: filterNameMap.put("hypervisor", value);
        return filterNameMap;
    }
}
