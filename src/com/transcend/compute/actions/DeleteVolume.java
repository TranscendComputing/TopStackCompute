package com.transcend.compute.actions;

import java.util.Map;

import com.generationjava.io.xml.XMLNode;
import com.msi.tough.query.AbstractQueuedAction;
import com.msi.tough.query.ErrorResponse;
import com.msi.tough.query.QueryFaults;
import com.msi.tough.query.QueryUtil;
import com.msi.tough.query.ServiceRequest;
import com.msi.tough.query.ServiceRequestContext;
import com.msi.tough.query.ServiceResponse;
import com.transcend.compute.message.DeleteVolumeMessage.DeleteVolumeRequestMessage;
import com.transcend.compute.message.DeleteVolumeMessage.DeleteVolumeResponseMessage;
import com.yammer.metrics.core.Meter;

public class DeleteVolume extends AbstractQueuedAction<DeleteVolumeRequestMessage, DeleteVolumeResponseMessage> {

	private static Map<String, Meter> meters = initMeter("Compute",
			"DeleteVolume");

	@Override
	protected void mark(Object ret, Exception e) {
		markStandard(meters, e);
	}


	public String marshall(ServiceResponse resp,
            DeleteVolumeResponseMessage result) {
		final XMLNode root = new XMLNode("DeleteVolumeResponse");
		root.addAttr("xmlns", "http://ec2.amazonaws.com/doc/2012-10-01/");
		QueryUtil.addNode(root, "requestId", result.getRequestId());
		QueryUtil.addNode(root, "result", result.getReturn());
		return root.toString();
	}

	public DeleteVolumeRequestMessage unmarshall(Map<String, String[]> mapIn) {
		DeleteVolumeRequestMessage.Builder request = DeleteVolumeRequestMessage.newBuilder();
		if (!mapIn.containsKey("VolumeId")) {
			throw QueryFaults
					.MissingParameter("VolumeId is a required parameter.");
		}
		request.setVolumeId(QueryUtil.getString(mapIn, "VolumeId"));
		return request.buildPartial();
	}


	@Override
	public ServiceResponse buildResponse(ServiceResponse resp,
			DeleteVolumeResponseMessage message) {
		resp.setPayload(marshall(resp, message));
        return resp;
	}


	@Override
	public DeleteVolumeRequestMessage handleRequest(ServiceRequest req,
			ServiceRequestContext context) throws ErrorResponse {
		DeleteVolumeRequestMessage requestObj = unmarshall(req.getParameterMap());
		return requestObj;
	}

}
