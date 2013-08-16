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
import com.transcend.compute.message.DisassociateAddressMessage.DisassociateAddressRequestMessage;
import com.transcend.compute.message.DisassociateAddressMessage.DisassociateAddressResponseMessage;
import com.yammer.metrics.core.Meter;

public class DisassociateAddress 
extends AbstractQueuedAction<DisassociateAddressRequestMessage,
DisassociateAddressResponseMessage> {

	private static Map<String, Meter> meters = initMeter("Compute",
			"DisassociateAddress");

	@Override
	protected void mark(Object ret, Exception e) {
		markStandard(meters, e);
	}

	public String marshall(ServiceResponse resp,
    		DisassociateAddressResponseMessage result) {
		final XMLNode root = new XMLNode("DisassociateAddressResponse");
		root.addAttr("xmlns", "http://ec2.amazonaws.com/doc/2012-10-01/");
		QueryUtil.addNode(root, "return", true);
		QueryUtil.addNode(root, "requestId", result.getRequestId());
		return root.toString();
	}


	private DisassociateAddressRequestMessage unmarshall(
			final Map<String, String[]> mapIn) {
		/*
		 * TODO: implemented EC2-CLASSIC ONLY -- NEED VPC SUPPORT
		 */
		DisassociateAddressRequestMessage.Builder request = DisassociateAddressRequestMessage.newBuilder();
		if (!mapIn.containsKey("PublicIp")) {
			throw QueryFaults
					.MissingParameter("PublicIp is a required parameter.");
		}
		request.setPublicIp(QueryUtil.getString(mapIn, "PublicIp"));
		return request.buildPartial();
	}

	@Override
	public ServiceResponse buildResponse(ServiceResponse resp,
			DisassociateAddressResponseMessage message) {
		resp.setPayload(marshall(resp, message));
		return resp;
	}

	@Override
	public DisassociateAddressRequestMessage handleRequest(ServiceRequest req,
			ServiceRequestContext context) throws ErrorResponse {
		DisassociateAddressRequestMessage request = unmarshall(req.getParameterMap());
		return request;
	}

}
