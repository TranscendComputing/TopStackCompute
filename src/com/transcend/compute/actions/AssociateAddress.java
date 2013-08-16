package com.transcend.compute.actions;

import java.util.Map;

import org.slf4j.Logger;

import com.generationjava.io.xml.XMLNode;
import com.msi.tough.core.Appctx;
import com.msi.tough.query.AbstractQueuedAction;
import com.msi.tough.query.ErrorResponse;
import com.msi.tough.query.QueryFaults;
import com.msi.tough.query.QueryUtil;
import com.msi.tough.query.ServiceRequest;
import com.msi.tough.query.ServiceRequestContext;
import com.msi.tough.query.ServiceResponse;
import com.transcend.compute.message.AssociateAddressMessage.AssociateAddressRequest;
import com.transcend.compute.message.AssociateAddressMessage.AssociateAddressResponse;
import com.yammer.metrics.core.Meter;

public class AssociateAddress
		extends AbstractQueuedAction<AssociateAddressRequest,
			AssociateAddressResponse> {
	private static final String TAG = AssociateAddress.class.getCanonicalName();

	private final Logger logger = Appctx.getLogger(AssociateAddress.class
			.getName());

	private static Map<String, Meter> meters = initMeter("Compute",
			"AssociateAddress");

	private Logger logDebug(String msg) {
		if (logger.isDebugEnabled()) {
			logger.debug(TAG + ": " +  msg);
		}
		return logger;
	}

	@Override
	protected void mark(Object ret, Exception e) {
		markStandard(meters, e);
	}

	/* (non-Javadoc)
     * @see com.msi.tough.query.AbstractQueuedAction#handleRequest(com.msi.tough.query.ServiceRequest, com.msi.tough.query.ServiceRequestContext)
     */
    @Override
	public AssociateAddressRequest handleRequest(ServiceRequest req,
			ServiceRequestContext context) throws ErrorResponse {
        final AssociateAddressRequest requestMessage = unmarshall(
        		req.getParameterMap());
        return requestMessage;
	}

    /* (non-Javadoc)
     * @see com.msi.tough.query.AbstractQueuedAction#buildResponse(com.msi.tough.query.ServiceResponse, com.google.protobuf.Message)
     */
	@Override
	public ServiceResponse buildResponse(ServiceResponse resp,
			AssociateAddressResponse message) {
		resp.setPayload(marshall(resp, message));
		return resp;
	}

    public String marshall(ServiceResponse resp,
    		AssociateAddressResponse result) {
		final XMLNode root = new XMLNode("AssociateAddressResponse");
		root.addAttr("xmlns", "http://ec2.amazonaws.com/doc/2012-10-01/");

		QueryUtil.addNode(root, "return", true);
		QueryUtil.addNode(root, "requestId", result.getRequestId());

		String marshalled = root.toString();
		this.logDebug(marshalled);
		return marshalled;
	}

	private AssociateAddressRequest unmarshall(final Map<String, String[]> mapIn) {
		AssociateAddressRequest.Builder builder =
				AssociateAddressRequest.newBuilder();

		/*
		 * TODO: EC2-CLASSIC ONLY -- NEED VPC SUPPORT
		 */
		if (!mapIn.containsKey("PublicIp")) {
			throw QueryFaults
					.MissingParameter("PublicIp is a required parameter.");
		}
		if (!mapIn.containsKey("InstanceId")) {
			throw QueryFaults
					.MissingParameter("InstanceId is a required parameter.");
		}
		builder.setPublicIp(QueryUtil.getString(mapIn, "PublicIp"));
		builder.setInstanceId(QueryUtil.getString(mapIn, "InstanceId"));

		/* If execution arrives here processing caused no exceptions. */
		return builder.buildPartial();
	}
}