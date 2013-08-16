package com.msi.compute.actions;

import static org.junit.Assert.assertNotNull;

import java.util.HashSet;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.msi.compute.helper.KeyPairLocalHelper;
import com.msi.compute.integration.AbstractBaseComputeTest;
import com.msi.tough.query.ActionTestHelper;
import com.msi.tough.query.ErrorResponse;
import com.transcend.compute.message.CreateKeyPairMessage.CreateKeyPairRequestMessage;
import com.transcend.compute.message.CreateKeyPairMessage.CreateKeyPairResponseMessage;

/**
 * Test creating keypairs locally.
 *
 * @author jgardner
 *
 */
public class CreateKeyPairLocalTest extends AbstractBaseComputeTest {

    private final String alarmBaseName = UUID.randomUUID().toString()
            .substring(0, 8);

    private final String bogusName = "invalid:name";

    String name1 = "crLocal-1-" + alarmBaseName;
    String name2 = "crLocal-2-" + alarmBaseName;

    private HashSet<String> remaining = new HashSet<String>();

    @Autowired
    private ActionTestHelper actionHelper = null;

    @Before
    public void createInitialKeys() throws Exception {
        KeyPairLocalHelper.createKeyPair(name1);
        remaining.add(name1);
        KeyPairLocalHelper.createKeyPair(name2);
        remaining.add(name2);
    }

    @Test(expected=NullPointerException.class)
    public void testCreateKeyPairMissingData() throws Exception {
    	CreateKeyPairRequestMessage cprm = KeyPairLocalHelper.createKeyPairRequest(null);
    	KeyPairLocalHelper.createKeyPair(cprm);
    }

    @Test(expected=ErrorResponse.class)
    public void testCreateIllegalName() throws Exception {
    	KeyPairLocalHelper.createKeyPair(bogusName);
    }

    @Test(expected=ErrorResponse.class)
    public void testCreateDupName() throws Exception {
        CreateKeyPairResponseMessage result = KeyPairLocalHelper.createKeyPair(name1);
        assertNotNull(result);
    }

    @After
    public void cleanupCreated() throws Exception {
        KeyPairLocalHelper.deleteAllCreatedKeyPairs(remaining);
    }
}
