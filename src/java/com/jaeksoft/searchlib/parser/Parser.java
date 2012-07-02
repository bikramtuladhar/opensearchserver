/**   
 * License Agreement for OpenSearchServer
 *
 * Copyright (C) 2008-2012 Emmanuel Keller / Jaeksoft
 * 
 * http://www.open-search-server.com
 * 
 * This file is part of OpenSearchServer.
 *
 * OpenSearchServer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * OpenSearchServer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with OpenSearchServer. 
 *  If not, see <http://www.gnu.org/licenses/>.
 **/

package com.jaeksoft.searchlib.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import org.knallgrau.utils.textcat.TextCategorizer;

import com.jaeksoft.searchlib.SearchLibException;
import com.jaeksoft.searchlib.analysis.LanguageEnum;
import com.jaeksoft.searchlib.crawler.file.process.FileInstanceAbstract;
import com.jaeksoft.searchlib.index.FieldContent;
import com.jaeksoft.searchlib.index.IndexDocument;
import com.jaeksoft.searchlib.schema.FieldValueItem;
import com.jaeksoft.searchlib.streamlimiter.LimitException;
import com.jaeksoft.searchlib.streamlimiter.StreamLimiter;
import com.jaeksoft.searchlib.streamlimiter.StreamLimiterBase64;
import com.jaeksoft.searchlib.streamlimiter.StreamLimiterFile;
import com.jaeksoft.searchlib.streamlimiter.StreamLimiterFileInstance;
import com.jaeksoft.searchlib.streamlimiter.StreamLimiterInputStream;
import com.jaeksoft.searchlib.util.Lang;

public abstract class Parser extends ParserFactory {

	private IndexDocument sourceDocument;

	private IndexDocument parserDocument;

	private IndexDocument directDocument;

	private StreamLimiter streamLimiter;

	protected Parser(ParserFieldEnum[] fieldList) {
		super(fieldList);
		sourceDocument = null;
		directDocument = null;
		parserDocument = new IndexDocument();
		streamLimiter = null;
	}

	public void populate(IndexDocument indexDocument) {
		getFieldMap().mapIndexDocument(parserDocument, indexDocument);
		if (directDocument != null)
			indexDocument.add(directDocument);
	}

	public IndexDocument getSourceDocument() {
		return sourceDocument;
	}

	private void setSourceDocument(IndexDocument sourceDocument) {
		this.sourceDocument = sourceDocument;
	}

	public IndexDocument getParserDocument() {
		return parserDocument;
	}

	public void resetParserFieldList() {
		ParserFieldEnum[] parserFieldList = getFieldList();
		if (parserFieldList == null)
			return;
		for (ParserFieldEnum parserField : parserFieldList) {
			FieldContent fc = parserDocument.getField(parserField.name());
			if (fc != null)
				fc.clear();
		}
	}

	public void addField(ParserFieldEnum field, String value) {
		if (value == null)
			return;
		if (value.length() == 0)
			return;
		parserDocument.add(field.name(), new FieldValueItem(value));
	}

	public void addField(ParserFieldEnum field, String value, Float boost) {
		if (value == null)
			return;
		if (value.length() == 0)
			return;
		parserDocument.add(field.name(), new FieldValueItem(value, boost));
	}

	public void addDirectFields(String[] fields, String value) {
		if (directDocument == null)
			directDocument = new IndexDocument();
		for (String field : fields)
			directDocument.add(field, new FieldValueItem(value));
	}

	public void addDirectFields(String[] fields, String value, Float boost) {
		if (directDocument == null)
			directDocument = new IndexDocument();
		for (String field : fields)
			directDocument.add(field, new FieldValueItem(value, boost));
	}

	protected void addField(ParserFieldEnum field, Object object) {
		if (object == null)
			return;
		addField(field, object.toString());
	}

	public FieldContent getFieldContent(ParserFieldEnum field) {
		return parserDocument.getFieldContent(field.name());
	}

	public String getFieldValue(ParserFieldEnum field, int pos) {
		FieldValueItem valueItem = parserDocument.getFieldValue(field.name(),
				pos);
		if (valueItem == null)
			return null;
		return valueItem.getValue();
	}

	public String getMergedBodyText(int maxChar, String separator,
			ParserFieldEnum field) {
		FieldContent fc = getFieldContent(field);
		if (fc == null)
			return "";
		return fc.getMergedValues(maxChar, separator);
	}

	protected Locale langDetection(int textLength, ParserFieldEnum parserField) {
		Locale lang = null;
		String langMethod = null;
		String text = getMergedBodyText(textLength, " ", parserField);
		if (text == null)
			return null;
		langMethod = "ngram recognition";
		String textcat = new TextCategorizer().categorize(text, text.length());
		lang = Lang.findLocaleDescription(textcat);

		if (lang == null)
			return null;

		addField(ParserFieldEnum.lang, lang.getLanguage());
		addField(ParserFieldEnum.lang_method, langMethod);
		return lang;
	}

	protected abstract void parseContent(StreamLimiter streamLimiter,
			LanguageEnum lang) throws IOException;

	final private void doParserContent(StreamLimiter streamLimiter,
			LanguageEnum lang) throws IOException {
		try {
			addField(ParserFieldEnum.parser_name, getParserName());
			parseContent(streamLimiter, lang);
		} finally {
			streamLimiter.close();
		}
	}

	final void parseStream(IndexDocument sourceDocument,
			String originalFileName, InputStream inputStream, LanguageEnum lang)
			throws IOException {
		if (sourceDocument != null)
			setSourceDocument(sourceDocument);
		StreamLimiter streamLimiter = new StreamLimiterInputStream(
				getSizeLimit(), inputStream, originalFileName);
		doParserContent(streamLimiter, lang);
	}

	final void parseFile(IndexDocument sourceDocument, File file,
			LanguageEnum lang) throws IOException {
		if (sourceDocument != null)
			setSourceDocument(sourceDocument);
		StreamLimiter streamLimiter = new StreamLimiterFile(getSizeLimit(),
				file);
		doParserContent(streamLimiter, lang);
	}

	final void parseBase64(IndexDocument sourceDocument, String base64text,
			String fileName, LanguageEnum lang) throws IOException {
		if (sourceDocument != null)
			setSourceDocument(sourceDocument);
		StreamLimiter streamLimiter = new StreamLimiterBase64(base64text,
				getSizeLimit(), fileName);
		doParserContent(streamLimiter, lang);
	}

	final void parseFileInstance(IndexDocument sourceDocument,
			FileInstanceAbstract fileInstance, LanguageEnum lang)
			throws IOException, SearchLibException {
		if (sourceDocument != null)
			setSourceDocument(sourceDocument);
		StreamLimiter streamLimiter = new StreamLimiterFileInstance(
				fileInstance, getSizeLimit());
		doParserContent(streamLimiter, lang);
	}

	public StreamLimiter getStreamLimiter() {
		return streamLimiter;
	}

	public String getMd5size() throws NoSuchAlgorithmException, LimitException,
			IOException {
		String hash = null;
		if (streamLimiter != null)
			hash = streamLimiter.getMD5Hash() + '_' + streamLimiter.getSize();
		return hash;
	}

	public boolean equals(Parser one) {
		return this.getClass().getName().equals(one.getClass().getName());
	}

}