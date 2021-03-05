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

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PahoMqtt5Producer extends DefaultProducer {

    private static final Logger LOG = LoggerFactory.getLogger(PahoMqtt5Producer.class);

    public PahoMqtt5Producer(PahoMqtt5Endpoint endpoint) {
        super(endpoint);
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        MqttClient client = getEndpoint().getClient();

        String topic = exchange.getIn().getHeader(PahoMqtt5Constants.CAMEL_PAHO_OVERRIDE_TOPIC,
                getEndpoint().getTopic(), String.class);
        int qos = exchange.getIn().getHeader(PahoMqtt5Constants.CAMEL_PAHO_MSG_QOS,
                getEndpoint().getQos(), Integer.class);
        boolean retained = exchange.getIn().getHeader(PahoMqtt5Constants.CAMEL_PAHO_MSG_RETAINED,
                getEndpoint().isRetained(), Boolean.class);
        byte[] payload = exchange.getIn().getBody(byte[].class);

        MqttMessage message = new MqttMessage(payload);
        message.setQos(qos);
        message.setRetained(retained);

        LOG.debug("Publishing to topic: {}, qos: {}, retrained: {}", topic, qos, retained);
        client.publish(topic, message);
    }

    @Override
    public PahoMqtt5Endpoint getEndpoint() {
        return (PahoMqtt5Endpoint) super.getEndpoint();
    }

}
