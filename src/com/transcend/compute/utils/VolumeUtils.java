package com.transcend.compute.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;

import com.amazonaws.util.DateUtils;
import com.generationjava.io.xml.XMLNode;
import com.msi.tough.query.QueryUtil;
import com.transcend.compute.message.DescribeVolumesMessage.DescribeVolumesRequestMessage.Filter;
import com.transcend.compute.message.VolumeMessage.Volume;
import com.transcend.compute.message.VolumeMessage.Volume.Attachment;

public class VolumeUtils {
	public static void marshallVolume(final XMLNode node, final Volume vol) {
		QueryUtil.addNode(node, "volumeId", vol.getVolumeId());
		QueryUtil.addNode(node, "size", vol.getSize());
		QueryUtil.addNode(node, "snapshotId", vol.getSnapshotId());
		QueryUtil.addNode(node, "availabilityZone",
				vol.getAvailabilityZone());
		String status = vol.getStatus().toString().toLowerCase();
		status = status.replace('_', '-');
		QueryUtil.addNode(node, "status", status);
		QueryUtil.addNode(node,"createTime", vol.getCreateTime());
		QueryUtil.addNode(node, "volumeType", vol.getVolumeType());

		final XMLNode attchSet = QueryUtil.addNode(node, "attachmentSet");
		for (Attachment va : vol.getAttachmentList())
		{
			QueryUtil.addNode(attchSet, "volumeId", va.getVolumeId());
			QueryUtil.addNode(attchSet, "instanceId", va.getInstanceId());
			QueryUtil.addNode(attchSet, "device", va.getDevice());
			String attachStatus = va.getStatus().toString().toLowerCase();
			QueryUtil.addNode(attchSet, "status", attachStatus);
			QueryUtil.addNode(attchSet, "attachTime", va.getAttachTime());
			QueryUtil.addNode(attchSet, "deleteOnTermination", va.getDeleteOnTermination());
		}
	}

	public static void marshallVolumes(final XMLNode node, final List<Volume> volumes) {
		for (Volume vol : volumes){
			final XMLNode item = QueryUtil.addNode(node, "item");
			marshallVolume(item, vol);
		}
	}

	public static boolean checkFilters(final org.dasein.cloud.compute.Volume v,
			final List<Filter> filters,
			final Map<String, String> filterNameMap) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException{

		if(filters == null || filters.size() == 0){
			return true;
		}
		boolean match=true;
		filterLoop:
			for(final Filter f: filters){
				//if last filter didn't match, then return false
				if(!match)
					return false;
				match = false;
				Object o = PropertyUtils.getProperty(v, filterNameMap.get(f.getName()));
				for(final String value : f.getValueList()){
					if(f.getName().equals("create-time") && (new DateUtils().formatIso8601Date(new Date((Long)o))).equals(value)){
						final Date timestamp = new Date((Long)o);
						if(value.equals(new DateUtils().formatIso8601Date(timestamp))){
							match=true;
							continue filterLoop;
						}
					}
					else{
						if(o.toString().equals(value)){
							match = true;
							continue filterLoop;
						}
					}
				}
			}
		return match;
	}
}
