package com.transcend.compute.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.msi.tough.core.Appctx;

public class ComputeServlet extends HttpServlet {
	private final Logger logger = LoggerFactory.getLogger(this.getClass()
			.getName());
	private static final long serialVersionUID = 1L;

	/**
	 * Process the GET request
	 *
	 * @param HttpServletRequest
	 * @param HttpServletRespinse
	 */
	@Override
	protected void doGet(final HttpServletRequest req,
			final HttpServletResponse resp) throws ServletException,
			IOException {
		logger.debug("Into doGet");
		doPost(req, resp);
	}

	/**
	 * Process doPost request
	 *
	 * @param HttpServletRequest
	 * @param HttpServletRespinse
	 */
	@Override
	protected void doPost(final HttpServletRequest req,
			final HttpServletResponse resp) throws ServletException,
			IOException {
	       try {
	            final ComputeServiceImpl impl = Appctx.getBean("computeService");
	            impl.process(req, resp);
	        } catch (final Exception e) {
	            e.printStackTrace();
	        }
	}
}
