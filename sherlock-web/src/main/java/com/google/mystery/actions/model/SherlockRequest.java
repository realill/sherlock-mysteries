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
import com.google.common.collect.ImmutableSet;

/**
 * Game action request.
 *
 * @author ilyaplatonov
 */
public class SherlockRequest {
  public static enum Flag {
    /** if request is for speaker type device. */
    SPEAKER,
    /** if device support opening browser links. */
    WEB_BROWSER,
    /** indicates that case just started. */
    CASE_START,
    /** client supports Interactive Canvas API. */
    CANVAS
  }

  /** string input from user, or <code>null</code> if there are not input. */
  private final String input;
  /** player id. */
  private final String sessionid;
  /** game action coming from api.ai. */
  private final String action;
  /** parameters of this action. */
  private final Map<String, String> parameters;

  private Set<Flag> flags;
  /** case data id in scope of this request. */
  private final String caseDataId;

  public SherlockRequest(String input, String sessionid, String action,
      Map<String, String> parameters, Set<Flag> flags, String caseDataId) {
    this.input = input;
    this.sessionid = sessionid;
    this.action = action;
    this.parameters = parameters;
    this.flags = ImmutableSet.copyOf(flags);
    this.caseDataId = caseDataId;
  }

  /** Checks if request is ran on speaker type device. */
  public boolean isSpeaker() {
    return flags.contains(Flag.SPEAKER);
  }

  /** Checks if device support browser. */
  public boolean hasBrowser() {
    return flags.contains(Flag.WEB_BROWSER);
  }

  /**
   * Checks if new case just started.
   */
  public boolean isNewCase() {
    return flags.contains(Flag.CASE_START);
  }

  public boolean isCanvas() {
    return getFlags().contains(Flag.CANVAS);
  }

  public String getAction() {
    return action;
  }

  public Map<String, String> getParameters() {
    return parameters;
  }

  public String getParameter(String name) {
    return parameters.get(name);
  }

  public String getSessionid() {
    return sessionid;
  }

  public String getInput() {
    return input;
  }

  public String getCaseDataId() {
    return caseDataId;
  }

  public Set<Flag> getFlags() {
    return flags;
  }
}
