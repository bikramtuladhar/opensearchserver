/*
 * Copyright 2017-2018 Emmanuel Keller / Jaeksoft
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.jaeksoft.opensearchserver.front;

import com.jaeksoft.opensearchserver.Components;
import com.jaeksoft.opensearchserver.model.UserRecord;
import com.qwazr.library.freemarker.FreeMarkerTool;
import com.qwazr.utils.ExceptionUtils;
import com.qwazr.utils.LinkUtils;
import com.qwazr.utils.LoggerUtils;
import com.qwazr.utils.StringUtils;
import freemarker.template.TemplateException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.RedirectionException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class ServletTransaction {

	private final static Logger LOGGER = LoggerUtils.getLogger(ServletTransaction.class);

	private final FreeMarkerTool freemarker;
	protected final HttpServletRequest request;
	protected final HttpServletResponse response;
	protected final HttpSession session;
	protected final UserRecord userRecord;

	protected ServletTransaction(final Components components, final HttpServletRequest request,
			final HttpServletResponse response, boolean requireLoggedUser) {
		this.freemarker = components.getFreemarkerTool();
		this.request = request;
		this.response = response;
		this.session = request.getSession();
		this.userRecord = (UserRecord) request.getUserPrincipal();
		if (requireLoggedUser)
			requireLoggedUser();
	}

	private void requireLoggedUser() {
		if (userRecord != null)
			return;
		final StringBuffer requestUrlBuilder = request.getRequestURL();
		final String queryString = request.getQueryString();
		if (!StringUtils.isBlank(queryString)) {
			requestUrlBuilder.append('?');
			requestUrlBuilder.append(queryString);
		}
		final String requestUrl = requestUrlBuilder.toString();
		addMessage(Message.Css.warning, "Please sign in to be able to see this content", requestUrl);
		throw new RedirectionException(requestUrl, Response.Status.TEMPORARY_REDIRECT,
				URI.create("/signin?url=" + LinkUtils.urlEncode(requestUrl)));
	}

	/**
	 * @return the message list
	 */
	synchronized List<Message> getMessages() {
		List<Message> messages = (List<Message>) session.getAttribute("messages");
		if (messages == null) {
			messages = new ArrayList<>();
			session.setAttribute("messages", messages);
		}
		return messages;
	}

	/**
	 * Returns the parameter value from the HTTP request or the default value.
	 *
	 * @param parameterName the name of the parameter
	 * @param defaultValue  a default value
	 * @return the value of the given parameter
	 */
	protected Integer getRequestParameter(String parameterName, Integer defaultValue) {
		final String value = request.getParameter(parameterName);
		if (StringUtils.isBlank(value))
			return defaultValue;
		return Integer.parseInt(value);
	}

	/**
	 * Returns the parameter value from the HTTP request or the value provided by the supplier
	 *
	 * @param parameterName the name of the parameter
	 * @param converter     convert the string to the value
	 * @param defaultValue  a default value supplier
	 * @return the value of the given parameter
	 */
	protected <T> T getRequestParameter(final String parameterName, final Function<String, T> converter,
			final Supplier<T> defaultValue) {
		final String value = request.getParameter(parameterName);
		if (StringUtils.isBlank(value))
			if (defaultValue == null)
				return null;
			else
				return defaultValue.get();
		return converter.apply(value);
	}

	/**
	 * @return the freemarker template
	 */
	protected String getTemplate() {
		return null;
	}

	/**
	 * This default implementation for HTTP GET method display the template provided by getTemplate().
	 * If there is not template, it returns a 405 error code.
	 *
	 * @throws IOException      if any I/O error occured
	 * @throws ServletException if any Servlet error occured
	 */
	protected void doGet() throws IOException, ServletException {
		final String template = getTemplate();
		if (template != null)
			doTemplate(template);
		else
			response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}

	/**
	 * This default implementation for HTTP POST method returns a 405 error code.
	 *
	 * @throws IOException
	 * @throws ServletException
	 */
	final void doPost() throws IOException {
		final String action = request.getParameter("action");
		if (StringUtils.isBlank(action)) {
			response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
			return;
		}
		try {
			final Method method = getClass().getDeclaredMethod(action);
			final Object redirect = method.invoke(this);
			if (!response.isCommitted())
				sendRedirect(redirect == null ? null : redirect.toString());
		} catch (ReflectiveOperationException | WebApplicationException e) {
			addMessage(e);
			sendRedirect(null);
		}
	}

	/**
	 * Redirect to the given URL. If redirect is null, it redirects to the current URL
	 *
	 * @throws IOException
	 */
	private void sendRedirect(String redirect) throws IOException {
		if (redirect == null) {
			final String queryString = request.getQueryString();
			redirect = request.getRequestURI();
			if (queryString != null)
				redirect += '?' + queryString;
		}
		response.sendRedirect(redirect);
	}

	/**
	 * Display a Freemarker template
	 *
	 * @param templatePath the path to the freemarker template
	 * @throws IOException      if any I/O exception occured
	 * @throws ServletException if any templace exception occured
	 */
	protected void doTemplate(String templatePath) throws IOException, ServletException {
		try {
			final List<Message> messages = getMessages();
			request.setAttribute("messages", messages);
			freemarker.template(templatePath, request, response);
			messages.clear();
		} catch (TemplateException e) {
			throw new ServletException(e);
		}
	}

	/**
	 * Add a message. The message may be displayed to the user.
	 *
	 * @param css     the CSS type to use
	 * @param title   the title of the message
	 * @param message the content of the message
	 */
	protected void addMessage(final Message.Css css, final String title, final String message) {
		getMessages().add(new Message(css, title, message));
	}

	protected void addMessage(final Exception e) {
		LOGGER.log(Level.SEVERE, e, e::getMessage);
		addMessage(Message.Css.danger, "Internal error", ExceptionUtils.getRootCauseMessage(e));
	}

}