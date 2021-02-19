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
package org.apache.camel.component.salesforce;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.component.salesforce.api.SalesforceException;
import org.apache.camel.component.salesforce.api.dto.bulkv2.Job;
import org.apache.camel.component.salesforce.api.dto.bulkv2.JobStateEnum;
import org.apache.camel.component.salesforce.api.dto.bulkv2.Jobs;
import org.apache.camel.component.salesforce.api.dto.bulkv2.OperationEnum;
import org.apache.camel.impl.DefaultExchange;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.Test;

@SuppressWarnings("BusyWait")
public class BulkApiV2IngestJobIntegrationTest extends AbstractSalesforceTestBase {

    @Test
    public void testLifecycle() throws Exception {
        Job job = new Job();
        job.setObject("Contact");
        job.setOperation(OperationEnum.INSERT);

        job = template().requestBody("salesforce:bulk2CreateJob", job, Job.class);
        assertNotNull(job.getId(), "JobId");

        job = template().requestBody("salesforce:bulk2GetJob", job, Job.class);
        //assertSame(JobStateEnum.OPEN, job.getState(), "Job state");
        Assertions.assertThat(JobStateEnum.OPEN).as("Job state").isSameAs(job.getState());
        Exchange exchange = new DefaultExchange(context());
        exchange.getIn().setBody("FirstName,LastName\nTestFirst,TestLast");
        exchange.getIn().setHeader("jobId", job.getId());
        template.send("salesforce:bulk2CreateBatch", exchange);
        assertNull(exchange.getException());

        job = template().requestBody("salesforce:bulk2GetJob", job, Job.class);
        Assertions.assertThat(JobStateEnum.OPEN).as("Job state").isSameAs(job.getState());

        job = template().requestBodyAndHeader("salesforce:bulk2CloseJob", "", "jobId", job.getId(),
                Job.class);

        Assertions.assertThat(JobStateEnum.UPLOAD_COMPLETE).as("Job state").isEqualTo(job.getState());

        // wait for job to finish
        while (job.getState() != JobStateEnum.JOB_COMPLETE) {
            Thread.sleep(2000);
            job = template().requestBodyAndHeader("salesforce:bulk2GetJob", "", "jobId",
                    job.getId(), Job.class);
        }

        InputStream is = template().requestBodyAndHeader("salesforce:bulk2GetSuccessfulResults",
                "", "jobId", job.getId(), InputStream.class);
        Assertions.assertThat(is).as("Successful results").isNotNull();
        List<String> successful = IOUtils.readLines(is, StandardCharsets.UTF_8);
        Assertions.assertThat(successful.size()).isEqualTo(2);
        Assertions.assertThat(successful.get(1)).contains("TestFirst");
        is = template().requestBodyAndHeader("salesforce:bulk2GetFailedResults",
                "", "jobId", job.getId(), InputStream.class);
        Assertions.assertThat(is).as("Failed results").isNotNull();
        List<String> failed = IOUtils.readLines(is, StandardCharsets.UTF_8);
        Assertions.assertThat(failed.size()).isEqualTo(1);
        is = template().requestBodyAndHeader("salesforce:bulk2GetUnprocessedRecords",
                "", "jobId", job.getId(), InputStream.class);
        Assertions.assertThat(is).as("Unprocessed records").isNotNull();
        List<String> unprocessed = IOUtils.readLines(is, StandardCharsets.UTF_8);
        Assertions.assertThat(unprocessed.size()).isEqualTo(1);
        Assertions.assertThat(unprocessed.get(0)).isEqualTo("FirstName,LastName");
    }

    @Test
    public void testAbort() {
        Job job = new Job();
        job.setObject("Contact");
        job.setOperation(OperationEnum.INSERT);
        job = createJob(job);

        job = template().requestBody("salesforce:bulk2GetJob", job, Job.class);
        Assertions.assertThat(job.getState()).isSameAs(JobStateEnum.OPEN).describedAs("Job should be OPEN");
        template().sendBodyAndHeader("salesforce:bulk2AbortJob", "", "jobId", job.getId());

        job = template().requestBody("salesforce:bulk2GetJob", job, Job.class);
        Assertions.assertThat(job.getState()).isSameAs(JobStateEnum.ABORTED).describedAs("Job state");
    }

    @Test
    public void testDelete() {
        Job job = new Job();
        job.setObject("Contact");
        job.setOperation(OperationEnum.INSERT);
        job = createJob(job);
        job = template().requestBody("salesforce:bulk2GetJob", job, Job.class);
        Assertions.assertThat(job.getState()).isSameAs(JobStateEnum.OPEN).describedAs("Job should be OPEN");
        template().sendBodyAndHeader("salesforce:bulk2AbortJob", "", "jobId", job.getId());
        job = template().requestBody("salesforce:bulk2GetJob", job, Job.class);
        Assertions.assertThat(job.getState()).as("Job state").isSameAs(JobStateEnum.ABORTED);
        template().sendBodyAndHeader("salesforce:bulk2DeleteJob", "", "jobId", job.getId());
        final Job finalJob = job;
        Throwable thrown = Assertions.catchThrowable(() -> template().requestBody("salesforce:bulk2GetJob", finalJob, Job.class));
        Assertions.assertThat(thrown).isInstanceOf(CamelExecutionException.class);
        CamelExecutionException ex = (CamelExecutionException) thrown;
        Assertions.assertThat(ex.getCause()).isInstanceOf(SalesforceException.class);
        SalesforceException sfEx = (SalesforceException) ex.getCause();
        Assertions.assertThat(sfEx.getStatusCode()).isEqualTo(404);

    }

    @Test
    public void testGetAll() {
        Jobs jobs = template().requestBody("salesforce:bulk2GetAllJobs", "", Jobs.class);
        assertNotNull(jobs);
    }

    private Job createJob(Job job) {
        job = template().requestBody("salesforce:bulk2CreateJob", job, Job.class);
        assertNotNull(job.getId(), "Missing JobId");
        return job;
    }

    /**
     *  Bulk API 2.0 is available in API version 41.0 and later.
     *  Query jobs in Bulk API 2.0 are available in API version 47.0 and later.
     */
    protected String salesforceApiVersionToUse() {
        return "47.0";
    }
}