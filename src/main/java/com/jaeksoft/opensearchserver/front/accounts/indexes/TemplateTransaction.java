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

package com.jaeksoft.opensearchserver.front.accounts.indexes;

import com.jaeksoft.opensearchserver.Components;
import com.jaeksoft.opensearchserver.front.Message;
import com.jaeksoft.opensearchserver.front.accounts.AccountTransaction;
import com.jaeksoft.opensearchserver.model.AccountRecord;
import com.jaeksoft.opensearchserver.services.TemplatesService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.NotFoundException;
import java.io.IOException;

public class TemplateTransaction extends AccountTransaction {

	private final static String TEMPLATE = "accounts/indexes/template.ftl";

	private final TemplatesService templatesService;
	private final String indexName;
	private final String templatePath;

	public TemplateTransaction(final Components components, final AccountRecord accountRecord, final String indexName,
			final String templatePath, final HttpServletRequest request, final HttpServletResponse response) {
		super(components, accountRecord, request, response);
		this.templatesService = components.getTemplatesService();
		this.indexName = indexName;
		this.templatePath = templatePath;
		if (!this.templatesService.getTemplateNames().containsKey(templatePath))
			throw new NotFoundException("Template not found: " + templatePath);
	}

	public void revertDefault() {
		templatesService.deleteTemplateSource(accountRecord.getId(), indexName, templatePath);
		addMessage(Message.Css.success, null, "Template reverted");
	}

	public void template() throws IOException {
		templatesService.setTemplateSource(accountRecord.getId(), indexName, templatePath,
				request.getParameter("editor"));
		addMessage(Message.Css.success, null, "Template saved");
	}

	@Override
	protected String getTemplate() {
		return TEMPLATE;
	}

	@Override
	public void doGet() throws IOException, ServletException {
		request.setAttribute("templates", templatesService.getTemplateNames());
		request.setAttribute("indexName", indexName);
		request.setAttribute("templatePath", templatePath);
		request.setAttribute("htmlTemplate",
				templatesService.getTemplateSource(accountRecord.getId(), indexName, templatePath));
		super.doGet();
	}

}