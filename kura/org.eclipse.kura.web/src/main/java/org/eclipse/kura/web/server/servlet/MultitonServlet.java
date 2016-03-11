package org.eclipse.kura.web.server.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultitonServlet extends HttpServlet {

	private static Logger s_logger = LoggerFactory.getLogger(MultitonServlet.class);
	/**
	 * 
	 */
	private static final long serialVersionUID = 5431123028422829699L;
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/html");
		s_logger.info("********* MULTITON SERVLET ************");
		return;
	}
	
	

}
