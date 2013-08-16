package com.transcend.compute.actions;

import java.util.Map;

import com.generationjava.io.xml.XMLNode;
import com.msi.tough.query.AbstractQueuedAction;
import com.msi.tough.query.ErrorResponse;
import com.msi.tough.query.QueryUtil;
import com.msi.tough.query.ServiceRequest;
import com.msi.tough.query.ServiceRequestContext;
import com.msi.tough.query.ServiceResponse;
import com.transcend.compute.message.AllocateAddressMessage.AllocateAddressRequestMessage;
import com.transcend.compute.message.AllocateAddressMessage.AllocateAddressResponseMessage;
import com.yammer.metrics.core.Meter;

public class AllocateAddress extends
    AbstractQueuedAction<AllocateAddressRequestMessage,
    AllocateAddressResponseMessage> {

	private static Map<String, Meter> meters = initMeter("Compute",
			"AllocateAddress");

	@Override
	protected void mark(Object ret,
	        Exception e) {
		markStandard(meters, e);
	}

    /* (non-Javadoc)
     * @see com.msi.tough.query.AbstractQueuedAction#handleRequest(com.msi.tough.query.ServiceRequest, com.msi.tough.query.ServiceRequestContext)
     */
    @Override
    public AllocateAddressRequestMessage handleRequest(
            ServiceRequest req, ServiceRequestContext context)
            throws ErrorResponse {
        final AllocateAddressRequestMessage requestMessage = unmarshall(req
                .getParameterMap());
        return requestMessage;
    }

    /* (non-Javadoc)
     * @see com.msi.tough.query.AbstractQueuedAction#buildResponse(com.msi.tough.query.ServiceResponse, com.google.protobuf.Message)
     */
    @Override
    public ServiceResponse buildResponse(
            ServiceResponse resp,
            AllocateAddressResponseMessage message) {
        resp.setPayload(marshall(resp, message));
        return resp;
    }

    public String marshall(ServiceResponse resp,
            AllocateAddressResponseMessage result) {
        final XMLNode root = new XMLNode("AllocateAddressResponse");
        root.addAttr("xmlns", "http://ec2.amazonaws.com/doc/2012-10-01/");
        QueryUtil.addNode(root, "publicIp", result.getPublicIp());
        QueryUtil.addNode(root, "requestId", result.getRequestId());
        return root.toString();
    }

	private AllocateAddressRequestMessage
	unmarshall(final Map<String, String[]> mapIn) {
	    AllocateAddressRequestMessage.Builder builder =
	            AllocateAddressRequestMessage.newBuilder();
		return builder.buildPartial();
	}


}
