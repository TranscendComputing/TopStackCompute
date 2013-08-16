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
import com.transcend.compute.message.TerminateInstancesMessage.TerminateInstancesRequestMessage;
import com.transcend.compute.message.TerminateInstancesMessage.TerminateInstancesResponseMessage;
import com.transcend.compute.utils.InstanceUtils;

public class TerminateInstances
        extends
        AbstractQueuedAction<TerminateInstancesRequestMessage, TerminateInstancesResponseMessage> {

    /*
     * (non-Javadoc)
     *
     * @see
     * com.msi.tough.query.AbstractQueuedAction#handleRequest(com.msi.tough.
     * query.ServiceRequest, com.msi.tough.query.ServiceRequestContext)
     */
    @Override
    public TerminateInstancesRequestMessage handleRequest(ServiceRequest req,
            ServiceRequestContext context) throws ErrorResponse {
        final TerminateInstancesRequestMessage requestMessage = unmarshall(req
                .getParameterMap());
        return requestMessage;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.msi.tough.query.AbstractQueuedAction#buildResponse(com.msi.tough.
     * query.ServiceResponse, com.google.protobuf.Message)
     */
    @Override
    public ServiceResponse buildResponse(ServiceResponse resp,
            TerminateInstancesResponseMessage result) {
        resp.setPayload(marshall(resp, result));
        return resp;
    }

    public String marshall(ServiceResponse resp,
            TerminateInstancesResponseMessage result) {
        final XMLNode root = new XMLNode("TerminateInstancesResponse");
        root.addAttr("xmlns", "http://ec2.amazonaws.com/doc/2012-10-01/");
        QueryUtil.addNode(root, "requestId", result.getRequestId());
        final XMLNode instancesSet = QueryUtil.addNode(root, "instancesSet");
        InstanceUtils.marshallTerminatingInstances(instancesSet,
                result.getTerminatingInstancesList());
        return root.toString();
    }

    public TerminateInstancesRequestMessage unmarshall(final Map<String, String[]> in) {
        final TerminateInstancesRequestMessage.Builder req =
                TerminateInstancesRequestMessage.newBuilder();
        {
            for (int i = 1;; ++i) {
                if (!in.containsKey("InstanceId." + i)) {
                    break;
                }
                req.addInstanceIds(QueryUtil.getString(in, "InstanceId." + i));
            }
        }
        if (req.getInstanceIdsCount()  == 0) {
            throw QueryFaults
                    .MissingParameter("At least 1 InstanceId must be specified.");
        }
        return req.buildPartial();
    }

}
