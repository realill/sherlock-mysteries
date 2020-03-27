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

/**
 * London directory entry
 *
 * @author ilyaplatonov
 */
public class DirectoryEntry {
  private final String location;
  private final String name;
  private final String category;
  /** used to improve search. */
  private final String keywords;

  public DirectoryEntry(String location, String name, String category, String keywords) {
    this.location = location;
    this.name = name;
    this.category = category;
    this.keywords = keywords;
  }

  public DirectoryEntry(String location, String name) {
    this(location, name, null, null);
  }

  public String getLocation() {
    return location;
  }

  public String getName() {
    return name;
  }

  public String getCategory() {
    return category;
  }

  public String getKeywords() {
    return keywords;
  }

  @Override
  public String toString() {
    return "DirectoryEntry [location="
        + location
        + ", title="
        + name
        + ", category="
        + category
        + ", keywords="
        + keywords
        + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((category == null) ? 0 : category.hashCode());
    result = prime * result + ((keywords == null) ? 0 : keywords.hashCode());
    result = prime * result + ((location == null) ? 0 : location.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    DirectoryEntry other = (DirectoryEntry) obj;
    if (category == null) {
      if (other.category != null) return false;
    } else if (!category.equals(other.category)) return false;
    if (keywords == null) {
      if (other.keywords != null) return false;
    } else if (!keywords.equals(other.keywords)) return false;
    if (location == null) {
      if (other.location != null) return false;
    } else if (!location.equals(other.location)) return false;
    if (name == null) {
      if (other.name != null) return false;
    } else if (!name.equals(other.name)) return false;
    return true;
  }
}
