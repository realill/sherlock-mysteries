// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.mystery.actions.model;

import java.util.Map;
import java.util.Set;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.mystery.actions.model.SherlockRequest.Flag;

public class SherlockRequestBuilder {
  private String input;
  private String sessionid;
  private String action;
  private Map<String, String> parameters;
  private Set<Flag> flags;
  private String caseDataId;

  public SherlockRequestBuilder(SherlockRequest request) {
    input = request.getInput();
    sessionid = request.getSessionid();
    action = request.getAction();
    parameters = Maps.newHashMap(request.getParameters());
    flags = Sets.newHashSet(request.getFlags());
    caseDataId = request.getCaseDataId();
  }

  public SherlockRequestBuilder(String input, String sessionid, String action) {
    this.input = input;
    this.sessionid = sessionid;
    this.action = action;
    parameters = Maps.newHashMap();
    flags = Sets.newHashSet();
    caseDataId = null;
  }

  public SherlockRequestBuilder parameters(Map<String, String> paramaters) {
    this.parameters = paramaters;
    return this;
  }

  public SherlockRequestBuilder flags(Set<SherlockRequest.Flag> flags) {
    this.flags = flags;
    return this;
  }

  public SherlockRequestBuilder caseDataId(String caseDataId) {
    this.caseDataId = caseDataId;
    return this;
  }

  public SherlockRequest build() {
    return new SherlockRequest(input, sessionid, action, parameters, flags, caseDataId);
  }

  public SherlockRequestBuilder addFlag(Flag flag) {
    flags.add(flag);
    return this;
  }
}
