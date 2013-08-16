package com.transcend.compute.utils;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineSupport;

import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.GroupIdentifier;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceBlockDeviceMapping;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.util.DateUtils;
import com.generationjava.io.xml.XMLNode;
import com.msi.tough.query.QueryUtil;
import com.transcend.compute.message.InstanceMessage;
import com.transcend.compute.message.ReservationMessage;
import com.transcend.compute.message.SecurityGroupMessage.SecurityGroup;
import com.transcend.compute.message.TerminateInstancesMessage.TerminateInstancesResponseMessage;

public class InstanceUtils {
	public static void marshallInstance(final XMLNode node, final Instance instance) {
		// QueryUtil.addNode(node, tag, volume.getAttachments());
		XMLNode instanceSet = QueryUtil.addNode(node, "instancesSet");
		XMLNode item = QueryUtil.addNode(instanceSet, "item");
		XMLNode placement = QueryUtil.addNode(item, "placement");
		QueryUtil.addNode(placement, "availabilityZone", instance.getPlacement().getAvailabilityZone());
		QueryUtil.addNode(item, "instanceId", instance.getInstanceId());
		QueryUtil.addNode(item, "imageId", instance.getImageId());
		QueryUtil.addNode(item, "instanceType", instance.getInstanceType());
		QueryUtil.addNode(item, "launchTime", instance.getLaunchTime());
	}

	public static void marshallInstances(final XMLNode node, final List<Instance> instances) {
		for(final Instance i : instances){
			//Should have something for reservation, but there doesn't seem to be any way to get in Dasein.
			final XMLNode instanceSet = QueryUtil.addNode(node, "instancesSet");
			final XMLNode item = QueryUtil.addNode(instanceSet, "item");
			QueryUtil.addNode(item, "instanceId", i.getInstanceId());
			QueryUtil.addNode(item, "architecture", i.getArchitecture());
			QueryUtil.addNode(item, "imageId", i.getImageId());

			final XMLNode instanceState = QueryUtil.addNode(item, "instanceState");
			final InstanceState state = i.getState();
			QueryUtil.addNode(instanceState, "name", state.getName());
			QueryUtil.addNode(instanceState, "code", state.getCode());
			QueryUtil.addNode(item, "privateDnsName", i.getPrivateDnsName());
			QueryUtil.addNode(item,  "dnsName",i.getPublicDnsName());
			QueryUtil.addNode(item,  "instanceType", i.getInstanceType());
			QueryUtil.addNode(item,  "launchTime", i.getLaunchTime());

			final XMLNode placement = QueryUtil.addNode(item, "placement");
			QueryUtil.addNode(placement, "availabilityZone", i.getPlacement().getAvailabilityZone());
			QueryUtil.addNode(item, "ipAddress", i.getPublicIpAddress());
			QueryUtil.addNode(item,  "privateIpAddress", i.getPrivateIpAddress());

			final XMLNode groupSet = QueryUtil.addNode(item,  "groupSet");
			for(GroupIdentifier g : i.getSecurityGroups()){
				final XMLNode group = QueryUtil.addNode(groupSet, "item");
				QueryUtil.addNode(group, "groupId", g.getGroupId());
				QueryUtil.addNode(group, "groupName", g.getGroupName());
			}

			final XMLNode tagSet = QueryUtil.addNode(item, "tagSet");
			for(Tag t : i.getTags()){
				final XMLNode tagItem = QueryUtil.addNode(tagSet, "item");
				QueryUtil.addNode(tagItem, "key", t.getKey());
				QueryUtil.addNode(tagItem, "value", t.getValue());
			}
			final XMLNode blockDeviceMapping = QueryUtil.addNode(item, "blockDeviceMapping");
			for(InstanceBlockDeviceMapping b : i.getBlockDeviceMappings()){
				final XMLNode blockDeviceItem = QueryUtil.addNode(blockDeviceMapping, "item");
				QueryUtil.addNode(blockDeviceItem, "deviceName", b.getDeviceName());
				final XMLNode ebs = QueryUtil.addNode(blockDeviceItem, "ebs");
				QueryUtil.addNode(ebs, "volumeId", b.getEbs().getVolumeId());
				QueryUtil.addNode(ebs, "status", b.getEbs().getStatus());
			}
			QueryUtil.addNode(item,  "platform", i.getPlatform());
			QueryUtil.addNode(item,  "subnetId", i.getSubnetId());
		}
	}

    public static void marshallInstances2(final XMLNode node,
            final List<InstanceMessage.Instance> instances) {
        for(final InstanceMessage.Instance i : instances){
            //Should have something for reservation, but there doesn't seem to be any way to get in Dasein.
            final XMLNode instanceSet = QueryUtil.addNode(node, "instancesSet");
            final XMLNode item = QueryUtil.addNode(instanceSet, "item");
            QueryUtil.addNode(item, "instanceId", i.getInstanceId());
            QueryUtil.addNode(item, "architecture", i.getArchitecture());
            QueryUtil.addNode(item, "imageId", i.getImageId());

            final XMLNode instanceState = QueryUtil.addNode(item, "instanceState");
            final com.transcend.compute.message.InstanceMessage.Instance.InstanceState state = i.getState();
            if (state != null) {
                QueryUtil.addNode(instanceState, "name", state.getName());
                QueryUtil.addNode(instanceState, "code", state.getCode());
            }
            QueryUtil.addNode(item, "privateDnsName", i.getPrivateDnsName());
            QueryUtil.addNode(item,  "dnsName",i.getPublicDnsName());
            QueryUtil.addNode(item,  "instanceType", i.getType());
            QueryUtil.addNode(item,  "launchTime", i.getLaunchTime());

            final XMLNode placement = QueryUtil.addNode(item, "placement");
            if (i.getPlacement() != null) {
                QueryUtil.addNode(placement, "availabilityZone",
                    i.getPlacement().getAvailabilityZone());
            }
            QueryUtil.addNode(item, "ipAddress", i.getPublicIp());
            QueryUtil.addNode(item,  "privateIpAddress", i.getPrivateIp());

            final XMLNode groupSet = QueryUtil.addNode(item,  "groupSet");
            if (i.getGroupList() != null) {
                for(SecurityGroup g : i.getGroupList()){
                    final XMLNode group = QueryUtil.addNode(groupSet, "item");
                    QueryUtil.addNode(group, "groupId", g.getGroupId());
                    QueryUtil.addNode(group, "groupName", g.getGroupName());
                }
            }
            final XMLNode tagSet = QueryUtil.addNode(item, "tagSet");
            if (i.getTagList() != null) {
                for(com.transcend.compute.message.InstanceMessage.Instance.Tag t : i.getTagList()){
                    final XMLNode tagItem = QueryUtil.addNode(tagSet, "item");
                    QueryUtil.addNode(tagItem, "key", t.getKey());
                    QueryUtil.addNode(tagItem, "value", t.getValue());
                }
            }
            final XMLNode blockDeviceMapping = QueryUtil.addNode(item, "blockDeviceMapping");
            /*TODO:
            for(InstanceBlockDeviceMapping b : i.getBlockDeviceMappings()){
                final XMLNode blockDeviceItem = QueryUtil.addNode(blockDeviceMapping, "item");
                QueryUtil.addNode(blockDeviceItem, "deviceName", b.getDeviceName());
                final XMLNode ebs = QueryUtil.addNode(blockDeviceItem, "ebs");
                QueryUtil.addNode(ebs, "volumeId", b.getEbs().getVolumeId());
                QueryUtil.addNode(ebs, "status", b.getEbs().getStatus());
            }*/
            QueryUtil.addNode(item,  "platform", i.getPlatform());
            QueryUtil.addNode(item,  "subnetId", i.getSubnetId());
        }
    }

	public static void marshallReservations(final XMLNode parent, final List<Reservation> reservations) {
		for(Reservation r: reservations){
			final List<Instance> instances = r.getInstances();
			final XMLNode reservationSet= QueryUtil.addNode(parent, "item");
			QueryUtil.addNode(reservationSet, "ownerId", r.getOwnerId());
			InstanceUtils.marshallInstances(reservationSet, instances);
		}
	}

	public static void marshallReservations2(final XMLNode parent,
	        final List<ReservationMessage.Reservation> reservations) {
	    for(ReservationMessage.Reservation r: reservations){
	        final List<InstanceMessage.Instance> instances = r.getInstanceList();
	        final XMLNode reservationSet= QueryUtil.addNode(parent, "item");
	        QueryUtil.addNode(reservationSet, "ownerId", r.getOwnerId());
	        InstanceUtils.marshallInstances2(reservationSet, instances);
	    }
	}

	public static void marshallTerminatingInstances(final XMLNode parent,
	        List<TerminateInstancesResponseMessage.InstanceStateChange> terminatingInstances){
		for(TerminateInstancesResponseMessage.InstanceStateChange i : terminatingInstances){
			final XMLNode item = QueryUtil.addNode(parent,  "item");
			QueryUtil.addNode(item,"instanceId", i.getInstanceId());

			final XMLNode currentState = QueryUtil.addNode(item, "currentState");
			QueryUtil.addNode(currentState, "code", i.getCurrentState().getCode());
			QueryUtil.addNode(currentState, "name", i.getCurrentState().getName());

			final XMLNode previousState = QueryUtil.addNode(item,  "previousState");
			QueryUtil.addNode(previousState, "code", i.getPreviousState().getCode());
			QueryUtil.addNode(previousState, "name", i.getPreviousState().getName());

		}
	}

	public static boolean checkFilters(final VirtualMachineSupport vmServ,
			final VirtualMachine vm,
			final List<Filter> filters,
			final Map<String, String> filterNameMap) throws Exception{

		if(filters == null || filters.size() == 0){
			return true;
		}
		boolean match = true;

		filterLoop:
			for(final Filter f: filters){
				//if last filter didn't match, then return false
				if(!match)
					return false;
				match = false;
				if(f.getName().startsWith("tag:")){
					for(final String filterValue : f.getValues()){
						String key = f.getName().substring(4);
						if(vm.getTags().containsKey(key)){
							if(vm.getTags().get(key).equals(filterValue)){
								match = true;
								continue filterLoop;
							}
						}
					}
				}
				else{
					Object o = PropertyUtils.getProperty(vm, filterNameMap.get(f.getName()));
					for(final String filterValue : f.getValues()){
						if(f.getName().equals("launch-time") ){
							final Date timestamp = new Date((Long)o);
							if(filterValue.equals(new DateUtils().formatIso8601Date(timestamp))){
								match=true;
								continue filterLoop;
							}
						}
						else if(f.getName().equals("instance-state-code") && filterValue.equals(getStateCode(o.toString().toLowerCase()))){
							match = true;
							continue filterLoop;
						}
						else if(f.getName().equals("ip-address") || f.getName().equals("private-ip-address")){
							final String[] ipAddresses = (String[])o;
							if(filterValue.equals(ipAddresses[0])){
								match = true;
								continue filterLoop;
							}
						}
						else if(f.getName().equals("instance-type")){
							if(filterValue.equals(vmServ.getProduct(vm.getProductId()).getName())){
								match = true;
								continue filterLoop;
							}
						}
						else if(f.getName().equals("tag-key") && (vm.getTags().containsKey(filterValue) || vm.getName() != null)){
							match = true;
							continue filterLoop;
						}
						else if(f.getName().equals("tag-value") && (vm.getTags().containsValue(filterValue) || filterValue.equals(vm.getName()))){
							match = true;
							continue filterLoop;
						}

						else{
							if(o != null && o.toString().equals(filterValue)){
								match = true;
								continue filterLoop;
							}
						}
					}
				}
			}
		//return match;

		return match;
	}

	public static Integer getStateCode(String state){
		if(state.toLowerCase().equals("pending"))
			return 0;
		else if(state.toLowerCase().equals("running"))
			return 16;
		else if(state.toLowerCase().equals("shutting-down"))
			return 32;
		else if(state.toLowerCase().equals("terminated"))
			return 48;
		else if(state.toLowerCase().equals("stopping"))
			return 64;
		else if(state.toLowerCase().equals("stopped"))
			return 80;
		return -1;
	}

}
