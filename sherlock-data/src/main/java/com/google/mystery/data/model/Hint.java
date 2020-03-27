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

package com.google.mystery.data.model;

import java.util.List;
import java.util.Set;
import com.google.appengine.api.datastore.Entity;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.mystery.data.DataManager;
import com.google.mystery.data.DataUtil;

public class Hint {
  /** referencing case data id. */
  private final String caseDataId;
  /** set of places that need to be visited for hint to be activated. */
  private final Set<String> precondition;
  /** hint text. */
  private final String hint;
  /** id to reference hint. */
  private final String id;
  /** suggestions that appear with this. */
  private final List<String> suggestions;

  public Hint(
      String caseDataId,
      Set<String> precondition,
      String hint,
      String id,
      List<String> suggestions) {
    this.caseDataId = caseDataId;
    this.precondition = precondition;
    this.hint = hint;
    this.id = id;
    this.suggestions = ImmutableList.copyOf(suggestions);
  }

  public Set<String> getPrecondition() {
    return precondition;
  }

  public String getHint() {
    return hint;
  }

  public String getId() {
    return id;
  }

  public List<String> getSuggestions() {
    return suggestions;
  }

  public String getCaseDataId() {
    return caseDataId;
  }

  public static Entity toEntity(Hint hint) {
    Entity entity = new Entity(DataManager.HINT_KIND);
    entity.setIndexedProperty("hintId", hint.getId());
    entity.setIndexedProperty(DataManager.CASE_DATA_ID, hint.getCaseDataId());
    entity.setUnindexedProperty("hint", hint.getHint());
    DataUtil.setStringListProperty(entity, "precondition", hint.getPrecondition());
    DataUtil.setStringListProperty(entity, "suggestions", hint.getSuggestions());
    return entity;
  }

  public static Hint fromEntity(Entity entity) {
    Set<String> precondition = ImmutableSet.copyOf(DataUtil.getStringList(entity, "precondition"));
    List<String> suggestions = ImmutableList.copyOf(DataUtil.getStringList(entity, "suggestions"));
    return new Hint(
        entity.getProperty(DataManager.CASE_DATA_ID).toString(),
        precondition,
        entity.getProperty("hint").toString(),
        entity.getProperty("hintId").toString(),
        suggestions);
  }

  @Override
  public String toString() {
    return "Hint [caseDataId="
        + caseDataId
        + ", precondition="
        + precondition
        + ", hint="
        + hint
        + ", id="
        + id
        + ", suggestions="
        + suggestions
        + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((caseDataId == null) ? 0 : caseDataId.hashCode());
    result = prime * result + ((hint == null) ? 0 : hint.hashCode());
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    result = prime * result + ((precondition == null) ? 0 : precondition.hashCode());
    result = prime * result + ((suggestions == null) ? 0 : suggestions.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    Hint other = (Hint) obj;
    if (caseDataId == null) {
      if (other.caseDataId != null) return false;
    } else if (!caseDataId.equals(other.caseDataId)) return false;
    if (hint == null) {
      if (other.hint != null) return false;
    } else if (!hint.equals(other.hint)) return false;
    if (id == null) {
      if (other.id != null) return false;
    } else if (!id.equals(other.id)) return false;
    if (precondition == null) {
      if (other.precondition != null) return false;
    } else if (!precondition.equals(other.precondition)) return false;
    if (suggestions == null) {
      if (other.suggestions != null) return false;
    } else if (!suggestions.equals(other.suggestions)) return false;
    return true;
  }
}
