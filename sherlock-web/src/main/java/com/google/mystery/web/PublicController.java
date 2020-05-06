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

package com.google.mystery.web;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.servlet.ServletException;
import org.apache.commons.text.WordUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import com.google.api.client.util.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.mystery.actions.SessionManager;
import com.google.mystery.actions.messages.MessageException;
import com.google.mystery.actions.messages.MessagesManager;
import com.google.mystery.actions.messages.MessagesTool;
import com.google.mystery.actions.model.SherlockResponse;
import com.google.mystery.assets.AssetsManager;
import com.google.mystery.assets.SearchManager;
import com.google.mystery.config.SherlockConfig;
import com.google.mystery.data.model.Case;
import com.google.mystery.data.model.Clue;
import com.google.mystery.data.model.Hint;
import com.google.mystery.data.model.Session;
import com.google.mystery.data.model.SessionLog;
import com.google.mystery.data.model.Story;
import com.google.mystery.web.model.CaseLogEntry;

@Controller
public class PublicController {
  @Inject private SessionManager sessionManager;
  @Inject private AssetsManager assetsManager;
  @Inject private MessagesManager messangesManager;
  @Inject private SherlockConfig config;

  private Logger logger = Logger.getLogger(this.getClass().getName());

  @GetMapping("/session/{sessionid}/about")
  protected String about(@PathVariable String sessionid, Model model)
      throws ServletException, IOException {
    Session session = sessionManager.getDBSession(sessionid);
    addTitle(model, session);

    return freemarker(model, "about");
  }

  protected void addTitle(Model model, Session session) {
    if (session != null && session.getCaseid() != null) {
      Case myCase = assetsManager.getCase(session.getCaseid());
      model.addAttribute("globalTitle", myCase.getName());
    }
  }

  @GetMapping("/google87ec9b3670740f8f.html")
  protected ResponseEntity<String> googleverification() throws ServletException, IOException {

    return new ResponseEntity<String>(
        "google-site-verification: google87ec9b3670740f8f.html", HttpStatus.OK);
  }

  @GetMapping("/session/{sessionid}/howtoplay")
  protected String howtoplay(@PathVariable String sessionid, Model model)
      throws ServletException, IOException {
    Session session = sessionManager.getDBSession(sessionid);
    addTitle(model, session);
    return freemarker(model, "how-to-play");
  }

  @GetMapping("/session/{sessionid}/checkrefresh")
  protected ResponseEntity<String> checkRefresh(
      @PathVariable String sessionid,
      @RequestParam(defaultValue = "time", required = true) String time) {
    try {
      long timeLong = Long.parseLong(time);
      if (sessionManager.checkSessionNewData(sessionid, timeLong)) {
        return new ResponseEntity<String>("{\"refresh\":true}", HttpStatus.OK);
      } else {
        return new ResponseEntity<String>("{\"refresh\":false}", HttpStatus.OK);
      }
    } catch (NumberFormatException e) {
      return new ResponseEntity<String>(
          "{\"error\":\"wrong time format\"}", HttpStatus.BAD_REQUEST);
    }
  }

  @GetMapping("/session/{sessionid}/map")
  protected String map(
      @PathVariable String sessionid, @RequestParam(defaultValue = "view") String mode, Model model)
      throws ServletException, IOException {
    List<Story> stories = ImmutableList.of();
    Session session = sessionManager.getDBSession(sessionid);
    addTitle(model, session);
    if (session != null) {
      String caseDataId = assetsManager.getCaseDataId(session);
      if (caseDataId != null) {
        if ("edit".equals(mode)) {
          stories = assetsManager.getAllStories(caseDataId);
        } else {
          stories = new ArrayList<>();
          stories.add(assetsManager.getStory(caseDataId, Story.CASE_INTRODUCTION_ID));
          for (String location : session.getLocationsBacklog()) {
            stories.add(assetsManager.getStory(caseDataId, location));
          }
        }
      }
    }

    JsonArray markers = new JsonArray();
    for (Story story : stories) {
      try {
        if (story.getLatlong() != null) {
          String[] split = story.getLatlong().split(",");
          if (split.length == 2) {
            JsonObject marker = new JsonObject();
            marker.addProperty("title", story.getTitle());
            marker.addProperty("lat", Double.parseDouble(split[0].trim()));
            marker.addProperty("long", Double.parseDouble(split[1].trim()));
            if (Story.LOCATION.equals(story.getType())) {
              marker.addProperty("address", WordUtils.capitalize(story.getId()));
            }
            marker.addProperty("id", story.getId());
            markers.add(marker);
          }
        }
      } catch (NumberFormatException e) {
        logger.log(Level.WARNING, "Error lat/long parsing", e);
      }
    }
    model.addAttribute("markers", markers);
    if ("edit".equals(mode)) {
      model.addAttribute("mark", true);
    }
    return freemarker(model, "map");
  }

  @GetMapping("/session/{sessionid}/clues")
  protected String clues(@PathVariable String sessionid, Model model)
      throws ServletException, IOException {
    Session session = sessionManager.getDBSession(sessionid);
    addTitle(model, session);
    List<Clue> clues = new ArrayList<>();
    if (session != null) {
      String caseDataId = assetsManager.getCaseDataId(session);
      if (caseDataId != null) {
        for (String clueid : session.getClues()) {
          Clue clue = assetsManager.getClue(caseDataId, clueid);
          if (clue != null) {
            clues.add(clue);
          } else {
            logger.warning("Session log reference unexisting clueid=" + clueid);
          }
        }
      }
    }
    Collections.reverse(clues);
    model.addAttribute("clues", clues);

    return freemarker(model, "clues");
  }

  private String freemarker(Model model, String name) {
    model.addAttribute("tool", new MessagesTool(assetsManager, config));
    return name;
  }

  @GetMapping("/session/{sessionid}/")
  protected String sessionLog(@PathVariable String sessionid, Model model)
      throws ServletException, IOException, MessageException {
    if (sessionid != null) {
      Session session = sessionManager.getDBSession(sessionid);
      if (session != null) {
        addTitle(model, session);
        String caseDataId = assetsManager.getCaseDataId(session);
        if (caseDataId != null) {
          List<SessionLog> sessionLogs = sessionManager.sessionLogs(sessionid);
          List<CaseLogEntry> logs = new ArrayList<>();
          for (ListIterator<SessionLog> i = sessionLogs.listIterator(sessionLogs.size());
              i.hasPrevious(); ) {
            SessionLog sessionLog = i.previous();
            if (SessionLog.THANK_YOU.equals(sessionLog.getStoryid())) {
              logs.add(
                  new CaseLogEntry(
                      sessionLog.getSessionid(),
                      "Finish",
                      "Thank you for playing",
                      messangesManager.message("thankYou"),
                      new URL(
                          "https://storage.googleapis.com/mystery-engine-voices/images/sherlock.png"),
                      null,
                      ImmutableList.of()));
            }
            Story story = assetsManager.getStory(caseDataId, sessionLog.getStoryid());
            if (story == null) {
              logger.warning("Session log reference unexisting storyid=" + sessionLog.getStoryid());
              continue;
            }
            Hint hint = null;
            if (sessionLog.getHintid() != null) {
              hint = assetsManager.getHint(caseDataId, sessionLog.getHintid());
            }
            List<Clue> clues = new ArrayList<>();
            for (String clueid : sessionLog.getClueids()) {
              clues.add(assetsManager.getClue(caseDataId, clueid));
            }
            String address = assetsManager.checkLocation(story.getId());
            String title = null;
            if (address != null) {
              title = String.format("address: %s", WordUtils.capitalize(story.getId()));
              // hardcode!!!
            } else if ("caseintroduction".equals(story.getId().toLowerCase())) {
              title = "case introduction";
            } else if ("finalsolution".equals(story.getId().toLowerCase())) {
              title = "final solution";
            }

            logs.add(
                new CaseLogEntry(
                    SearchManager.normalize(story.getId()),
                    story.getTitle(),
                    title,
                    SherlockResponse.textToHtml(story.getText()),
                    story.getImageUrl(),
                    hint == null || Strings.isNullOrEmpty(hint.getHint()) ? null : hint.getHint(),
                    clues));
          }
          model.addAttribute("logs", logs);
          return freemarker(model, "case-files");
        }
      }
    }
    return freemarker(model, "case-files");
  }

  @ExceptionHandler({Throwable.class})
  public ResponseEntity<String> errorHandling(Throwable e) {
    logger.log(Level.SEVERE, "unexpected error", e);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(new MediaType("application", "json", Charsets.UTF_8));
    JsonObject content = new JsonObject();
    content.addProperty("message", e.getMessage());
    return new ResponseEntity<>(content.toString(), headers, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
