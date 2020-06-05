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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.csv.CSVFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.collect.ImmutableList;
import com.google.mystery.assets.AdminManager;
import com.google.mystery.assets.AssetsImportManager;
import com.google.mystery.assets.AssetsManager;
import com.google.mystery.assets.DialogflowExportEntitiesManager;
import com.google.mystery.config.SherlockConfig;
import com.google.mystery.data.model.Case;
import com.google.mystery.data.model.DirectoryEntry;
import com.google.mystery.data.model.Story;
import com.sherlockmysteries.pdata.SMDataManager;

@Controller
public class AdminController {
  @Inject private DialogflowExportEntitiesManager apiaiExportEntitiesManager;
  @Inject private AssetsImportManager assetsImportManager;
  @Inject private AssetsManager assetsManager;
  @Inject private AdminManager adminManager;
  @Inject private SherlockConfig config;
  @Inject private SMDataManager smDataManager;

  static List<String> CONFIG_KEYS =
      ImmutableList.of(
          SherlockConfig.BASE_URL,
          SherlockConfig.ASSISTANT_DIRECTORY_URL,
          SherlockConfig.BUCKET_NAME,
          SherlockConfig.DOCS_SECRET,
          SherlockConfig.FOOTER_SCRIPTS,
          SherlockConfig.DIALOGFLOW_SECRET);

  @GetMapping("/admin/export-smdata")
  public String exportSMData(
      @RequestParam(value = "caseId", required = true) String caseId, Model model) {
    PrintWriter writer = assetsImportManager.createWriter();
    try (FileOutputStream tempFile = new FileOutputStream("/tmp/case.zip")) {
      smDataManager.generateSMData(caseId, writer, tempFile);
    } catch (IOException e) {
      writer.println("Error exporting sm data:" + e.getMessage());
    }
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

  @GetMapping("/admin/dialogflow")
  public String exportEntitiesGet() {
    return "admin/dialogflow";
  }

  @PostMapping("/admin/dialogflow")
  public String exportEntities(
      HttpServletRequest request,
      Model model,
      @RequestParam(value = "projectName", required = true) String projectName)
      throws IOException {

    if (projectName != null) {
      StringWriter stringWriter = new StringWriter();
      PrintWriter printWriter = new PrintWriter(stringWriter);
      apiaiExportEntitiesManager.update(projectName, printWriter);
      model.addAttribute("log", stringWriter.toString());
    }
    model.addAttribute("projectName", projectName);
    return "admin/dialogflow";
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
