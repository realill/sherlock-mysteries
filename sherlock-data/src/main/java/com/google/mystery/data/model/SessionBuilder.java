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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.mystery.data.model.Session.State;

/**
 * Builder for {@link Session} class
 *
 * @author ilyaplatonov
 */
public class SessionBuilder {
  private String sessionid;
  private State state;
  private List<String> locationsBacklog;
  private List<String> clues;
  private List<String> usedHints;
  private List<String> answers;
  private List<Boolean> answersResults;
  private String followupText;
  private List<SessionActiveSuggestion> activeSuggestions;
  private String caseid;

  /**
   * Creates new fresh session with default parameters
   *
   * @param sessionId
   */
  public SessionBuilder(String sessionId) {
    this.sessionid = sessionId;
    this.state = Session.State.NEW;
    this.locationsBacklog = Lists.newArrayList();
    this.clues = Lists.newArrayList();
    this.usedHints = Lists.newArrayList();
    this.answers = Lists.newArrayList();
    this.answersResults = Lists.newArrayList();
    this.followupText = null;
    this.activeSuggestions = ImmutableList.of();
    this.caseid = null;
  }

  public SessionBuilder(Session session) {
    this.sessionid = session.getSessionid();
    this.state = session.getState();
    this.locationsBacklog = Lists.newArrayList(session.getLocationsBacklog());
    this.clues = Lists.newArrayList(session.getClues());
    this.usedHints = Lists.newArrayList(session.getUsedHints());
    this.answers = Lists.newArrayList(session.getAnswers());
    this.answersResults = Lists.newArrayList(session.getAnswersResults());
    this.followupText = session.getFollowupText();
    this.activeSuggestions = session.getActiveSuggestions();
    this.caseid = session.getCaseid();
  }

  public SessionBuilder activeSuggestions(List<SessionActiveSuggestion> suggestions) {
    this.activeSuggestions = suggestions;
    return this;
  }

  public SessionBuilder state(State state) {
    this.state = state;
    return this;
  }

  public SessionBuilder locationsBacklog(List<String> locationsBacklog) {
    this.locationsBacklog = locationsBacklog;
    return this;
  }

  public SessionBuilder addLocation(String location) {
    locationsBacklog.add(location);
    return this;
  }

  public SessionBuilder clues(List<String> clues) {
    this.clues = clues;
    return this;
  }

  public SessionBuilder usedHints(List<String> usedHints) {
    this.usedHints = usedHints;
    return this;
  }

  public SessionBuilder answers(List<String> answers) {
    this.answers = answers;
    return this;
  }

  public SessionBuilder answersResults(List<Boolean> answersResults) {
    this.answersResults = answersResults;
    return this;
  }

  public SessionBuilder followupText(String followupText) {
    this.followupText = followupText;
    return this;
  }

  public Session build() {
    return new Session(
        sessionid,
        state,
        ImmutableList.copyOf(locationsBacklog),
        ImmutableList.copyOf(clues),
        ImmutableList.copyOf(usedHints),
        ImmutableList.copyOf(answers),
        ImmutableList.copyOf(answersResults),
        followupText,
        activeSuggestions,
        caseid);
  }

  public SessionBuilder addHint(String id) {
    usedHints.add(id);
    return this;
  }

  public SessionBuilder addClue(String clueid) {
    clues.add(clueid);
    return this;
  }

  public SessionBuilder addAnswer(String answer) {
    answers.add(answer);
    return this;
  }

  public SessionBuilder addAnswerResult(boolean correct) {
    answersResults.add(correct);
    return this;
  }

  public SessionBuilder caseid(String caseid) {
    this.caseid = caseid;
    return this;
  }
}
