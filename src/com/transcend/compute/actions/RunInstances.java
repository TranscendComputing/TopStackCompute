package com.transcend.compute.actions;

import java.util.List;
import java.util.Map;

import com.generationjava.io.xml.XMLNode;
import com.msi.tough.core.StringHelper;
import com.msi.tough.query.AbstractQueuedAction;
import com.msi.tough.query.ErrorResponse;
import com.msi.tough.query.QueryUtil;
import com.msi.tough.query.ServiceRequest;
import com.msi.tough.query.ServiceRequestContext;
import com.msi.tough.query.ServiceResponse;
import com.transcend.compute.message.InstanceMessage;
import com.transcend.compute.message.InstanceMessage.Instance.Placement;
import com.transcend.compute.message.ReservationMessage;
import com.transcend.compute.message.RunInstancesMessage.RunInstancesRequestMessage;
import com.transcend.compute.message.RunInstancesMessage.RunInstancesResponseMessage;
import com.transcend.compute.utils.InstanceUtils;
import com.yammer.metrics.core.Meter;

public class RunInstances
		extends
		AbstractQueuedAction<RunInstancesRequestMessage,
		RunInstancesResponseMessage> {

	private static Map<String, Meter> meters = initMeter("Compute",
			"RunInstances");

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.msi.tough.query.AbstractQueuedAction#buildResponse(com.msi.tough.
	 * query.ServiceResponse, com.google.protobuf.Message)
	 */
	@Override
	public ServiceResponse buildResponse(ServiceResponse resp,
			RunInstancesResponseMessage result) {
		resp.setPayload(marshall(resp, result));
		return resp;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.msi.tough.query.AbstractQueuedAction#handleRequest(com.msi.tough.
	 * query.ServiceRequest, com.msi.tough.query.ServiceRequestContext)
	 */
	@Override
	public RunInstancesRequestMessage handleRequest(ServiceRequest req,
			ServiceRequestContext context) throws ErrorResponse {
		final RunInstancesRequestMessage requestMessage = unmarshall(req
				.getParameterMap());
		return requestMessage;
	}

	@Override
	protected void mark(Object ret, Exception e) {
	    markStandard(meters, e);
	}

	// @Override
	public String marshall(ServiceResponse resp,
			RunInstancesResponseMessage result) {
		final ReservationMessage.Reservation reserve = result.getReservation();
		final List<InstanceMessage.Instance> instanceList = reserve
				.getInstanceList();
		// final InstanceMessage i = instanceList.get(0);

		final XMLNode root = new XMLNode("RunInstanceResponse");
		root.addAttr("xmlns", "http://ec2.amazonaws.com/doc/2012-10-01/");
		QueryUtil.addNode(root, "requestId", result.getRequestId());
		InstanceUtils.marshallInstances2(root, instanceList);
		return root.toString();
	}

	public RunInstancesRequestMessage unmarshall(final Map<String, String[]> in) {
		RunInstancesRequestMessage.Builder req =
		        RunInstancesRequestMessage.newBuilder();
		req.setImageId(QueryUtil.requiredString(in, "ImageId"));
		req.setInstanceType(QueryUtil.requiredString(in, "InstanceType"));
		req.setRamdiskId(StringHelper.nullToEmpty(QueryUtil.getString(in, "RamdiskId")));
		req.setKernelId(StringHelper.nullToEmpty(QueryUtil.getString(in, "KernelId")));
		req.setUserData(StringHelper.nullToEmpty(QueryUtil.getString(in, "UserData")));
		Placement.Builder placement = Placement.newBuilder();
		placement.setAvailabilityZone(QueryUtil.requiredString(in,
				"Placement.AvailabilityZone"));
		req.setPlacement(placement);
		req.setMinCount(1);
		req.setMaxCount(1);
		// req.setMinCount(Integer.valueOf(QueryUtil.requiredString(in,
		// "MinCount")));
		// req.setMaxCount(Integer.valueOf(QueryUtil.requiredString(in,
		// "MaxCount")));

		return req.buildPartial();
	}

}