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
import com.transcend.compute.message.DeleteSecurityGroupMessage.DeleteSecurityGroupRequestMessage;
import com.transcend.compute.message.DeleteSecurityGroupMessage.DeleteSecurityGroupResponseMessage;
import com.yammer.metrics.core.Meter;

public class DeleteSecurityGroup
extends AbstractQueuedAction<DeleteSecurityGroupRequestMessage, DeleteSecurityGroupResponseMessage> {

	private static Map<String, Meter> meters = initMeter("Compute",
			"DeleteSecurityGroup");

	@Override
	protected void mark(Object ret, Exception e) {
		markStandard(meters, e);
	}

	public String marshall(ServiceResponse resp,
    		DeleteSecurityGroupResponseMessage result) {
		final XMLNode root = new XMLNode("DeleteSecurityGroupResponse");
		root.addAttr("xmlns", "http://ec2.amazonaws.com/doc/2012-10-01/");
		QueryUtil.addNode(root, "requestId", result.getRequestId());

		QueryUtil.addNode(root, "result", result.getReturn());
		return root.toString();
	}

	public DeleteSecurityGroupRequestMessage unmarshall(Map<String, String[]> mapIn) {
		DeleteSecurityGroupRequestMessage.Builder builder = DeleteSecurityGroupRequestMessage.newBuilder();

		if (!mapIn.containsKey("GroupId") && !mapIn.containsKey("GroupName")) {
			throw QueryFaults
					.MissingParameter("You must specify GroupId or GroupName");
		}
		if (mapIn.containsKey("GroupId") && mapIn.containsKey("GroupName")) {
			throw QueryFaults
					.InvalidParameterCombination("GroupId and GroupName should not be specified together.");
		}

		if (mapIn.containsKey("GroupId")) {
			builder.setGroupId(QueryUtil.getString(mapIn, "GroupId"));
		} else if (mapIn.containsKey("GroupName")) {
			builder.setGroupName(QueryUtil.getString(mapIn, "GroupName"));
		}
		return builder.buildPartial();
	}

	@Override
	public ServiceResponse buildResponse(ServiceResponse resp,
			DeleteSecurityGroupResponseMessage message) {
		resp.setPayload(marshall(resp, message));
		return resp;
	}

	@Override
	public DeleteSecurityGroupRequestMessage handleRequest(ServiceRequest req,
			ServiceRequestContext context) throws ErrorResponse {
		final DeleteSecurityGroupRequestMessage requestMessage = unmarshall(
        		req.getParameterMap());
        return requestMessage;
	}
}
