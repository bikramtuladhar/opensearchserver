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

package com.jaeksoft.opensearchserver.crawler.web;

import com.jaeksoft.opensearchserver.crawler.CrawlerContext;
import com.jaeksoft.opensearchserver.crawler.SessionStore;
import com.qwazr.crawler.web.WebCrawlScriptEvent;
import com.qwazr.crawler.web.WebCrawlSession;
import com.qwazr.crawler.web.WebCurrentCrawl;

import java.util.Map;

public abstract class WebAbstractEvent extends WebCrawlScriptEvent implements CrawlerContext {

    protected abstract boolean run(final EventContext context) throws Exception;

    @Override
    protected final boolean run(final WebCrawlSession session, final WebCurrentCrawl crawl,
        final Map<String, ?> variables) throws Exception {
        return run(new EventContext(session, crawl));
    }

    public static class EventContext {

        final WebCrawlSession crawlSession;
        final WebCurrentCrawl currentCrawl;
        final SessionStore sessionStore;

        private EventContext(final WebCrawlSession crawlSession, final WebCurrentCrawl currentCrawl) {
            this.crawlSession = crawlSession;
            this.currentCrawl = currentCrawl;
            sessionStore = crawlSession.getAttribute(SESSION_STORE, SessionStore.class);
        }

    }

}
