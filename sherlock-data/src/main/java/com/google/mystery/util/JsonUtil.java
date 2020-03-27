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

package com.google.mystery.util;

import java.util.Collection;
import java.util.List;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public class JsonUtil {
  public static List<String> fromStringArray(JsonArray array) {
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    for (JsonElement e : array) {
      builder.add(e.getAsString());
    }
    return builder.build();
  }

  public static JsonArray toStringArray(Collection<String> collection) {
    JsonArray array = new JsonArray();
    for (String e : collection) {
      array.add(e);
    }
    return array;
  }
}
