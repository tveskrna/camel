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
package org.apache.camel.component.paho.mqtt5;

import org.apache.camel.builder.RouteBuilder;
import org.junit.Test;

public class PahoMqtt5OverrideTopicMqtt5Test extends PahoMqtt5TestSupport {

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                PahoMqtt5Component paho = context.getComponent("paho-mqtt5", PahoMqtt5Component.class);
                paho.setBrokerUrl("tcp://localhost:" + mqttPort);

                from("direct:test").to("paho-mqtt5:queue").log("Message sent");

                from("paho-mqtt5:myoverride").log("Message received").to("mock:test");
            }
        };
    }

    // Tests

    @Test
    public void shouldOverride() throws InterruptedException {
        // Given
        getMockEndpoint("mock:test").expectedMessageCount(1);

        // When
        template.sendBodyAndHeader("direct:test", "Hello World", PahoMqtt5Constants.CAMEL_PAHO_OVERRIDE_TOPIC, "myoverride");

        // Then
        assertMockEndpointsSatisfied();
    }

}
