package com.transcend.compute.actions;

import java.util.Map;

import org.slf4j.Logger;

import com.generationjava.io.xml.XMLNode;
import com.msi.tough.core.Appctx;
import com.msi.tough.query.AbstractQueuedAction;
import com.msi.tough.query.ErrorResponse;
import com.msi.tough.query.QueryUtil;
import com.msi.tough.query.ServiceRequest;
import com.msi.tough.query.ServiceRequestContext;
import com.msi.tough.query.ServiceResponse;
import com.transcend.compute.message.AttachVolumeMessage.AttachVolumeRequest;
import com.transcend.compute.message.AttachVolumeMessage.AttachVolumeResponse;
import com.yammer.metrics.core.Meter;

public class AttachVolume extends
        AbstractQueuedAction<AttachVolumeRequest, AttachVolumeResponse> {

    private final Logger logger = Appctx
            .getLogger(AttachVolume.class.getName());

    private static Map<String, Meter> meters = initMeter("Compute",
            "AttachVolume");

    @Override
    protected void mark(Object ret, Exception e) {
        markStandard(meters, e);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.msi.tough.query.AbstractQueuedAction#handleRequest(com.msi.tough.
     * query.ServiceRequest, com.msi.tough.query.ServiceRequestContext)
     */
    @Override
    public AttachVolumeRequest handleRequest(ServiceRequest req,
            ServiceRequestContext context) throws ErrorResponse {
        final AttachVolumeRequest requestMessage = unmarshall(req
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
            AttachVolumeResponse message) {
        resp.setPayload(marshall(resp, message));
        return resp;
    }

    public String marshall(ServiceResponse resp, AttachVolumeResponse result) {
        final XMLNode root = new XMLNode("AttachVolumeResponse");
        root.addAttr("xmlns", "http://ec2.amazonaws.com/doc/2012-10-01/");

        QueryUtil.addNode(root, "requestId", result.getRequestId());
        QueryUtil.addNode(root, "volumeId", result.getVolumeId());
        QueryUtil.addNode(root, "instanceId", result.getInstanceId());
        QueryUtil.addNode(root, "device", result.getDevice());
        QueryUtil.addNode(root, "status",
                result.getStatus().toString().toLowerCase());
        QueryUtil.addNode(root, "attachTime", result.getAttachTime());

        String marshalled = root.toString();
        logger.debug(marshalled);
        return marshalled;
    }

    private AttachVolumeRequest unmarshall(final Map<String, String[]> mapIn) {
        AttachVolumeRequest.Builder req = AttachVolumeRequest
                .newBuilder();

        req.setVolumeId(QueryUtil.requiredString(mapIn, "VolumeId"));
        req.setInstanceId(QueryUtil.requiredString(mapIn, "InstanceId"));
        req.setDevice(QueryUtil.requiredString(mapIn, "Device"));

        return req.buildPartial();
    }
}