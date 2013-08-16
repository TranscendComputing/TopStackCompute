package com.transcend.compute.actions;

import java.util.Map;

import com.generationjava.io.xml.XMLNode;
import com.msi.tough.query.AbstractQueuedAction;
import com.msi.tough.query.ErrorResponse;
import com.msi.tough.query.QueryUtil;
import com.msi.tough.query.ServiceRequest;
import com.msi.tough.query.ServiceRequestContext;
import com.msi.tough.query.ServiceResponse;
import com.transcend.compute.message.CreateKeyPairMessage.CreateKeyPairRequestMessage;
import com.transcend.compute.message.CreateKeyPairMessage.CreateKeyPairResponseMessage;
import com.yammer.metrics.core.Meter;

public class CreateKeyPair extends AbstractQueuedAction<CreateKeyPairRequestMessage,
CreateKeyPairResponseMessage>  {

	private static final String RESPONSE_NODE = "CreateKeyPairResponse";

	private static Map<String, Meter> meters = initMeter("Compute",
			"CreateKeyPair");

	@Override
	protected void mark(Object ret, Exception e) {
		markStandard(meters, e);
	}


	public void marshall(ServiceResponse response,
			CreateKeyPairResponseMessage result) throws ErrorResponse {
		final XMLNode root = new XMLNode(RESPONSE_NODE);
		root.addAttr("xmlns", "http://ec2.amazonaws.com/doc/2012-12-01/");
		QueryUtil.addNode(root, "requestId", response.getRequestId());
		QueryUtil.addNode(root, "keyFingerprint", result.getKeyFingerprint());
		QueryUtil.addNode(root, "keyName", result.getKeyName());
		QueryUtil.addNode(root, "keyMaterial", result.getKeyMaterial());
		response.setPayload(root.toString());
	}

	private CreateKeyPairRequestMessage unmarshall(final Map<String, String[]> mapIn) {
	    CreateKeyPairRequestMessage.Builder builder =
	    		CreateKeyPairRequestMessage.newBuilder();
	    builder.setKeyName(QueryUtil.getString(mapIn,"KeyName"));
		return builder.buildPartial();
	}

	@Override
	public ServiceResponse buildResponse(ServiceResponse resp,
			CreateKeyPairResponseMessage result) {
		marshall(resp, result);
		return resp;
	}

	@Override
	public CreateKeyPairRequestMessage handleRequest(ServiceRequest req,
			ServiceRequestContext context) throws ErrorResponse {
		final CreateKeyPairRequestMessage request = unmarshall(req
				.getParameterMap());
		return request;
	}
}
