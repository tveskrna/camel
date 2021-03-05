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

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultComponent;
import org.apache.camel.spi.Metadata;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;

/**
 * Component to integrate with the Eclipse Paho MQTT v5 library.
 */
public class PahoMqtt5Component extends DefaultComponent {

    private String brokerUrl;
    private String clientId;
    @Metadata(label = "advanced")
    private MqttConnectionOptions connectionOptions;

    public PahoMqtt5Component() {
        this(null);
    }

    public PahoMqtt5Component(CamelContext context) {
        super(context);

        registerExtension(new PahoMqtt5ComponentVerifierExtension());
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        PahoMqtt5Endpoint answer = new PahoMqtt5Endpoint(uri, remaining, this);

        if (brokerUrl != null) {
            answer.setBrokerUrl(brokerUrl);
        }
        if (clientId != null) {
            answer.setClientId(clientId);
        }
        if (connectionOptions != null) {
            answer.setConnectionOptions(connectionOptions);
        }

        setProperties(answer, parameters);
        return answer;
    }

    // Getters and setters

    public String getBrokerUrl() {
        return brokerUrl;
    }

    /**
     * The URL of the MQTT broker.
     */
    public void setBrokerUrl(String brokerUrl) {
        this.brokerUrl = brokerUrl;
    }

    public String getClientId() {
        return clientId;
    }

    /**
     * MQTT client identifier.
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public MqttConnectionOptions getConnectionOptions() {
        return connectionOptions;
    }

    /**
     * Client connection options
     */
    public void setConnectionOptions(MqttConnectionOptions connectionOptions) {
        this.connectionOptions = connectionOptions;
    }

}
