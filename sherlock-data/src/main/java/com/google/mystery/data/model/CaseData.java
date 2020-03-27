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

import com.google.appengine.api.datastore.Entity;
import com.google.mystery.data.DataManager;

/**
 * Case specific data.
 *
 * @author ilyaplatonov
 */
public class CaseData {
  private final String caseDataId;

  public CaseData(String caseDataId) {
    this.caseDataId = caseDataId;
  }

  public String getCaseDataId() {
    return caseDataId;
  }

  public static Entity toEntity(CaseData data) {
    Entity entity = new Entity(DataManager.CASE_DATA_KIND, data.getCaseDataId());
    return entity;
  }

  public static CaseData fromEntity(Entity entity) {
    return new CaseData(entity.getKey().getName());
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((caseDataId == null) ? 0 : caseDataId.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    CaseData other = (CaseData) obj;
    if (caseDataId == null) {
      if (other.caseDataId != null) return false;
    } else if (!caseDataId.equals(other.caseDataId)) return false;
    return true;
  }

  @Override
  public String toString() {
    return "CaseData [caseDataId=" + caseDataId + "]";
  }
}
