/*
 * Copyright 2015 VMware, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, without warranties or
 * conditions of any kind, EITHER EXPRESS OR IMPLIED.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.vmware.photon.controller.cloudstore.dcp.entity;

import com.vmware.photon.controller.api.QuotaLineItem;
import com.vmware.photon.controller.api.QuotaUnit;
import com.vmware.photon.controller.common.dcp.BasicServiceHost;
import com.vmware.photon.controller.common.dcp.DcpRestClient;
import com.vmware.photon.controller.common.dcp.exceptions.BadRequestException;
import com.vmware.photon.controller.common.dcp.exceptions.DcpRuntimeException;
import com.vmware.photon.controller.common.thrift.StaticServerSet;
import com.vmware.xenon.common.Operation;
import com.vmware.xenon.common.Service;
import com.vmware.xenon.common.ServiceErrorResponse;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.testng.Assert.fail;

import java.net.InetSocketAddress;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Executors;

/**
 * Tests {@link ResourceTicketService}.
 */
public class ResourceTicketServiceTest {

  private static DcpRestClient dcpRestClient;
  private static BasicServiceHost host;
  private static ResourceTicketService service;
  private static ResourceTicketService.State testState;

  private static void commonSetup() throws Throwable {
    service = new ResourceTicketService();
    host = BasicServiceHost.create(BasicServiceHost.BIND_ADDRESS,
        BasicServiceHost.BIND_PORT,
        null,
        ResourceTicketServiceFactory.SELF_LINK,
        10, 10);

    StaticServerSet serverSet = new StaticServerSet(
        new InetSocketAddress(host.getPreferredAddress(), host.getPort()));

    dcpRestClient = new DcpRestClient(serverSet, Executors.newFixedThreadPool(1));
    dcpRestClient.start();

    testState = new ResourceTicketService.State();
    testState.name = UUID.randomUUID().toString();
    testState.tenantId = UUID.randomUUID().toString();
    testState.parentId = UUID.randomUUID().toString();
    testState.limitMap = new HashMap<>();
    QuotaLineItem costItem = new QuotaLineItem();
    costItem.setKey("key1");
    costItem.setValue(10.0);
    costItem.setUnit(QuotaUnit.COUNT);
    testState.limitMap.put(costItem.getKey(), costItem);

    host.startServiceSynchronously(new ResourceTicketServiceFactory(), null);
  }

  private static void commonTearDown() throws Throwable {
    if (host != null) {
      BasicServiceHost.destroy(host);
    }

    service = null;
    dcpRestClient.stop();
  }

  /**
   * Dummy test case to make Intellij recognize this as a test class.
   */
  @Test
  private void dummy() {
  }

  /**
   * Tests for the constructors.
   */
  public class InitializationTest {
    @BeforeMethod
    public void setUp() {
      service = new ResourceTicketService();
    }

    /**
     * Test that the service starts with the expected options.
     */
    @Test
    public void testServiceOptions() {
      EnumSet<Service.ServiceOption> expected = EnumSet.of(
          Service.ServiceOption.PERSISTENCE,
          Service.ServiceOption.REPLICATION,
          Service.ServiceOption.OWNER_SELECTION,
          Service.ServiceOption.INSTRUMENTATION);
      assertThat(service.getOptions(), is(expected));
    }
  }

  /**
   * Tests for the handleStart method.
   */
  public class HandleStartTest {
    @BeforeMethod
    public void setUp() throws Throwable {
      commonSetup();
    }

    @AfterMethod
    public void tearDown() throws Throwable {
      commonTearDown();
    }

    /**
     * Test start of service with valid start state.
     *
     * @throws Throwable
     */
    @Test
    public void testStartState() throws Throwable {
      Operation result = dcpRestClient.post(ResourceTicketServiceFactory.SELF_LINK, testState);

      assertThat(result.getStatusCode(), is(200));
      ResourceTicketService.State createdState = result.getBody(ResourceTicketService.State.class);
      assertThat(createdState.name, is(equalTo(testState.name)));
      ResourceTicketService.State savedState =
          host.getServiceState(ResourceTicketService.State.class, createdState.documentSelfLink);
      assertThat(savedState.name, is(equalTo(testState.name)));
    }
  }

  /**
   * Tests for the handlePatch method.
   */
  public class HandlePatchTest {
    @BeforeMethod
    public void setUp() throws Throwable {
      commonSetup();
    }

    @AfterMethod
    public void tearDown() throws Throwable {
      commonTearDown();
    }

    /**
     * Test consume within limits succeeds.
     *
     * @throws Throwable
     */
    @Test
    public void testConsumeWithinLimits() throws Throwable {

      Operation result = dcpRestClient.post(ResourceTicketServiceFactory.SELF_LINK, testState);
      assertThat(result.getStatusCode(), is(200));
      ResourceTicketService.State createdState = result.getBody(ResourceTicketService.State.class);

      ResourceTicketService.Patch patch = new ResourceTicketService.Patch();
      patch.patchtype = ResourceTicketService.Patch.PatchType.USAGE_CONSUME;
      patch.cost = new HashMap<>();

      QuotaLineItem costItem = new QuotaLineItem();
      costItem.setKey("key1");
      costItem.setValue(testState.limitMap.get("key1").getValue() / 2);
      costItem.setUnit(QuotaUnit.COUNT);
      patch.cost.put(costItem.getKey(), costItem);

      costItem = new QuotaLineItem();
      costItem.setKey("key2");
      costItem.setValue(testState.limitMap.get("key1").getValue() / 2);
      costItem.setUnit(QuotaUnit.COUNT);
      patch.cost.put(costItem.getKey(), costItem);

      dcpRestClient.patch(createdState.documentSelfLink, patch);

      Operation found = dcpRestClient.get(createdState.documentSelfLink);
      ResourceTicketService.State patchedState = found.getBody(ResourceTicketService.State.class);
      assertThat(patchedState.usageMap.get("key1").getValue(), is(testState.limitMap.get("key1").getValue() / 2));
      assertThat(patchedState.usageMap.get("key2").getValue(), is(testState.limitMap.get("key1").getValue() / 2));

      dcpRestClient.patch(createdState.documentSelfLink, patch);

      found = dcpRestClient.get(createdState.documentSelfLink);
      patchedState = found.getBody(ResourceTicketService.State.class);
      assertThat(patchedState.usageMap.get("key1").getValue(), is(testState.limitMap.get("key1").getValue()));
      assertThat(patchedState.usageMap.get("key2").getValue(), is(testState.limitMap.get("key1").getValue()));

      patch.cost.remove("key1");
      dcpRestClient.patch(createdState.documentSelfLink, patch);

      found = dcpRestClient.get(createdState.documentSelfLink);
      patchedState = found.getBody(ResourceTicketService.State.class);
      assertThat(patchedState.usageMap.get("key1").getValue(), is(testState.limitMap.get("key1").getValue()));
      assertThat(patchedState.usageMap.get("key2").getValue(), is(testState.limitMap.get("key1").getValue() * 1.5));
    }

    /**
     * Test consume above limits fails.
     *
     * @throws Throwable
     */
    @Test()
    public void testConsumeAboveLimits() throws Throwable {
      Operation result = dcpRestClient.post(ResourceTicketServiceFactory.SELF_LINK, testState);
      assertThat(result.getStatusCode(), is(200));
      ResourceTicketService.State createdState = result.getBody(ResourceTicketService.State.class);

      ResourceTicketService.Patch patch = new ResourceTicketService.Patch();
      patch.patchtype = ResourceTicketService.Patch.PatchType.USAGE_CONSUME;
      patch.cost = new HashMap<>();


      QuotaLineItem costItem = new QuotaLineItem();
      costItem.setKey("key1");
      costItem.setValue(testState.limitMap.get("key1").getValue() + 1);
      costItem.setUnit(QuotaUnit.COUNT);
      patch.cost.put(costItem.getKey(), costItem);

      costItem = new QuotaLineItem();
      costItem.setKey("key2");
      costItem.setValue(testState.limitMap.get("key1").getValue() / 2);
      costItem.setUnit(QuotaUnit.COUNT);
      patch.cost.put(costItem.getKey(), costItem);

      try {
        dcpRestClient.patch(createdState.documentSelfLink, patch);
        fail("resource ticket consume above limits should have failed");
      } catch (BadRequestException e) {
        ServiceErrorResponse serviceErrorResponse =
            e.getCompletedOperation().getBody(ServiceErrorResponse.class);
        assertThat(serviceErrorResponse.message,
            containsString("Not enough quota: Current Limit: " + testState.limitMap.get("key1").toString() +
                ", desiredUsage " + patch.cost.get("key1").toString()));
      }

      Operation found = dcpRestClient.get(createdState.documentSelfLink);
      ResourceTicketService.State patchedState = found.getBody(ResourceTicketService.State.class);
      assertThat(patchedState.usageMap.get("key1"), is(nullValue()));
      assertThat(patchedState.usageMap.get("key2"), is(nullValue()));
    }

    /**
     * Test consume above limits returns all the usage and limit details.
     *
     * @throws Throwable
     */
    @Test
    public void testConsumeAboveLimitsErrorDetails() throws Throwable {
      Operation result = dcpRestClient.post(ResourceTicketServiceFactory.SELF_LINK, testState);
      assertThat(result.getStatusCode(), is(200));
      ResourceTicketService.State createdState = result.getBody(ResourceTicketService.State.class);

      ResourceTicketService.Patch patch = new ResourceTicketService.Patch();
      patch.patchtype = ResourceTicketService.Patch.PatchType.USAGE_CONSUME;
      patch.cost = new HashMap<>();

      QuotaLineItem costItem = new QuotaLineItem();
      costItem.setKey("key1");
      costItem.setValue(testState.limitMap.get("key1").getValue() + 1);
      costItem.setUnit(QuotaUnit.COUNT);
      patch.cost.put(costItem.getKey(), costItem);

      try {
        dcpRestClient.patch(createdState.documentSelfLink, patch);
        fail("resource ticket consume above limits should have failed");
      } catch (BadRequestException e) {
        ResourceTicketService.QuotaErrorResponse quotaErrorResponse =
            e.getCompletedOperation().getBody(ResourceTicketService.QuotaErrorResponse.class);
        assertThat(quotaErrorResponse.limit, is(notNullValue()));
        assertThat(quotaErrorResponse.limit.getValue(), is(testState.limitMap.get("key1").getValue()));
        assertThat(quotaErrorResponse.usage, is(notNullValue()));
        assertThat(quotaErrorResponse.usage.getValue(), is(0.0));
        assertThat(quotaErrorResponse.newUsage, is(notNullValue()));
        assertThat(quotaErrorResponse.newUsage.getValue(), is(costItem.getValue()));
      }
    }

    /**
     * Test with patch type NONE.
     *
     * @throws Throwable
     */
    @Test()
    public void testInvalidPatchType() throws Throwable {
      Operation result = dcpRestClient.post(ResourceTicketServiceFactory.SELF_LINK, testState);
      assertThat(result.getStatusCode(), is(200));
      ResourceTicketService.State createdState = result.getBody(ResourceTicketService.State.class);

      ResourceTicketService.Patch patch = new ResourceTicketService.Patch();

      QuotaLineItem costItem = new QuotaLineItem();
      costItem.setKey("key1");
      costItem.setValue(testState.limitMap.get("key1").getValue() + 1);
      costItem.setUnit(QuotaUnit.COUNT);
      patch.cost = new HashMap<>();
      patch.cost.put(costItem.getKey(), costItem);

      try {
        dcpRestClient.patch(createdState.documentSelfLink, patch);
      } catch (DcpRuntimeException e) {
        assertThat(e.getMessage(),
            containsString("PatchType {NONE} in patchOperation"));
        return;
      }

      fail("resource ticket with invalid patch type should have failed");
    }

    /**
     * Test return of usage.
     *
     * @throws Throwable
     */
    @Test
    public void testReturnUsage() throws Throwable {

      Operation result = dcpRestClient.post(ResourceTicketServiceFactory.SELF_LINK, testState);
      assertThat(result.getStatusCode(), is(200));
      ResourceTicketService.State createdState = result.getBody(ResourceTicketService.State.class);

      ResourceTicketService.Patch patch = new ResourceTicketService.Patch();
      patch.patchtype = ResourceTicketService.Patch.PatchType.USAGE_CONSUME;
      patch.cost = new HashMap<>();

      QuotaLineItem costItem = new QuotaLineItem();
      costItem.setKey("key1");
      costItem.setValue(testState.limitMap.get("key1").getValue() / 2);
      costItem.setUnit(QuotaUnit.COUNT);
      patch.cost.put(costItem.getKey(), costItem);

      costItem = new QuotaLineItem();
      costItem.setKey("key2");
      costItem.setValue(testState.limitMap.get("key1").getValue() / 2);
      costItem.setUnit(QuotaUnit.COUNT);
      patch.cost.put(costItem.getKey(), costItem);

      dcpRestClient.patch(createdState.documentSelfLink, patch);

      Operation found = dcpRestClient.get(createdState.documentSelfLink);
      ResourceTicketService.State patchedState = found.getBody(ResourceTicketService.State.class);
      assertThat(patchedState.usageMap.get("key1").getValue(), is(testState.limitMap.get("key1").getValue() / 2));
      assertThat(patchedState.usageMap.get("key2").getValue(), is(testState.limitMap.get("key1").getValue() / 2));

      patch.patchtype = ResourceTicketService.Patch.PatchType.USAGE_RETURN;
      dcpRestClient.patch(createdState.documentSelfLink, patch);

      found = dcpRestClient.get(createdState.documentSelfLink);
      patchedState = found.getBody(ResourceTicketService.State.class);
      assertThat(patchedState.usageMap.get("key1").getValue(), is(0.0));
      assertThat(patchedState.usageMap.get("key2").getValue(), is(0.0));
    }
  }
}
