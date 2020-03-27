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

public class SherlockContext {
  final String name;
  final Integer lifespan;

  public SherlockContext(String name) {
    this(name, null);
  }

  public SherlockContext(String name, Integer lifespan) {
    this.name = name;
    this.lifespan = lifespan;
  }


  public String getName() {
    return name;
  }

  public Integer getLifespan() {
    return lifespan;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((lifespan == null) ? 0 : lifespan.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    SherlockContext other = (SherlockContext) obj;
    if (lifespan == null) {
      if (other.lifespan != null)
        return false;
    } else if (!lifespan.equals(other.lifespan))
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "SherlockContext [name=" + name + ", lifespan=" + lifespan + "]";
  }
}
