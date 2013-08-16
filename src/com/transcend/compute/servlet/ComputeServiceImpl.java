/**
 * Transcend Computing, Inc.
 * Confidential and Proprietary
 * Copyright (c) Transcend Computing, Inc. 2013
 * All Rights Reserved.
 */
package com.transcend.compute.servlet;

import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import com.msi.tough.core.Appctx;
import com.msi.tough.query.Action;

/**
 * @author jgardner
 */
public class ComputeServiceImpl
{
    private final static Logger logger = Appctx.getLogger(ComputeServiceImpl.class
        .getName());

    private final Map<String, Action> actionMap;

    public ComputeServiceImpl(final Map<String, Action> actionMap)
    {
        this.actionMap = actionMap;
    }

    public void process(final HttpServletRequest req,
        final HttpServletResponse resp) throws Exception
    {
        final Action a = this.actionMap.get(req.getParameter("Action"));
        if (a == null)
        {
            logger.debug("No action exists for " + req.getQueryString());
            logger.debug("Those that exist are:");
            for (Entry<String, Action> item : this.actionMap.entrySet())
            {
                logger.error("\"" + item.getKey() + "\"");
            }
        }
        else
        {
            logger.debug("calling action " + a);
            a.process(req, resp);
        }
    }
}
