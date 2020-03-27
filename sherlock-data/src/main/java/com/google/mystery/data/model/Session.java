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

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Text;
import com.google.mystery.data.DataManager;
import com.google.mystery.data.DataUtil;

/**
 * Represents user in-game session
 *
 * @author ilyaplatonov
 */
public class Session implements Serializable {
  private static final long serialVersionUID = 622777008112546117L;

  private static final String ANSWERS_RESULTS_PROPERTY = "answersResults";
  private static final String ANSWERS_PROPERTY = "answers";
  private static final String USED_HINTS_PROPERTY = "usedHints";
  private static final String CLUES_PROPERTY = "clues";
  private static final String LOCATIONS_BACKLOG_PROPERTY = "locationsBacklog";
  private static final String STATE_PROPERTY = "state";
  private static final String FOLLOWUP_TEXT_PROPERTY = "followupText";
  private static final String ACTIVE_SUGGESTIONS_PROPERTY = "activeSuggestions";
  private final String sessionid;
  private final State state;
  private final List<String> locationsBacklog;
  private final List<String> clues;
  private final List<String> usedHints;
  private final List<String> answers;
  private final List<Boolean> answersResults;
  /** text that need to be given to user on next "welcome" request. */
  private final String followupText;
  /** suggestions that should be show to the player. */
  private final List<SessionActiveSuggestion> activeSuggestions;
  /** id of active played case. */
  private final String caseid;

  public static SessionBuilder builder(String sessionid) {
    return new SessionBuilder(sessionid);
  }

  public static SessionBuilder builder(Session session) {
    return new SessionBuilder(session);
  }

  protected Session(
      String sessionid,
      State state,
      List<String> locationsBacklog,
      List<String> clues,
      List<String> usedHints,
      List<String> answers,
      List<Boolean> answersResults,
      String followupText,
      List<SessionActiveSuggestion> activeSuggestions,
      String caseid) {
    this.sessionid = sessionid;
    this.state = state;
    this.locationsBacklog = locationsBacklog;
    this.clues = clues;
    this.usedHints = usedHints;
    this.answers = answers;
    this.answersResults = answersResults;
    this.followupText = followupText;
    this.activeSuggestions = activeSuggestions;
    this.caseid = caseid;
  }

  public static Session fromEntity(Entity entity) {
    String followupText = null;
    if (entity.hasProperty(FOLLOWUP_TEXT_PROPERTY)) {
      followupText = ((Text) entity.getProperty(FOLLOWUP_TEXT_PROPERTY)).getValue();
    }
    List<SessionActiveSuggestion> activeSuggestions =
        DataUtil.getList(entity, ACTIVE_SUGGESTIONS_PROPERTY, SessionActiveSuggestion.class);
    return Session.builder(entity.getKey().getName())
        .state(State.valueOf(entity.getProperty(STATE_PROPERTY).toString()))
        .locationsBacklog(DataUtil.getStringList(entity, LOCATIONS_BACKLOG_PROPERTY))
        .clues(DataUtil.getStringList(entity, CLUES_PROPERTY))
        .usedHints(DataUtil.getStringList(entity, USED_HINTS_PROPERTY))
        .answers(DataUtil.getStringList(entity, ANSWERS_PROPERTY))
        .answersResults(
            DataUtil.getStringList(entity, ANSWERS_RESULTS_PROPERTY)
                .stream()
                .map(Boolean::valueOf)
                .collect(Collectors.toList()))
        .followupText(followupText)
        .activeSuggestions(activeSuggestions)
        .caseid((String) entity.getProperty("caseid"))
        .build();
  }

  public static Entity toEntity(Session session) {
    Entity storyEntity = new Entity(DataManager.SESSION_KIND, session.getSessionid());
    storyEntity.setUnindexedProperty(STATE_PROPERTY, session.getState().toString());
    DataUtil.setStringListProperty(
        storyEntity, LOCATIONS_BACKLOG_PROPERTY, session.getLocationsBacklog());
    DataUtil.setStringListProperty(storyEntity, CLUES_PROPERTY, session.getClues());
    DataUtil.setStringListProperty(storyEntity, USED_HINTS_PROPERTY, session.getUsedHints());
    DataUtil.setStringListProperty(storyEntity, ANSWERS_PROPERTY, session.getAnswers());
    DataUtil.setStringListProperty(
        storyEntity,
        ANSWERS_RESULTS_PROPERTY,
        session.getAnswersResults().stream().map(b -> b.toString()).collect(Collectors.toList()));
    DataUtil.setListProperty(
        storyEntity, ACTIVE_SUGGESTIONS_PROPERTY, session.getActiveSuggestions());
    if (session.getFollowupText() != null) {
      storyEntity.setUnindexedProperty(FOLLOWUP_TEXT_PROPERTY, new Text(session.getFollowupText()));
    }

    if (session.getCaseid() != null) {
      storyEntity.setUnindexedProperty("caseid", session.getCaseid());
    }
    return storyEntity;
  }

  public String getSessionid() {
    return sessionid;
  }

  public State getState() {
    return state;
  }

  public List<String> getLocationsBacklog() {
    return locationsBacklog;
  }

  public List<String> getClues() {
    return clues;
  }

  public List<String> getUsedHints() {
    return usedHints;
  }

  public List<String> getAnswers() {
    return answers;
  }

  public List<Boolean> getAnswersResults() {
    return answersResults;
  }

  public String getFollowupText() {
    return followupText;
  }

  public List<SessionActiveSuggestion> getActiveSuggestions() {
    return activeSuggestions;
  }

  public String getCaseid() {
    return caseid;
  }

  public enum State {
    /** fresh new user. */
    NEW,
    /** main state when game is happening. */
    CASE_STARTED,
    /** end-game players presented with questions one by one. */
    QUESTIONS,
    /** end-game players presented with answers and have to confirm if their answers are correct. */
    ANSWERS,
    /** game is finished. */
    FINISH
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((activeSuggestions == null) ? 0 : activeSuggestions.hashCode());
    result = prime * result + ((answers == null) ? 0 : answers.hashCode());
    result = prime * result + ((answersResults == null) ? 0 : answersResults.hashCode());
    result = prime * result + ((caseid == null) ? 0 : caseid.hashCode());
    result = prime * result + ((clues == null) ? 0 : clues.hashCode());
    result = prime * result + ((followupText == null) ? 0 : followupText.hashCode());
    result = prime * result + ((locationsBacklog == null) ? 0 : locationsBacklog.hashCode());
    result = prime * result + ((sessionid == null) ? 0 : sessionid.hashCode());
    result = prime * result + ((state == null) ? 0 : state.hashCode());
    result = prime * result + ((usedHints == null) ? 0 : usedHints.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    Session other = (Session) obj;
    if (activeSuggestions == null) {
      if (other.activeSuggestions != null) return false;
    } else if (!activeSuggestions.equals(other.activeSuggestions)) return false;
    if (answers == null) {
      if (other.answers != null) return false;
    } else if (!answers.equals(other.answers)) return false;
    if (answersResults == null) {
      if (other.answersResults != null) return false;
    } else if (!answersResults.equals(other.answersResults)) return false;
    if (caseid == null) {
      if (other.caseid != null) return false;
    } else if (!caseid.equals(other.caseid)) return false;
    if (clues == null) {
      if (other.clues != null) return false;
    } else if (!clues.equals(other.clues)) return false;
    if (followupText == null) {
      if (other.followupText != null) return false;
    } else if (!followupText.equals(other.followupText)) return false;
    if (locationsBacklog == null) {
      if (other.locationsBacklog != null) return false;
    } else if (!locationsBacklog.equals(other.locationsBacklog)) return false;
    if (sessionid == null) {
      if (other.sessionid != null) return false;
    } else if (!sessionid.equals(other.sessionid)) return false;
    if (state != other.state) return false;
    if (usedHints == null) {
      if (other.usedHints != null) return false;
    } else if (!usedHints.equals(other.usedHints)) return false;
    return true;
  }
}
