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

package com.vmware.photon.controller.apife.commands.steps;

import com.vmware.photon.controller.api.HostState;
import com.vmware.photon.controller.api.common.entities.base.BaseEntity;
import com.vmware.photon.controller.api.common.exceptions.external.ExternalException;
import com.vmware.photon.controller.apife.backends.HostBackend;
import com.vmware.photon.controller.apife.backends.StepBackend;
import com.vmware.photon.controller.apife.commands.tasks.TaskCommand;
import com.vmware.photon.controller.apife.entities.HostEntity;
import com.vmware.photon.controller.apife.entities.StepEntity;
import com.vmware.photon.controller.apife.exceptions.external.HostStateChangeException;
import com.vmware.photon.controller.common.clients.exceptions.RpcException;

import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * StepCommand for host enter suspended mode.
 * <p>
 * Host can only go into SUSPENDED mode from READY or MAINTENANCE modes:
 * READY <=> SUSPENDED <=> MAINTENANCE MODE => DEPROVISIONED
 * <p>
 * When the host is in SUSPENDED mode no new VMs can be created on this host.
 * All existing VMs will be killed or moved out before the host can be set to MAINTENANCE mode.
 */
public class HostEnterSuspendedModeStepCmd extends StepCommand {
  private static final Logger logger = LoggerFactory.getLogger(HostEnterSuspendedModeStepCmd.class);

  private final HostBackend hostBackend;

  public HostEnterSuspendedModeStepCmd(TaskCommand taskCommand,
                                       StepBackend stepBackend,
                                       StepEntity stepEntity,
                                       HostBackend hostBackend) {
    super(taskCommand, stepBackend, stepEntity);
    this.hostBackend = hostBackend;
  }

  @Override
  protected void execute() throws ExternalException {

    // Precondition check: only one host can be referenced.
    List<BaseEntity> entityList = step.getTransientResourceEntities();
    HostEntity hostEntity = (HostEntity) Iterables.getOnlyElement(entityList);

    // Call deployer for action and error handling.
    try {
      taskCommand.getDeployerClient().enterSuspendedMode(hostEntity.getId());
      hostBackend.updateState(hostEntity, HostState.SUSPENDED);
    } catch (InterruptedException | RpcException e) {
      HostStateChangeException exception = new HostStateChangeException(
          hostEntity,
          HostState.SUSPENDED,
          e);
      throw exception;
    }
  }

  @Override
  protected void cleanup() {

  }
}
