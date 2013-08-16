package com.msi.compute.helper;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.msi.tough.query.ActionRequest;
import com.msi.tough.query.ActionTestHelper;
import com.transcend.compute.message.AssociateAddressMessage.AssociateAddressRequest;
import com.transcend.compute.message.AssociateAddressMessage.AssociateAddressResponse;
import com.transcend.compute.worker.AssociateAddressWorker;

/**
 * Address helper for non-web tests (using actions in-VM).
 *
 */
@Component
public class AssociateAddressLocalHelper {

    private static List<String> mAssociations = new ArrayList<String>();

    private static ActionTestHelper mActionHelper = null;

    private static AssociateAddressWorker mAssociateAddressWorker = null;

    /**
     * Construct a minimal valid associate address request.
     *
     * @return
     */
    public static AssociateAddressRequest associateAddressRequest(
    		final String publicIp, final String instanceId) {
        final AssociateAddressRequest.Builder builder =
        		AssociateAddressRequest.newBuilder();
        builder.setTypeId(true);
        builder.setCallerAccessKey(mActionHelper.getAccessKey());
        builder.setRequestId("test");
        builder.setInstanceId(instanceId);
        builder.setPublicIp(publicIp);

        return builder.build();
    }

    /**
     * Put a metric address (don't care about the details, just need one.
     *
     * @param addressName
     */
    public static boolean associateAddress(final String publicIp,
    		final String instanceId) throws Exception {
        AssociateAddressRequest req = associateAddressRequest(
        		publicIp, instanceId);
        AssociateAddressResponse response = null;
        response = mAssociateAddressWorker.doWork(req);

        /* TODO: This should detect if VPC is in use -- if so use associationId */
        if (response.getReturn()) {
        	mAssociations.add(req.getPublicIp());
        } else {
        	throw new Exception("Assiciate Address Failed wit publicIp/instanceId: "
        			+ publicIp + "/" + instanceId);
		}

        return true;
}

    /**
     * Release an address with the given name.
     *
     * @param ip
     * @param client
     *//*
    public static void disassociateAddress(String addr) throws Exception {
    	final DisassociateAddressActionRequest request = new DisassociateAddressActionRequest();
    	DisassociateAddress disassociateAddresses = new DisassociateAddress();
        request.withPublicIp(addr);
        mActionHelper.invokeProcess(disassociateAddresses,
                request.getRequest(), request.getResponse(),
                request.getMap());
    }

    *//**
     * Delete an address with the given name.
     *
     * @param addressName
     * @param client
     *//*
    public void disassociateAllAssociatedAddresses() throws Exception {
        final DisassociateAddressActionRequest request = new DisassociateAddressActionRequest();
        DisassociateAddress disassociateddresses = new DisassociateAddress();

        // TODO: VPC Support -- this does not use associationId, which VPCs use.
        for (String ip : mAssociations) {
            request.withPublicIp(ip);
            mActionHelper.invokeProcess(disassociateddresses,
                    request.getRequest(), request.getResponse(),
                    request.getMap());
            request.reset();
        }

        mAssociations.clear();
    }*/

    @Autowired(required=true)
    public void setActionTestHelper(ActionTestHelper actionTestHelper) {
    	AssociateAddressLocalHelper.mActionHelper = actionTestHelper;
    }

    @Autowired(required=true)
    public void setAssociateAddressWorker(AssociateAddressWorker associateAddressWorker) {
    	AssociateAddressLocalHelper.mAssociateAddressWorker = associateAddressWorker;
    }

    public static class DisassociateAddressRequest extends ActionRequest {
    }

    public static class DisassociateAddressActionRequest extends DisassociateAddressRequest {
        public DisassociateAddressRequest withPublicIp(String ip) {
            put("PublicIp", ip);
            return this;
        }
    }
}
