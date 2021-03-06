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

package com.vmware.photon.controller.chairman;

import com.vmware.photon.controller.chairman.hierarchy.FlowFactory;
import com.vmware.photon.controller.common.dcp.DcpRestClient;
import com.vmware.photon.controller.common.manifest.BuildInfo;
import com.vmware.photon.controller.common.thrift.ClientPool;
import com.vmware.photon.controller.common.thrift.ClientPoolFactory;
import com.vmware.photon.controller.common.thrift.ClientPoolOptions;
import com.vmware.photon.controller.common.thrift.ClientProxy;
import com.vmware.photon.controller.common.thrift.ClientProxyFactory;
import com.vmware.photon.controller.common.thrift.ServerSet;
import com.vmware.photon.controller.common.zookeeper.DataDictionary;
import com.vmware.photon.controller.common.zookeeper.ZookeeperServerSetFactory;
import com.vmware.photon.controller.scheduler.root.gen.RootScheduler;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.apache.curator.framework.CuratorFramework;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Chairman Guice module.
 */
public class ChairmanModule extends AbstractModule {

  private final Config config;

  public ChairmanModule(Config config) {
    this.config = config;
  }

  @Override
  protected void configure() {
    bind(Config.class).toInstance(config);
    bindConstant().annotatedWith(Config.Bind.class).to(config.getBind());
    bindConstant().annotatedWith(Config.RegistrationAddress.class).to(config.getRegistrationAddress());
    bindConstant().annotatedWith(Config.Port.class).to(config.getPort());
    bind(HierarchyConfig.class).toInstance(config.getHierarchy());
    bind(BuildInfo.class).toInstance(BuildInfo.get(ChairmanModule.class));

    install(new FactoryModuleBuilder().build(FlowFactory.class));
    bind(ScheduledExecutorService.class)
        .toInstance(Executors.newScheduledThreadPool(4));
  }

  @Provides
  @Singleton
  @RootSchedulerServerSet
  public ServerSet getRootSchedulerServerSet(ZookeeperServerSetFactory serverSetFactory) {
    return serverSetFactory.createServiceServerSet("root-scheduler", true);
  }

  @Provides
  @Singleton
  ClientPool<RootScheduler.AsyncClient> getRootSchedulerClientPool(
      @RootSchedulerServerSet ServerSet serverSet,
      ClientPoolFactory<RootScheduler.AsyncClient> clientPoolFactory) {

    ClientPoolOptions options = new ClientPoolOptions()
        .setMaxClients(10)
        .setMaxWaiters(10)
        .setTimeout(10, TimeUnit.SECONDS)
        .setServiceName("RootScheduler");

    return clientPoolFactory.create(serverSet, options);
  }

  @Provides
  ClientProxy<RootScheduler.AsyncClient> getRootSchedulerClientProxy(
      ClientProxyFactory<RootScheduler.AsyncClient> factory,
      ClientPool<RootScheduler.AsyncClient> clientPool) {
    return factory.create(clientPool);
  }

  @Provides
  @Singleton
  @ChairmanServerSet
  public ServerSet getChairmanLeaderServerSet(ZookeeperServerSetFactory serverSetFactory) {
        return serverSetFactory.createServiceServerSet("chairman", true);
  }

  @Provides
  @Singleton
  @HostConfigRegistry
  public DataDictionary getConfigDictionary(CuratorFramework zkClient) {
    return new DataDictionary(zkClient, Executors.newCachedThreadPool(), "hosts");
  }

  @Provides
  @Singleton
  @HostMissingRegistry
  public DataDictionary getMissingDictionary(CuratorFramework zkClient) {
    return new DataDictionary(zkClient, Executors.newCachedThreadPool(), "missing");
  }

  @Provides
  @Singleton
  @RolesRegistry
  public DataDictionary getRolesDictionary(CuratorFramework zkClient) {
    return new DataDictionary(zkClient, Executors.newCachedThreadPool(), "roles");
  }

  @Provides
  @Singleton
  public DcpRestClient getDcpRestClient(ZookeeperServerSetFactory serverSetFactory) {
    ServerSet serverSet = serverSetFactory.createServiceServerSet("cloudstore", true);
    DcpRestClient client = new DcpRestClient(serverSet, Executors.newFixedThreadPool(4));
    client.start();
    return client;
  }
}
