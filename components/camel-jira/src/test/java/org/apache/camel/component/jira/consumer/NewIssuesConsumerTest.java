/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.jira.consumer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClientFactory;
import com.atlassian.jira.rest.client.api.SearchRestClient;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import io.atlassian.util.concurrent.Promise;
import io.atlassian.util.concurrent.Promises;
import org.apache.camel.CamelContext;
import org.apache.camel.Consumer;
import org.apache.camel.Endpoint;
import org.apache.camel.EndpointInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jira.JiraComponent;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.apache.camel.component.jira.JiraConstants.JIRA;
import static org.apache.camel.component.jira.JiraConstants.JIRA_REST_CLIENT_FACTORY;
import static org.apache.camel.component.jira.JiraTestConstants.JIRA_CREDENTIALS;
import static org.apache.camel.component.jira.JiraTestConstants.PROJECT;
import static org.apache.camel.component.jira.Utils.createIssue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NewIssuesConsumerTest extends CamelTestSupport {

    private static List<Issue> issues = new ArrayList<>();

    @Mock
    private JiraRestClient jiraClient;

    @Mock
    private JiraRestClientFactory jiraRestClientFactory;

    @Mock
    private SearchRestClient searchRestClient;

    @EndpointInject(uri = "mock:result")
    private MockEndpoint mockResult;

    @Override
    protected JndiRegistry createRegistry() throws Exception {
        JndiRegistry registry = super.createRegistry();
        registry.bind(JIRA_REST_CLIENT_FACTORY, jiraRestClientFactory);
        return registry;
    }

    @BeforeClass
    public static void beforeAll() {
        issues.add(createIssue(1L));
        issues.add(createIssue(2L));
        issues.add(createIssue(3L));
    }

    public void setMocks() {
        SearchResult result = new SearchResult(0, 50, 100, issues);
        Promise<SearchResult> promiseSearchResult = Promises.promise(result);

        when(jiraClient.getSearchClient()).thenReturn(searchRestClient);
        when(jiraRestClientFactory.createWithBasicHttpAuthentication(any(), any(), any())).thenReturn(jiraClient);
        when(searchRestClient.searchJql(any(), any(), any(), any())).thenReturn(promiseSearchResult);
    }

    @Override
    protected CamelContext createCamelContext() throws Exception {
        setMocks();
        CamelContext camelContext = super.createCamelContext();
        camelContext.disableJMX();
        JiraComponent component = new JiraComponent(camelContext);
        camelContext.addComponent(JIRA, component);
        return camelContext;
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("jira://newIssues?jiraUrl=" + JIRA_CREDENTIALS + "&jql=project=" + PROJECT + "&delay=5000")
                    .to(mockResult);
            }
        };
    }

    @Test
    public void emptyAtStartupTest() throws Exception {
        mockResult.expectedMessageCount(0);
        mockResult.assertIsSatisfied();
    }

    @Test
    public void singleIssueTest() throws Exception {
        Issue issue = createIssue(11);

        reset(searchRestClient);
        AtomicBoolean searched = new AtomicBoolean(false);
        when(searchRestClient.searchJql(any(), any(), any(), any())).then(invocation -> {
            List<Issue> newIissues = new ArrayList<>();
            if (!searched.get()) {
                newIissues.add(issue);
                searched.set(true);
            }
            SearchResult result = new SearchResult(0, 50, 100, newIissues);
            return Promises.promise(result);
        });
        mockResult.expectedBodiesReceived(issue);
        mockResult.assertIsSatisfied();
    }

    @Test
    public void multipleIssuesTest() throws Exception {
        Issue issue1 = createIssue(21);
        Issue issue2 = createIssue(22);
        Issue issue3 = createIssue(23);

        reset(searchRestClient);
        AtomicBoolean searched = new AtomicBoolean(false);
        when(searchRestClient.searchJql(any(), any(), any(), any())).then(invocation -> {
            List<Issue> newIssues = new ArrayList<>();
            if (!searched.get()) {
                newIssues.add(issue1);
                newIssues.add(issue2);
                newIssues.add(issue3);
                searched.set(true);
            }
            SearchResult result = new SearchResult(0, 50, 3, newIssues);
            return Promises.promise(result);
        });

        mockResult.expectedBodiesReceived(issue3, issue2, issue1);
        mockResult.assertIsSatisfied();
    }

    @Test
    public void useDeprecatedNewIssue() throws Exception {
        String camelUrl = "jira://newIssue?jiraUrl=" + JIRA_CREDENTIALS + "&jql=project=" + PROJECT + "&delay=500";
        Endpoint endpoint = context.getEndpoint(camelUrl);
        Consumer newIssuesConsumer = endpoint.createConsumer(exchange -> template.send(mockResult, exchange));
        assertEquals(newIssuesConsumer.getClass(), NewIssuesConsumer.class);
    }

}
