package com.transcend.compute.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineSupport;
import org.dasein.cloud.network.Firewall;
import org.hibernate.Session;

import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.EbsInstanceBlockDevice;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.GroupIdentifier;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceBlockDeviceMapping;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.Placement;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Tag;
import com.generationjava.io.xml.XMLNode;
import com.msi.tough.dasein.DaseinHelper;
import com.msi.tough.model.AccountBean;
import com.msi.tough.query.AbstractAction;
import com.msi.tough.query.QueryUtil;
import com.transcend.compute.utils.ComputeFaults;
import com.transcend.compute.utils.InstanceUtils;
import com.yammer.metrics.core.Meter;

public class DescribeInstances extends AbstractAction<DescribeInstancesResult> {

	private static Map<String, Meter> meters = initMeter("Compute",
			"DescribeInstances");

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

	@Override
	protected void mark(DescribeInstancesResult ret, Exception e) {
		markStandard(meters, e);
	}

	@Override
	public String marshall(
			final com.msi.tough.query.MarshallStruct<DescribeInstancesResult> in,
			final HttpServletResponse resp) throws Exception {
		final DescribeInstancesResult result = in.getMainObject();

		final XMLNode root = new XMLNode("DescribeInstancesResponse");
		root.addAttr("xmlns", "http://ec2.amazonaws.com/doc/2012-12-01/");
		QueryUtil.addNode(root, "requestId", in.getRequestId());
		final XMLNode reservationSet = QueryUtil
				.addNode(root, "reservationSet");
		InstanceUtils.marshallReservations(reservationSet,
				result.getReservations());
		return root.toString();
	}

	@Override
	public DescribeInstancesResult process0(final Session session,
			final HttpServletRequest httpRequest,
			final HttpServletResponse httpResponse,
			final Map<String, String[]> mapIn) throws Exception {

		// unmarshall request XML into a java bean
		final DescribeInstancesRequest request = unmarshall(mapIn);
		final DescribeInstancesResult result = new DescribeInstancesResult();

		final AccountBean account = getAccountBean();
		final CloudProvider cloudProvider = DaseinHelper.getProvider(
				account.getDefZone(), account.getTenant(),
				account.getAccessKey(), account.getSecretKey());
		final ComputeServices comp = cloudProvider.getComputeServices();

		final VirtualMachineSupport vmServ = comp.getVirtualMachineSupport();

		for (final String vmId : request.getInstanceIds()) {
			if (vmServ.getVirtualMachine(vmId) == null) {
				throw ComputeFaults.instanceDoesNotExist(vmId);
			}
		}
		final List<Reservation> reservations = new ArrayList<Reservation>();
		// Don't see anything about Reservations in Dasein, so for now just
		// putting each instance in its own reservation set
		for (final VirtualMachine vm : vmServ.listVirtualMachines()) {
			final String vmId = vm.getProviderVirtualMachineId();
			if (request.getInstanceIds() != null
					&& request.getInstanceIds().size() > 0) {
				if (!request.getInstanceIds().contains(vmId)) {
					continue;
				}
			}
			// check to see if vm matches filters, if not then move on to next
			// vm
			if (!InstanceUtils.checkFilters(vmServ, vm, request.getFilters(),
					createFilterNameMap())) {
				continue;
			}
			final Instance instance = new Instance();
			final List<Instance> instanceList = new ArrayList<Instance>();
			final Reservation reserve = new Reservation();
			reserve.setOwnerId(vm.getProviderOwnerId());
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
				final List<GroupIdentifier> securityGroups = new ArrayList<GroupIdentifier>();
				for (String firewallId : vmServ.listFirewalls(vmId)) {
					Firewall f = cloudProvider.getNetworkServices()
							.getFirewallSupport().getFirewall(firewallId);
					GroupIdentifier group = new GroupIdentifier();
					group.setGroupName(f.getName());
					group.setGroupId(f.getProviderFirewallId());
					securityGroups.add(group);
				}
				instance.setSecurityGroups(securityGroups);
			}
			{
				final InstanceState state = new InstanceState();
				state.setName(vm.getCurrentState().toString().toLowerCase());
				state.setCode(InstanceUtils.getStateCode(vm.getCurrentState()
						.toString().toLowerCase()));
				instance.setState(state);
			}
			{
				final Placement placement = new Placement();
				placement.setAvailabilityZone(cloudProvider
						.getDataCenterServices()
						.getRegion(vm.getProviderRegionId()).getName());
				instance.setPlacement(placement);
			}

			instance.setPrivateDnsName(vm.getPrivateDnsAddress());
			instance.setPublicDnsName(vm.getPublicDnsAddress());
			// uses productId() to get the actual name like "m1.medium"
			instance.setInstanceType(vmServ.getProduct(vm.getProductId())
					.getName());
			instance.setLaunchTime(new Date(vm.getCreationTimestamp()));
			instance.setSubnetId(vm.getProviderSubnetId());
			// AWS Instance only supports a single string. For now, we'll just
			// return the first IP given.
			if (vm.getPrivateIpAddresses() != null
					&& vm.getPrivateIpAddresses().length != 0) {
				instance.setPrivateIpAddress(vm.getPrivateIpAddresses()[0]);
			}
			if (vm.getPublicIpAddresses() != null
					&& vm.getPublicIpAddresses().length != 0) {
				instance.setPublicIpAddress(vm.getPublicIpAddresses()[0]);
			}
			instance.setPlatform(vm.getPlatform().toString());

			final List<Tag> tags = new ArrayList<Tag>();
			{
				final Tag nameTag = new Tag();
				nameTag.setKey("name");
				nameTag.setValue(vm.getName());
				tags.add(nameTag);
			}
			for (String key : vm.getTags().keySet()) {
				final Tag tag = new Tag();
				tag.setKey(key);
				tag.setValue(vm.getTags().get(key));
				tags.add(tag);
			}
			instance.setTags(tags);
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

			instance.setBlockDeviceMappings(blockDeviceMappings);
			instance.setKeyName(vm.getProviderKeypairId());
			// TODO: instance.setStateReason(vm.)
			// TODO: rootDeviceType/Name
			// TODO: virtualizationType
			// TODO: Client token?
			// TODO: hypervisor
			instanceList.add(instance);
			reserve.setInstances(instanceList);
			reservations.add(reserve);
		}
		result.setReservations(reservations);
		return result;
	}

	private DescribeInstancesRequest unmarshall(Map<String, String[]> in) {
		final DescribeInstancesRequest req = new DescribeInstancesRequest();
		{
			final Collection<String> ids = new ArrayList<String>();
			for (int i = 1;; ++i) {
				if (!in.containsKey("InstanceId." + i)) {
					break;
				}
				ids.add(QueryUtil.getString(in, "InstanceId." + i));
			}
			req.setInstanceIds(ids);
		}
		{
			final String s = "Filter.";
			final List<Filter> filters = new ArrayList<Filter>();
			for (int i = 1;; i++) {
				if (!in.containsKey(s + i + ".Name")) {
					break;
				}
				final List<String> ids = new ArrayList<String>();
				for (int m = 1;; m++) {
					if (!in.containsKey(s + i + ".Value." + m)) {
						break;
					}
					ids.add(QueryUtil.getString(in, s + i + ".Value." + m));
				}
				filters.add(new Filter(
						QueryUtil.getString(in, s + i + ".Name"), ids));
				QueryUtil.getString(in, s + i);
			}
			req.setFilters(filters);
		}
		return req;
	}

}
