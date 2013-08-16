package com.transcend.compute.actions;

import java.util.Map;

import com.generationjava.io.xml.XMLNode;
import com.msi.tough.query.AbstractQueuedAction;
import com.msi.tough.query.ErrorResponse;
import com.msi.tough.query.QueryUtil;
import com.msi.tough.query.ServiceRequest;
import com.msi.tough.query.ServiceRequestContext;
import com.msi.tough.query.ServiceResponse;
import com.transcend.compute.message.DefaultMessage.DefaultResponseMessage;
import com.transcend.compute.message.DeleteKeyPairMessage.DeleteKeyPairRequestMessage;
import com.yammer.metrics.core.Meter;

public class DeleteKeyPair extends AbstractQueuedAction<DeleteKeyPairRequestMessage,
	DefaultResponseMessage>  {

	private static final String RESPONSE_NODE = "DeleteKeyPairResponse";

	private static Map<String, Meter> meters = initMeter("Compute",
			"DeleteKeyPair");

	@Override
	protected void mark(Object ret, Exception e) {
		markStandard(meters, e);
	}


	public void marshall(ServiceResponse response,
			DefaultResponseMessage result) throws ErrorResponse {
		final XMLNode root = new XMLNode(RESPONSE_NODE);
		root.addAttr("xmlns", "http://ec2.amazonaws.com/doc/2012-12-01/");
		QueryUtil.addNode(root, "requestId", response.getRequestId());
		response.setPayload(root.toString());
	}

	private DeleteKeyPairRequestMessage unmarshall(final Map<String, String[]> mapIn) {
	    DeleteKeyPairRequestMessage.Builder builder =
	    		DeleteKeyPairRequestMessage.newBuilder();
	    builder.setKeyName(QueryUtil.getString(mapIn,"KeyName"));
		return builder.buildPartial();
	}

	@Override
	public ServiceResponse buildResponse(ServiceResponse resp,
			DefaultResponseMessage result) {
		marshall(resp, result);
		return resp;
	}

	@Override
	public DeleteKeyPairRequestMessage handleRequest(ServiceRequest req,
			ServiceRequestContext context) throws ErrorResponse {
		final DeleteKeyPairRequestMessage request = unmarshall(req
				.getParameterMap());
		return request;
	}
}
