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

package com.google.mystery.mvc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TesterRequest {
  private String input;
  private String action;
  private String speech;
  private Map<String, String> parameters = new HashMap<>();
  private List<String> contexts = new ArrayList<>();

  public List<String> getContexts() {
    return contexts;
  }

  public static TesterRequest action(String action) {
    return new TesterRequest(action);
  }

  TesterRequest(String action) {
    this.action = action;
  }

  public TesterRequest input(String input) {
    this.input = input;
    return this;
  }

  public TesterRequest param(String name, String value) {
    parameters.put(name, value);
    return this;
  }

  public String getAction() {
    return action;
  }

  public Map<String, String> getParameters() {
    return parameters;
  }

  public TesterRequest speakerDevice() {
    this.contexts.add("actions_capability_screen_output");
    return this;
  }

  public String getInput() {
    return input;
  }

  public TesterRequest speech(String speech) {
    this.speech = speech;
    return this;
  }

  public String getSpeech() {
    return speech;
  }
}
