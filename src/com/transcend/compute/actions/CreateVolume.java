package com.transcend.compute.actions;

import java.util.Map;

import com.generationjava.io.xml.XMLNode;
import com.msi.tough.query.AbstractQueuedAction;
import com.msi.tough.query.ErrorResponse;
import com.msi.tough.query.QueryUtil;
import com.msi.tough.query.ServiceRequest;
import com.msi.tough.query.ServiceRequestContext;
import com.msi.tough.query.ServiceResponse;
import com.transcend.compute.message.CreateVolumeMessage.CreateVolumeRequestMessage;
import com.transcend.compute.message.CreateVolumeMessage.CreateVolumeResponseMessage;
import com.yammer.metrics.core.Meter;

public class CreateVolume extends AbstractQueuedAction<CreateVolumeRequestMessage, CreateVolumeResponseMessage> {

	private static Map<String, Meter> meters = initMeter("Compute",
			"CreateVolume");

	@Override
	protected void mark(Object ret, Exception e) {
		markStandard(meters, e);
	}

	@Override
	public ServiceResponse buildResponse(ServiceResponse resp,
			CreateVolumeResponseMessage message) {
		resp.setPayload(marshall(resp, message));
        return resp;
	}

	@Override
	public CreateVolumeRequestMessage handleRequest(ServiceRequest req,
			ServiceRequestContext context) throws ErrorResponse {
		CreateVolumeRequestMessage result = unmarshall(req.getParameterMap());
		return result;
	}

	public String marshall(ServiceResponse resp,
            CreateVolumeResponseMessage result) {
		final XMLNode root = new XMLNode("CreateVolumeResponse");
		root.addAttr("xmlns", "http://ec2.amazonaws.com/doc/2012-10-01/");
		QueryUtil.addNode(root, "requestId", result.getRequestId());
		QueryUtil.addNode(root, "volumeId", result.getVolumeId());
		QueryUtil.addNode(root, "size", result.getSize());
		QueryUtil.addNode(root, "snapshotId", result.getSnapshotId());
		QueryUtil.addNode(root, "availabilityZone", result.getAvailabilityZone());
		QueryUtil.addNode(root, "status", result.getStatus());
		QueryUtil.addNode(root, "createTime", result.getCreateTime());
		QueryUtil.addNode(root, "volumeType", result.getVolumeType());
		if(result.getIops() != 0){
			QueryUtil.addNode(root, "iops", result.getIops());
		}
		return root.toString();
	}

	private CreateVolumeRequestMessage unmarshall(final Map<String, String[]> in) {
		CreateVolumeRequestMessage.Builder builder = CreateVolumeRequestMessage.newBuilder();
		builder.setAvailabilityZone(QueryUtil
				.requiredString(in, "AvailabilityZone"));
		builder.setIops(QueryUtil.getInt(in, "Iops", -1));
		builder.setSize(QueryUtil.getInt(in, "Size", -1));
		if(QueryUtil.getString(in, "SnapshotId") != null){
			builder.setSnapshotId(QueryUtil.getString(in, "SnapshotId"));
		}
		if(QueryUtil.getString(in, "VolumeType") != null){
			builder.setVolumeType(QueryUtil.getString(in, "VolumeType"));
		}
		return builder.buildPartial();
	}
}
