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

package com.google.mystery.actions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.mystery.actions.messages.MessageException;
import com.google.mystery.actions.model.SherlockResponseBuilder;
import com.google.mystery.assets.AssetsManager;
import com.google.mystery.config.SherlockConfig;
import com.google.mystery.data.DataManager;
import com.google.mystery.data.model.Clue;
import com.google.mystery.data.model.Hint;
import com.google.mystery.data.model.Session;
import com.google.mystery.data.model.Session.State;
import com.google.mystery.data.model.SessionActiveSuggestion;
import com.google.mystery.data.model.SessionBuilder;
import com.google.mystery.data.model.SessionLog;
import com.google.mystery.data.model.Story;

/**
 * Handles player session.
 *
 * <p>
 * Current request session is initialized {@link #initSession(String)} in the beginning of the
 * request. Then all changes are happening in memory and committed into DB by
 * {@link #commitSession()};
 *
 * <p>
 * {@link #getSession()} gives you current request session.
 *
 * <p>
 * Not thread safe
 *
 * @author ilyaplatonov
 */
public class SessionManager {
  @Inject
  private DataManager dataManager;

  @Inject
  private AssetsManager assetsManager;
  @Inject
  private SessionHolder sessionHolder;
  @Inject
  private SherlockConfig config;

  public void initSession(String sessionid) {
    sessionHolder.initSession(getSessionOrCreate(sessionid));
  }

  public void commitSession() {
    if (sessionHolder.isChanged()) {
      dataManager.putSession(sessionHolder.getSession());
    }
  }

  /** @return active player session, if it does not exist, then new session will be created */
  private Session getSessionOrCreate(String sessionid) {
    Session session = dataManager.getSession(sessionid);
    if (session == null) {
      session = newSession(sessionid);
    }
    return session;
  }

  /**
   * Session is immutable so you should call this method every time you need new changes.
   *
   * @return session or <code>null</code> if does not exist
   */
  public Session getSession() {
    return sessionHolder.getSession();
  }

  /** Gets session directly from database. It should be used for read only operations */
  public Session getDBSession(String sessionid) {
    return dataManager.getSession(sessionid);
  }

  /** Starts new case session */
  public void startCase(String sessionid, String caseId) {
    sessionHolder
        .setSession(Session.builder(sessionid).state(State.CASE_STARTED).caseid(caseId).build());
    dataManager.clearSessionLog(sessionid);
  }

  /** Changes state of session */
  public void sessionState(State newState) {
    sessionHolder.setSession(Session.builder(getSession()).state(newState).build());
  }

  public void resetSession(String sessionid) {
    sessionHolder.setSession(newSession(sessionid));
  }

  private Session newSession(String sessionid) {
    Session session = Session.builder(sessionid).state(State.NEW).build();
    dataManager.putSession(session);
    dataManager.clearSessionLog(sessionid);
    return session;
  }

  /** @return session logs for session */
  public List<SessionLog> sessionLogs(String sessionid) {
    return dataManager.getSessionLogs(sessionid);
  }

  public void addActiveSuggestions(List<String> suggestions) {
    Session session = getSession();
    List<SessionActiveSuggestion> activeSuggestions =
        Lists.newArrayList(session.getActiveSuggestions());
    for (String s : suggestions) {
      activeSuggestions.add(new SessionActiveSuggestion(s, 10));
    }
    sessionHolder.setSession(Session.builder(session).activeSuggestions(activeSuggestions).build());
  }

  /** reducing relevancy of all active suggestions */
  public void ageActiveSuggestions() {
    Session session = getSession();
    List<SessionActiveSuggestion> activeSuggestions =
        new ArrayList<>(session.getActiveSuggestions().size());
    for (SessionActiveSuggestion sug : session.getActiveSuggestions()) {
      activeSuggestions.add(new SessionActiveSuggestion(sug.getSuggestion(),
          sug.getRelevancy() > 0 ? sug.getRelevancy() - 1 : 0));
    }
    sessionHolder.setSession(Session.builder(session).activeSuggestions(activeSuggestions).build());
  }

  public void addLocation(String location) {
    Session session = getSession();
    sessionHolder.setSession(Session.builder(session).addLocation(location).build());
  }

  public void addHint(Hint hint) {
    Session session = getSession();
    sessionHolder.setSession(Session.builder(session).addHint(hint.getId()).build());
  }

  public void addClue(Collection<Clue> clues) throws IOException, MessageException {
    Session session = getSession();
    SessionBuilder sessionBuilder = Session.builder(session);
    for (Clue clue : clues) {
      sessionBuilder.addClue(clue.getId());
    }
    sessionHolder.setSession(sessionBuilder.build());
  }

  public List<Clue> getSessionClues(String caseDataId) {
    Session session = getSession();
    List<Clue> clues = new ArrayList<>();
    for (String clueid : session.getClues()) {
      Clue clue = assetsManager.getClue(caseDataId, clueid);
      if (clue != null) {
        clues.add(clue);
      }
    }
    // from news to last one.
    Collections.reverse(clues);
    return clues;
  }

  public void addAnswer(String answer) {
    Session session = getSession();
    sessionHolder.setSession(Session.builder(session).addAnswer(answer).build());
  }

  public void addAnswerResult(boolean correct) {
    Session session = getSession();
    sessionHolder.setSession(Session.builder(session).addAnswerResult(correct).build());
  }

  public void setFollowupText(String followupText) {
    Session session = getSession();
    sessionHolder.setSession(Session.builder(session).followupText(followupText).build());
  }

  public void pushSessionLog(Story story, SherlockResponseBuilder response) {
    SessionLog sessionLog = new SessionLog(getSession().getSessionid(), story.getId(),
        // list of clue ids.
        response.getClues().stream().map(c -> c.getId()).collect(Collectors.toList()),
        response.getHint() != null ? response.getHint().getId() : null);
    dataManager.pushSessionLog(sessionLog);
  }

  /** Final message after game is finished. */
  public void pushFinalSessionLog() {
    SessionLog sessionLog =
        new SessionLog(getSession().getSessionid(), SessionLog.THANK_YOU, ImmutableList.of(), null);
    dataManager.pushSessionLog(sessionLog);
  }

  /** Checking if session got data that is new then specific time */
  public boolean checkSessionNewData(String sessionid, long time) {
    return !dataManager.getSessionLogs(sessionid, time).isEmpty();
  }
}
