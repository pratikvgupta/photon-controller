# Copyright 2015 VMware, Inc. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License.  You may obtain a copy
# of the License at http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, without
# warranties or conditions of any kind, EITHER EXPRESS OR IMPLIED.  See the
# License for then specific language governing permissions and limitations
# under the License.

import enum
from enum import Enum


@enum.unique
class ServiceName(Enum):
    SERVICE_ADDRESS = 1
    REQUEST_ID = 2
    AGENT_CONFIG = 3
    LOCKED_VMS = 4
    STATE = 5
    MODE = 6
    DATASTORE_TAGS = 7
    HYPERVISOR = 8
    REGISTRANT = 9
    VIM_CLIENT = 10
