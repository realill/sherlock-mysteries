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

import java.io.Serializable;

public class SessionActiveSuggestion implements Serializable {
  private static final long serialVersionUID = -4914921694192899069L;

  final String suggestion;
  final int relevancy;

  public SessionActiveSuggestion(String suggestion, int relevancy) {
    super();
    this.suggestion = suggestion;
    this.relevancy = relevancy;
  }

  public String getSuggestion() {
    return suggestion;
  }

  public int getRelevancy() {
    return relevancy;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + relevancy;
    result = prime * result + ((suggestion == null) ? 0 : suggestion.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    SessionActiveSuggestion other = (SessionActiveSuggestion) obj;
    if (relevancy != other.relevancy) return false;
    if (suggestion == null) {
      if (other.suggestion != null) return false;
    } else if (!suggestion.equals(other.suggestion)) return false;
    return true;
  }
}
