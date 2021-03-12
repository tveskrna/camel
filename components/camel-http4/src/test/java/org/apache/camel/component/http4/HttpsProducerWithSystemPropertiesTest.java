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
package org.apache.camel.component.http4;

import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.apache.camel.Exchange;
import org.apache.camel.component.http4.handler.BasicValidationHandler;
import org.apache.camel.component.http4.handler.HeaderValidationHandler;
import org.apache.camel.impl.JndiRegistry;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.ssl.SSLContexts;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @version 
 */
public class HttpsProducerWithSystemPropertiesTest extends BaseHttpsTest {

    private static Object defaultSystemHttpAgent;
    private HttpServer localServer;

    @Override
    protected JndiRegistry createRegistry() throws Exception {
        JndiRegistry registry = super.createRegistry();
        registry.bind("x509HostnameVerifier", new NoopHostnameVerifier());

        return registry;
    }

    @BeforeClass
    public static void setUpHttpAgentSystemProperty() throws Exception {
        // the 'http.agent' java system-property corresponds to the http 'User-Agent' header
        defaultSystemHttpAgent = System.setProperty("http.agent", "myCoolCamelCaseAgent");

        System.setProperty("javax.net.ssl.keyStore", KEYSTORE_PATH);
        System.setProperty("javax.net.ssl.keyStorePassword", "changeit");
        System.setProperty("javax.net.ssl.trustStore", KEYSTORE_PATH);
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
    }

    @AfterClass
    public static void resetHttpAgentSystemProperty() throws Exception {
        if (defaultSystemHttpAgent != null) {
            System.setProperty("http.agent", String.valueOf(defaultSystemHttpAgent));
        } else {
            System.clearProperty("http.agent");
        }
        System.clearProperty("javax.net.ssl.trustStorePassword");
        System.clearProperty("javax.net.ssl.trustStore");
        System.clearProperty("javax.net.ssl.keyStorePassword");
        System.clearProperty("javax.net.ssl.keyStore");
    }

    @Before
    @Override
    public void setUp() throws Exception {
        SSLContext sslcontext = SSLContexts.custom()
                .loadKeyMaterial(KEYSTORE, "changeit".toCharArray(), "changeit".toCharArray())
                .loadTrustMaterial(KEYSTORE, "changeit".toCharArray())
                .build();

        Map<String, String> expectedHeaders = new HashMap<>();
        expectedHeaders.put("User-Agent", "myCoolCamelCaseAgent");

        localServer = ServerBootstrap.bootstrap().
                setHttpProcessor(getBasicHttpProcessor()).
                setConnectionReuseStrategy(getConnectionReuseStrategy()).
                setResponseFactory(getHttpResponseFactory()).
                setExpectationVerifier(getHttpExpectationVerifier()).
                setSslContext(sslcontext).
                setSslSetupHandler(socket -> socket.setNeedClientAuth(true)).
                registerHandler("/mail/", new BasicValidationHandler("GET", null, null, getExpectedContent())).
                registerHandler("/header/", new HeaderValidationHandler("GET", null, null, getExpectedContent(), expectedHeaders)).
                create();
        localServer.start();

        super.setUp();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();

        if (localServer != null) {
            localServer.stop();
        }
    }

    @Test
    public void httpGetWithProxyFromSystemProperties() throws Exception {

        String endpointUri = "https4://" + localServer.getInetAddress().getHostName() + ":" + localServer.getLocalPort()
                + "/header/?x509HostnameVerifier=x509HostnameVerifier&useSystemProperties=true";
        Exchange exchange = template.request(endpointUri, exchange1 -> {
        });

        assertExchange(exchange);
    }

    @Test
    public void testTwoWaySuccessfull() throws Exception {
        Exchange exchange = template.request("https4://127.0.0.1:" + localServer.getLocalPort()
                        + "/mail/?x509HostnameVerifier=x509HostnameVerifier&useSystemProperties=true",
            exchange1 -> {
            });
        assertExchange(exchange);
    }

    @Test
    public void testTwoWayFailure() throws Exception {
        Exchange exchange = template.request("https4://127.0.0.1:" + localServer.getLocalPort()
                        + "/mail/?x509HostnameVerifier=x509HostnameVerifier",
            exchange1 -> {
            });
        //exchange does not have response code, because it was rejected
        assertNull("Should not have Content-Type header", exchange.getMessage().getHeaders().get("Content-Type"));
    }

}
