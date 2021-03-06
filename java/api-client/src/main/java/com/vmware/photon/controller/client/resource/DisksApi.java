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

package com.vmware.photon.controller.client.resource;

import com.vmware.photon.controller.api.PersistentDisk;
import com.vmware.photon.controller.api.ResourceList;
import com.vmware.photon.controller.api.Task;
import com.vmware.photon.controller.client.RestClient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.util.concurrent.FutureCallback;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import java.io.IOException;

/**
 * Disks Api.
 */
public class DisksApi extends ApiBase {
  public DisksApi(RestClient restClient) {
    super(restClient);
  }

  @Override
  public String getBasePath() {
    return "/disks";
  }

  /**
   * Get details about the specified disk.
   *
   * @param diskId
   * @return Disk details
   * @throws java.io.IOException
   */
  public PersistentDisk getDisk(String diskId) throws IOException {
    String path = String.format("%s/%s", getBasePath(), diskId);

    HttpResponse httpResponse = this.restClient.perform(RestClient.Method.GET, path, null);
    this.restClient.checkResponse(httpResponse, HttpStatus.SC_OK);

    return this.restClient.parseHttpResponse(
        httpResponse,
        new TypeReference<PersistentDisk>() {
        }
    );
  }

  /**
   * This method get a disk object.
   *
   * @param diskId
   * @param responseCallback
   * @throws IOException
   */
  public void getDiskAsync(final String diskId, final FutureCallback<PersistentDisk> responseCallback) throws
      IOException {
    String path = String.format("%s/%s", getBasePath(), diskId);

    getObjectByPathAsync(path, responseCallback, new TypeReference<PersistentDisk>() {
    });
  }

  /**
   * Get tasks associated with the specified disk.
   *
   * @param diskId
   * @return {@link ResourceList} of {@link Task}
   * @throws IOException
   */
  public ResourceList<Task> getTasksForDisk(String diskId) throws IOException {
    String path = String.format("%s/%s/tasks", getBasePath(), diskId);

    HttpResponse httpResponse = this.restClient.perform(RestClient.Method.GET, path, null);
    this.restClient.checkResponse(httpResponse, HttpStatus.SC_OK);

    return this.restClient.parseHttpResponse(
        httpResponse,
        new TypeReference<ResourceList<Task>>() {
        }
    );
  }

  /**
   * This method gets all tasks associated with a disk.
   *
   * @param diskId
   * @param responseCallback
   * @throws IOException
   */
  public void getTasksForDiskAsync(final String diskId, final FutureCallback<ResourceList<Task>> responseCallback)
      throws IOException {
    String path = String.format("%s/%s/tasks", getBasePath(), diskId);

    getObjectByPathAsync(path, responseCallback, new TypeReference<ResourceList<Task>>() {
    });
  }

  /**
   * @param diskId
   * @return
   * @throws IOException
   */
  public Task delete(String diskId) throws IOException {
    String path = getBasePath() + "/" + diskId;

    HttpResponse response = this.restClient.perform(RestClient.Method.DELETE, path, null);

    this.restClient.checkResponse(response, HttpStatus.SC_CREATED);
    return parseTaskFromHttpResponse(response);
  }

  /**
   * This method deletes a disk.
   *
   * @param diskId
   * @param responseCallback
   * @throws IOException
   */
  public void deleteAsync(final String diskId, final FutureCallback<Task> responseCallback) throws
      IOException {

    deleteObjectAsync(diskId, responseCallback);
  }
}
