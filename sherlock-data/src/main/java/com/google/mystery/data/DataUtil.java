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

package com.google.mystery.data;

import java.util.Collection;
import java.util.List;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Text;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.mystery.util.JsonUtil;

/**
 * Utility class to work with datastore related data
 *
 * @author ilyaplatonov
 */
public class DataUtil {
  private static Gson gson = new Gson();

  /**
   * gets entity property and presents it as list of strings using json array
   *
   * @return empty list if property is not set of list of values
   */
  public static List<String> getStringList(Entity entity, String propertName) {
    if (!entity.hasProperty(propertName)) {
      return ImmutableList.of();
    }
    JsonArray locationsBacklogJson =
        new JsonParser()
            .parse(((Text) entity.getProperty(propertName)).getValue())
            .getAsJsonArray();

    return JsonUtil.fromStringArray(locationsBacklogJson);
  }

  /** sets collection of strings property into entity using json array format string */
  public static void setStringListProperty(
      Entity entity, String propertyName, Collection<String> collection) {
    JsonArray locationBacklog = JsonUtil.toStringArray(collection);
    entity.setUnindexedProperty(propertyName, new Text(locationBacklog.toString()));
  }

  /** Sets collection of objects into entity. It uses json format for serialization. */
  public static void setListProperty(Entity entity, String propertyName, Collection<?> collection) {
    JsonArray array = new JsonArray();
    for (Object object : collection) {
      array.add(gson.toJsonTree(object));
    }
    entity.setUnindexedProperty(propertyName, new Text(array.toString()));
  }

  public static <T> List<T> getList(Entity entity, String propertName, Class<T> clazz) {
    if (!entity.hasProperty(propertName)) {
      return ImmutableList.of();
    }
    JsonArray arrayJson =
        new JsonParser()
            .parse(((Text) entity.getProperty(propertName)).getValue())
            .getAsJsonArray();
    ImmutableList.Builder<T> listBuilder = ImmutableList.builder();
    for (JsonElement json : arrayJson) {
      listBuilder.add(gson.fromJson(json, clazz));
    }

    return listBuilder.build();
  }

  /** @return value of property if set or <code>null</code> */
  public static String getOptionalString(Entity entity, String propertyName) {
    if (entity.hasProperty(propertyName)) {
      return (String) entity.getProperty(propertyName);
    }
    return null;
  }
}
