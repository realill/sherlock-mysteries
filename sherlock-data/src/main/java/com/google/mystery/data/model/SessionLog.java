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

/** */
package com.google.mystery.data.model;

import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;
import com.google.appengine.api.datastore.Entity;
import com.google.mystery.data.DataManager;
import com.google.mystery.data.DataUtil;

/**
 * Session log entry to show to player
 *
 * @author ilyaplatonov
 */
public class SessionLog {
  public static String THANK_YOU = "thankyou";
  private final String sessionid;
  private final String storyid;
  private final List<String> clueids;
  @Nullable private final String hintid;

  public SessionLog(String sessionid, String storyid, List<String> clueids, String hintid) {
    this.sessionid = sessionid;
    this.storyid = storyid;
    this.clueids = clueids;
    this.hintid = hintid;
  }

  public static SessionLog fromEntity(Entity entity) {
    return new SessionLog(
        (String) entity.getProperty("sessionid"),
        (String) entity.getProperty("storyid"),
        DataUtil.getStringList(entity, "clueids"),
        DataUtil.getOptionalString(entity, "hintid"));
  }

  public static Entity toEntity(SessionLog sessionLog) {
    Entity storyLogEntity = new Entity(DataManager.SESSION_LOG_KIND);
    storyLogEntity.setIndexedProperty("sessionid", sessionLog.getSessionid());
    storyLogEntity.setIndexedProperty("created", new Date().getTime());
    storyLogEntity.setUnindexedProperty("storyid", sessionLog.getStoryid());
    DataUtil.setStringListProperty(storyLogEntity, "clueids", sessionLog.getClueids());
    storyLogEntity.setUnindexedProperty("hintid", sessionLog.getHintid());
    return storyLogEntity;
  }

  public String getSessionid() {
    return sessionid;
  }

  public String getStoryid() {
    return storyid;
  }

  public List<String> getClueids() {
    return clueids;
  }

  public String getHintid() {
    return hintid;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((clueids == null) ? 0 : clueids.hashCode());
    result = prime * result + ((hintid == null) ? 0 : hintid.hashCode());
    result = prime * result + ((sessionid == null) ? 0 : sessionid.hashCode());
    result = prime * result + ((storyid == null) ? 0 : storyid.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    SessionLog other = (SessionLog) obj;
    if (clueids == null) {
      if (other.clueids != null) return false;
    } else if (!clueids.equals(other.clueids)) return false;
    if (hintid == null) {
      if (other.hintid != null) return false;
    } else if (!hintid.equals(other.hintid)) return false;
    if (sessionid == null) {
      if (other.sessionid != null) return false;
    } else if (!sessionid.equals(other.sessionid)) return false;
    if (storyid == null) {
      if (other.storyid != null) return false;
    } else if (!storyid.equals(other.storyid)) return false;
    return true;
  }

  @Override
  public String toString() {
    return "SessionLog [sessionid="
        + sessionid
        + ", storyid="
        + storyid
        + ", clueids="
        + clueids
        + ", hintid="
        + hintid
        + "]";
  }
}
