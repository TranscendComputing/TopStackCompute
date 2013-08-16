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
import com.transcend.compute.message.CreateSecurityGroupMessage.CreateSecurityGroupRequest;
import com.transcend.compute.message.CreateSecurityGroupMessage.CreateSecurityGroupResponse;
import com.yammer.metrics.core.Meter;

public class CreateSecurityGroup extends
	AbstractQueuedAction<CreateSecurityGroupRequest,CreateSecurityGroupResponse> {
	private static final String TAG = CreateSecurityGroup.class.getCanonicalName();

	private final Logger logger = Appctx.getLogger(DescribeSecurityGroups.class
			.getName());

	private static Map<String, Meter> meters = initMeter("Compute",
			"CreateSecurityGroup");

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
	public CreateSecurityGroupRequest handleRequest(ServiceRequest req,
			ServiceRequestContext context) throws ErrorResponse {
        final CreateSecurityGroupRequest requestMessage = unmarshall(
        		req.getParameterMap());
        return requestMessage;
	}

    /* (non-Javadoc)
     * @see com.msi.tough.query.AbstractQueuedAction#buildResponse(com.msi.tough.query.ServiceResponse, com.google.protobuf.Message)
     */
	@Override
	public ServiceResponse buildResponse(ServiceResponse resp,
			CreateSecurityGroupResponse message) {
		resp.setPayload(marshall(resp, message));
		return resp;
	}

    public String marshall(ServiceResponse resp,
    		CreateSecurityGroupResponse result) {
		final XMLNode root = new XMLNode("CreateSecurityGroupResponse");
		root.addAttr("xmlns", "http://ec2.amazonaws.com/doc/2012-10-01/");
		QueryUtil.addNode(root, "requestId", result.getRequestId());
		QueryUtil.addNode(root, "groupId", result.getGroupId());

		// Not 100% sure what AWS means this value to represent, but it looks
		// like it will never be false since an error will be thrown otherwise.
		QueryUtil.addNode(root, "result", true);

		String marshalled = root.toString();
		this.logDebug(marshalled);
		return marshalled;
	}

	public CreateSecurityGroupRequest unmarshall(final Map<String, String[]> in) {
		CreateSecurityGroupRequest.Builder builder =
				CreateSecurityGroupRequest.newBuilder();

		if (!in.containsKey("GroupName") || in.get("GroupName") == null
				|| "".equals(in.get("GroupName"))) {
			String msg = "GroupName is a required parameter.";
			this.logDebug(msg);
			throw QueryFaults.MissingParameter(msg);
		} else {
			builder.setGroupName(in.get("GroupName")[0]);
		}
		if (!in.containsKey("GroupDescription")
				|| in.get("GroupDescription") == null
				|| "".equals(in.get("GroupDescription"))) {
			String msg = "GroupName is a required parameter.";
			this.logDebug(msg);
			throw QueryFaults.MissingParameter(msg);
		} else {
			builder.setGroupDescription(in.get("GroupDescription")[0]);
		}

		/* If execution arrives here processing caused no exceptions. */
		return builder.buildPartial();
	}
}
