package com.msi.compute;

import static org.junit.Assert.assertEquals;

import java.util.UUID;

import org.junit.Test;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.msi.compute.integration.AbstractBaseComputeTest;
import com.msi.tough.core.Appctx;
import com.transcend.compute.message.AllocateAddressMessage.AllocateAddressRequestMessage;
import com.transcend.compute.message.DescribeInstancesMessage.DescribeInstancesRequestMessage;

/**
 * Test marshal/unmarshall of a protobuf message.
 * @author jgardner
 *
 */
@Component
public class ProtobufMessageTest extends AbstractBaseComputeTest {

    private final static Logger logger = Appctx
            .getLogger(ProtobufMessageTest.class.getName());

    private final String requestId = UUID.randomUUID().toString()
            .substring(0, 8);

    @Test
    public void testMarshallRoundTrip() throws Exception
    {
        DescribeInstancesRequestMessage.Builder req =
                DescribeInstancesRequestMessage.newBuilder();
        req.setTypeId(true);
        req.setCallerAccessKey(getCreds().getAWSAccessKeyId());
        req.setRequestId(requestId);

        DescribeInstancesRequestMessage message = req.build();
        byte[] flat = message.toByteArray();
        Object result = null;
        try {
            result = AllocateAddressRequestMessage.parseFrom(flat);
        } catch (Exception e) {
            result = DescribeInstancesRequestMessage.parseFrom(flat);
        }

        logger.debug("Got payload:"+result.getClass().getName());
        assertEquals("Expect same type.", DescribeInstancesRequestMessage.class,
                result.getClass());
    }
}
