/**
 * Transcend Computing, Inc.
 * Confidential and Proprietary
 * Copyright (c) Transcend Computing, Inc. 2012
 * All Rights Reserved.
 */
package com.transcend.compute.utils;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.slf4j.Logger;

import com.msi.tough.core.Appctx;
import com.msi.tough.query.ErrorResponse;


/**
 * Simple component to inspect a mule payload to verify transmission.
 * @author jgardner
 *
 */
public class Snooper implements MessageProcessor {

    private final static Logger logger = Appctx
            .getLogger(Snooper.class.getName());


    public Object snoop(Object payload) {
        logger.info("Got object of type:"+payload.getClass().getName());
        return payload;
    }

    public Object snoopErrorResponse(ErrorResponse ex) {
        logger.info("Got ErrorResponse of type:"+ex.getClass().getName());
        return ex;
    }


    public Object snoop(Exception ex) {
        logger.info("Got ex of type:"+ex.getClass().getName());
        return ex;
    }

    /* (non-Javadoc)
     * @see org.mule.api.processor.MessageProcessor#process(org.mule.api.MuleEvent)
     */
    @Override
    public MuleEvent process(MuleEvent event) throws MuleException {
        logger.info("Got payload of type:"+event.getClass().getName());
        return event;
    }

}