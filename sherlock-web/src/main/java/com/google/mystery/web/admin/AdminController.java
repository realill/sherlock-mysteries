// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.mystery.web.admin;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.text.WordUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.mystery.assets.AdminManager;
import com.google.mystery.assets.AssetsManager;
import com.google.mystery.assets.LongOperationsManager;
import com.google.mystery.config.SherlockConfig;
import com.google.mystery.data.model.Case;
import com.google.mystery.data.model.DirectoryEntry;
import com.google.mystery.data.model.Story;

@Controller
public class AdminController {
  @Inject private LongOperationsManager assetsImportManager;
  @Inject private AssetsManager assetsManager;
  @Inject private AdminManager adminManager;
  @Inject private SherlockConfig config;

  static List<String> CONFIG_KEYS =
      ImmutableList.of(
          SherlockConfig.BASE_URL,
          SherlockConfig.ASSISTANT_DIRECTORY_URL,
          SherlockConfig.BUCKET_NAME,
          SherlockConfig.DOCS_SECRET,
          SherlockConfig.FOOTER_SCRIPTS,
          SherlockConfig.DIALOGFLOW_SECRET);

  private Logger logger = Logger.getLogger(this.getClass().getName());

  @GetMapping("/map")
  protected String map(
      @RequestParam(required = true) String caseDataId,
      @RequestParam(defaultValue = "BIRDEYE") String mapType,
      Model model)
      throws ServletException, IOException {
    List<Story> stories = assetsManager.getAllStories(caseDataId);

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
    if (mapType.equals("BIRDEYE")) {
      model.addAttribute("mapUrl", "/static/map.jpg");
      model.addAttribute("boundWidth", "1000");
      model.addAttribute("boundHeight", "715");
    } else {
      model.addAttribute("mapUrl", "/static/pictorial-map.jpg");
      model.addAttribute("boundWidth", "1024");
      model.addAttribute("boundHeight", "515");
    }
    return "admin/map";
  }

  @GetMapping("/admin/export-smdata")
  public String exportSMData(
      @RequestParam(value = "caseId", required = true) String caseId, Model model) {
    assetsImportManager.exportSMData(caseId);
    return "redirect:/admin/import-log";
  }

  @PostMapping("/admin/config")
  public String saveConfig(HttpServletRequest request, Model model) {
    Map<String, String> configMap = new HashMap<>();
    for (String k : CONFIG_KEYS) {
      if (request.getParameter(k) != null) {
        configMap.put(k, request.getParameter(k));
      }
    }
    try {
      // validating
      new SherlockConfig(configMap);
    } catch (MalformedURLException e) {
      model.addAttribute(
          "errors", ImmutableList.of("Wrong url, config is not saved " + e.getMessage()));
      model.addAttribute("config", config);
      return "admin/config";
    }
    adminManager.saveConfig(configMap);
    return "redirect:/admin/config";
  }

  @GetMapping("/admin/config")
  public String config(Model model) {
    model.addAttribute("config", config);
    return "admin/config";
  }

  @GetMapping("/admin/generate-audio")
  public String generateAudio(
      @RequestParam(value = "caseId", required = true) String caseId,
      @RequestParam(value = "storyId", required = false) String storyId,
      Model model) {
    Case c = assetsManager.getCase(caseId);
    if (c == null || c.getCaseDataId() == null) {
      model.addAttribute("content", "Invalid case, caseId=" + caseId);
      return "admin/show-data";
    }

    // filtering out stories that we do not need
    List<Story> stories;
    if (storyId == null) {
      stories =
          assetsManager
              .getAllStories(c.getCaseDataId())
              .stream()
              .filter(
                  s ->
                      s.getType().equals(Story.LOCATION)
                          || s.getId().equalsIgnoreCase("caseIntroduction")
                          || s.getId().equalsIgnoreCase("finalSolution"))
              .collect(Collectors.toList());
    } else {
      Story story = assetsManager.getStory(c.getCaseDataId(), storyId);
      if (story == null) {
        model.addAttribute("content", "Can not find storyid=" + storyId);
        return "admin/show-data";
      }
      stories = ImmutableList.of(story);
    }
    assetsImportManager.generateAudio(caseId, stories);
    return "redirect:/admin/import-log";
  }

  @GetMapping("/admin/generate-directory")
  public String generateDirectory(Model model) {
    StringBuilder outputCsv = new StringBuilder();
    for (DirectoryEntry entry : adminManager.genereteDirectory()) {
      outputCsv.append(CSVFormat.DEFAULT.format(entry.getName(), entry.getLocation()));
      outputCsv.append('\n');
    }
    model.addAttribute("content", outputCsv.toString());
    return "admin/show-data";
  }
}
