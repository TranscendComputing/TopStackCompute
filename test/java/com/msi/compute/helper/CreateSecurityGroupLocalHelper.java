package com.msi.compute.helper;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.msi.tough.core.Appctx;
import com.msi.tough.query.ActionTestHelper;
import com.transcend.compute.message.CreateSecurityGroupMessage.CreateSecurityGroupRequest;
import com.transcend.compute.message.CreateSecurityGroupMessage.CreateSecurityGroupResponse;
import com.transcend.compute.message.DeleteSecurityGroupMessage.DeleteSecurityGroupRequestMessage;
import com.transcend.compute.worker.CreateSecurityGroupWorker;
import com.transcend.compute.worker.DeleteSecurityGroupWorker;

/**
 * Address helper for non-web tests (using actions in-VM).
 *
 */
@Component
public class CreateSecurityGroupLocalHelper {
    private static Logger logger = Appctx
            .getLogger(CreateSecurityGroupLocalHelper.class.getName());

    private List<String> mSecurityGroups = new ArrayList<String>();

    private static CreateSecurityGroupWorker mCreateSecurityGroupWorker = null;
    private static DeleteSecurityGroupWorker deleteSecurityGroupWorker = null;

    private static ActionTestHelper mActionHelper = null;

    /**
     * Construct a minimal valid create request.
     *
     * @param sgName security group name
     * @param sgDescription security group description
     * @return
     */
    public CreateSecurityGroupRequest createSecurityGroupRequest(
    		String sgName, String sgDescription) {
        final CreateSecurityGroupRequest.Builder builder =
        		CreateSecurityGroupRequest.newBuilder();
        builder.setTypeId(true);
        builder.setCallerAccessKey(mActionHelper.getAccessKey());
        builder.setRequestId("test");
        builder.setGroupName(sgName);
        builder.setGroupDescription(sgDescription);
        return builder.build();
    }

    /**
     * Create a security group (don't care about the details, just need one).
     *
     * @return: String groupId
     */
    public String createSecurityGroup()
    		throws Exception {

        final String suffix = UUID.randomUUID().toString().substring(0, 8);
        final String groupName = this.getTestGroupName(suffix);
        final String groupDescription = this.getTestGoupDescription(suffix);

    	CreateSecurityGroupRequest req = createSecurityGroupRequest(
    			groupName, groupDescription);
    	CreateSecurityGroupResponse response = null;

    	response = mCreateSecurityGroupWorker.doWork(req);
    	String groupId = response.getGroupId();
    	logger.debug("Created GroupID: " + groupId);
        mSecurityGroups.add(groupId);
        return groupId;
    }

    /**
     * Perform operations necessary to complete a full integration test.
     *
     * @return: String groupId
     */
    public String integrationTest(AmazonEC2Client computeClient)
    	throws AmazonServiceException, AmazonClientException
    {
	    final String suffix = UUID.randomUUID().toString().substring(0, 8);
	    final String groupName = this.getTestGroupName(suffix);
	    final String groupDescription = this.getTestGoupDescription(suffix);
	    CreateSecurityGroupResult createResult = null;
	    final com.amazonaws.services.ec2.model.CreateSecurityGroupRequest ec2request =
	    		new com.amazonaws.services.ec2.model.CreateSecurityGroupRequest();
	    ec2request.setGroupName(groupName);
	    ec2request.setDescription(groupDescription);

	    logger.info("Creating Security Group");
	    createResult = computeClient.createSecurityGroup(ec2request);
	    mSecurityGroups.add(groupName);
	    final String groupId = createResult.getGroupId();
	    assertNotNull("Expect a group id.", groupId);

	    return groupId;
    }


	public String getTestGroupName(String suffix) {
        return "sgLocal-test-" + suffix;
	}

    public String getTestGoupDescription(String suffix) {
        return "sgLocal-test-" + suffix + " create group test.";
	}

	/**
     * Delete a group with the given name.
     *
     * @param groupId
     */
    public void deleteSecurityGroup(String groupId) throws Exception {
        DeleteSecurityGroupRequestMessage.Builder req =
                DeleteSecurityGroupRequestMessage.newBuilder();
        req.setTypeId(true);
        req.setCallerAccessKey(mActionHelper.getAccessKey());
        req.setRequestId("test");
        req.setGroupId(groupId);
        deleteSecurityGroupWorker.doWork(req.build());
    }

    /**
     * Delete all created groups.
     *
     */
    public void deleteAllSecurityGroups() throws Exception {
        for (String groupName : mSecurityGroups) {
            deleteSecurityGroup(groupName);
        }
        mSecurityGroups.clear();
    }

    @Autowired(required=true)
    public void setActionTestHelper(ActionTestHelper actionTestHelper) {
        CreateSecurityGroupLocalHelper.mActionHelper = actionTestHelper;
    }

    @Autowired(required=true)
    public void setCreateSecurityGroupWorker(CreateSecurityGroupWorker createSecurityGroupWorker) {
    	CreateSecurityGroupLocalHelper.mCreateSecurityGroupWorker = createSecurityGroupWorker;
    }

    @Autowired(required=true)
    public void setDeleteSecurityGroupWorker(DeleteSecurityGroupWorker deleteSecurityGroupWorker) {
        CreateSecurityGroupLocalHelper.deleteSecurityGroupWorker = deleteSecurityGroupWorker;
    }
}
