# Copyright 2015 VMware, Inc. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License. You may obtain a copy of
# the License at http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software distributed
# under the License is distributed on an "AS IS" BASIS, without warranties or
# conditions of any kind, EITHER EXPRESS OR IMPLIED. See the License for the
# specific language governing permissions and limitations under the License.

require "spec_helper"

describe "deployment upgrade lifecycle", life_cycle: true do
  describe "#pause_system" do
    let(:deployment) do
      client.find_all_api_deployments.items.first
    end

    let(:items_to_cleanup) { [] }

    after(:each) do
      client.resume_system(deployment.id)
    end

    after(:each) do
      items_to_cleanup.each do |item|
        item.delete unless item.nil?
      end
    end

    context "when deployment exists" do
      it "should pause/resume system successfully" do
        expect(client.find_all_api_deployments.items.size).to eq 1

        client.resume_system(deployment.id)
        2.times do
          client.pause_system(deployment.id)

          # tests that while system is paused no POSTs are accepted
          5.times do
            begin
              items_to_cleanup << create_tenant(name: random_name("tenant-"))
              fail("pause_system should fail")
            rescue EsxCloud::ApiError => e
              expect(e.response_code).to eq 403
              expect(e.errors.size).to eq 1
              expect(e.errors[0].code).to eq "SystemPaused"
              expect(e.errors[0].message).to match /System is paused/
            end
          end

          client.resume_system(deployment.id)

          # testing that after resuming the system we accept posts again
          items_to_cleanup << create_tenant(name: random_name("tenant-"))
        end
      end
    end

    context "when deployment does not exist" do
      it "should fail to pause system" do
        error_msg = "Deployment #non-existing-deployment not found"
        begin
          client.pause_system("non-existing-deployment")
          fail("pause system should fail when the deployment does not exist")
        rescue EsxCloud::ApiError => e
          expect(e.response_code).to eq 404
          expect(e.errors.size).to eq 1
          expect(e.errors.first.code).to eq("DeploymentNotFound")
          expect(e.errors.first.message).to include(error_msg)
        end
      end
    end
  end
end
