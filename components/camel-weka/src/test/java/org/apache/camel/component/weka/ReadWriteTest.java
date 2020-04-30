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
package org.apache.camel.component.weka;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.nessus.weka.Dataset;
import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.Assert;
import org.junit.Test;

public class ReadWriteTest {

    @Test
    public void wekaVersion() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();

        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("weka:version");
            }
        });
        camelctx.start();

        try {

            ProducerTemplate producer = camelctx.createProducerTemplate();
            String res = producer.requestBody("direct:start", null, String.class);
            Assert.assertTrue(res.startsWith("3.8"));

        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void readCsvFile() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();

        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("file:src/test/resources/data?fileName=sfny.csv&noop=true").to("weka:read").to("direct:end");
            }
        });
        camelctx.start();

        try {

            ConsumerTemplate consumer = camelctx.createConsumerTemplate();
            Dataset dataset = consumer.receiveBody("direct:end", Dataset.class);
            Assert.assertNotNull(dataset);

        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void readCsvUrl() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();

        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("weka:read");
            }
        });
        camelctx.start();

        try {

            Path absPath = Paths.get("src/test/resources/data/sfny.csv").toAbsolutePath();
            URL sourceUrl = absPath.toUri().toURL();

            ProducerTemplate producer = camelctx.createProducerTemplate();
            Dataset dataset = producer.requestBody("direct:start", sourceUrl, Dataset.class);
            Assert.assertNotNull(dataset);

        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void readCsvInputStream() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();

        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("weka:read");
            }
        });
        camelctx.start();

        try {

            Path absPath = Paths.get("src/test/resources/data/sfny.csv").toAbsolutePath();
            InputStream input = absPath.toUri().toURL().openStream();

            ProducerTemplate producer = camelctx.createProducerTemplate();
            Dataset dataset = producer.requestBody("direct:start", input, Dataset.class);
            Assert.assertNotNull(dataset);

        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void readArffWithPath() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();

        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("weka:read?path=src/test/resources/data/sfny.arff");
            }
        });
        camelctx.start();

        try {

            ProducerTemplate producer = camelctx.createProducerTemplate();
            Dataset dataset = producer.requestBody("direct:start", null, Dataset.class);
            Assert.assertNotNull(dataset);

        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void readArffInputStream() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();

        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("weka:read");
            }
        });
        camelctx.start();

        try {

            Path absPath = Paths.get("src/test/resources/data/sfny.arff").toAbsolutePath();
            InputStream input = absPath.toUri().toURL().openStream();

            ProducerTemplate producer = camelctx.createProducerTemplate();
            Dataset dataset = producer.requestBody("direct:start", input, Dataset.class);
            Assert.assertNotNull(dataset);

        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void writeDatasetWithConversion() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();

        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("file:target/data?fileName=sfny.arff");
            }
        });
        camelctx.start();

        try {

            Path inpath = Paths.get("src/test/resources/data/sfny.arff");
            Dataset dataset = Dataset.create(inpath);

            ProducerTemplate producer = camelctx.createProducerTemplate();
            producer.sendBody("direct:start", dataset);

            Path outpath = Paths.get("target/data/sfny.arff");
            dataset = Dataset.create(outpath);
            Assert.assertNotNull(dataset);

        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void writeDatasetWithoutPath() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();

        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("weka:write").to("file:target/data?fileName=sfny.arff");
            }
        });
        camelctx.start();

        try {

            Path inpath = Paths.get("src/test/resources/data/sfny.arff");
            Dataset dataset = Dataset.create(inpath);

            ProducerTemplate producer = camelctx.createProducerTemplate();
            producer.sendBody("direct:start", dataset);

            Path outpath = Paths.get("target/data/sfny.arff");
            dataset = Dataset.create(outpath);
            Assert.assertNotNull(dataset);

        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void writeDatasetWithPath() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();

        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("weka:write?path=target/data/sfny.arff");
            }
        });
        camelctx.start();

        try {

            Path inpath = Paths.get("src/test/resources/data/sfny.arff");
            Dataset dataset = Dataset.create(inpath);

            ProducerTemplate producer = camelctx.createProducerTemplate();
            producer.sendBody("direct:start", dataset);

            Path outpath = Paths.get("target/data/sfny.arff");
            dataset = Dataset.create(outpath);
            Assert.assertNotNull(dataset);

        } finally {
            camelctx.stop();
        }
    }
}
