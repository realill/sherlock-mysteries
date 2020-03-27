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

package com.google.mystery.web.admin;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.AuthorizationCodeResponseUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.mystery.assets.AdminManager;
import com.google.mystery.assets.AssetsImportManager;
import com.google.mystery.assets.AssetsManager;
import com.google.mystery.assets.GSuiteDataSource;
import com.google.mystery.data.model.Case;
import com.google.mystery.data.model.CaseData;
import com.google.mystery.data.model.Clue;
import com.google.mystery.data.model.Story;

@Controller
public class CasesController {
  private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("-YYYYMMdd-hhmm");
  private Logger logger = Logger.getLogger(this.getClass().getName());
  @Inject
  private AdminManager adminManager;
  @Inject
  private AssetsImportManager assetsImportManager;
  @Inject
  private AssetsManager assetsManager;
  @Inject
  private GSuiteDataSource gsuiteClient;

  @GetMapping("/admin/show-story-text")
  public String showStoryText(Model model, @RequestParam(value = "caseDataId") String caseDataId,
      @RequestParam(value = "storyId") String storyId) {

    Story story = assetsManager.getStory(caseDataId, storyId);
    model.addAttribute("content", story.getText());

    return "admin/show-data";
  }

  @GetMapping("/admin/delete-case-data")
  public String deleteCaseData(Model model,
      @RequestParam(value = "caseDataId", required = true) String caseDataId) {
    try {
      adminManager.deleteCaseData(caseDataId);
    } catch (IOException e) {
      model.addAttribute("errors", ImmutableList.of(e.getMessage()));
      return showCaseData(model, caseDataId);
    }
    return "redirect:/admin/case-data";
  }

  @GetMapping("/admin/show-case-data")
  public String showCaseData(Model model,
      @RequestParam(value = "caseDataId", required = true) String caseDataId) {
    List<Story> stories = assetsManager.getAllStories(caseDataId).stream()
        .sorted((s1, s2) -> s1.getId().compareTo(s2.getId())).collect(Collectors.toList());
    List<Clue> clues = assetsManager.getAllClues(caseDataId);

    model.addAttribute("caseDataId", caseDataId);
    model.addAttribute("stories", stories);
    model.addAttribute("clues", clues);
    return "admin/show-case-data";
  }

  @GetMapping("/admin/import-log")
  public String uploadCaseData(Model model) {
    model.addAttribute("log", assetsImportManager.getLatestLog());
    return "admin/import-log";
  }

  @GetMapping("/admin/upload-case-data")
  public String uploadCaseData(HttpServletRequest request, Model model,
      @RequestParam(value = "urlOrId", required = false) String spreadsheetUrlOrId,
      @RequestParam(value = "generalUrlOrId", required = false) String generalUrlOrId,
      @RequestParam(value = "caseDataId", required = false) String caseDataId) {

    if (spreadsheetUrlOrId == null && generalUrlOrId == null) {
      return "admin/upload-case-data";
    }

    List<String> errors = new ArrayList<>();
    try {
      String userid = UserServiceFactory.getUserService().getCurrentUser().getUserId();
      Credential credential = gsuiteClient.getAuthFlow().loadCredential(userid);
      if (credential == null || credential.getAccessToken() == null
          || credential.getExpiresInSeconds() < 10) {
        AuthorizationCodeRequestUrl authorizationUrl =
            gsuiteClient.getAuthFlow().newAuthorizationUrl();
        String redirectUrl = oauth2Redirect(request);
        authorizationUrl.setRedirectUri(redirectUrl.toString());
        authorizationUrl.setState(request.getQueryString());
        return "redirect:" + authorizationUrl.build();
      }

      if (generalUrlOrId != null) {
        return handleGeneralDataImport(generalUrlOrId, credential);
      }

      if (spreadsheetUrlOrId != null) {
        return handleCaseDataImport(model, spreadsheetUrlOrId, caseDataId, credential);
      }

    } catch (IOException e) {
      logger.log(Level.WARNING, "Error getting metadata", e);
      model.addAttribute("urlOrId", spreadsheetUrlOrId);
      model.addAttribute("caseDataId", caseDataId);
      errors.add(e.getMessage());
    }
    model.addAttribute("errors", errors);
    return "admin/upload-case-data";
  }

  public String handleCaseDataImport(Model model, String urlOrId, String caseDataId,
      Credential credential) throws IOException {
    List<String> errors = new ArrayList<>();
    if (caseDataId == null) {
      Map<String, String> metadata =
          assetsImportManager.importCaseDataMetadata(credential, urlOrId);
      if (metadata.containsKey("caseId")) {
        model.addAttribute("urlOrId", urlOrId);
        String caseId = metadata.get("caseId");
        model.addAttribute("caseDataId", caseId + SIMPLE_DATE_FORMAT.format(new Date()));
        model.addAttribute("message", "Press Import to create new case data.");
      } else {
        errors.add("Metadata does not have caseId");
      }
      return "admin/upload-case-data";
    }

    adminManager.createCaseData(caseDataId);
    assetsImportManager.importCaseAssets(credential, urlOrId, caseDataId);
    return "redirect:/admin/import-log";

  }

  public String handleGeneralDataImport(String generalUrlOrId, Credential credential)
      throws IOException {
    assetsImportManager.importGeneralAssets(credential, generalUrlOrId);
    return "redirect:/admin/import-log";
  }

  private String oauth2Redirect(HttpServletRequest request) {
    GenericUrl redirectUrl = new GenericUrl(request.getRequestURL().toString());
    redirectUrl.setRawPath("/admin/oauth2callback");
    return redirectUrl.toString();
  }

  @GetMapping("/admin/oauth2callback")
  public void importOauthcallback(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    StringBuffer buf = request.getRequestURL();
    if (request.getQueryString() != null) {
      buf.append('?').append(request.getQueryString());
    }
    AuthorizationCodeResponseUrl responseUrl = new AuthorizationCodeResponseUrl(buf.toString());
    String code = responseUrl.getCode();
    if (responseUrl.getError() != null) {
      throw new IllegalArgumentException("auth error " + responseUrl.getError());
    } else if (code == null) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.getWriter().print("Missing authorization code");
    } else {
      String userid = UserServiceFactory.getUserService().getCurrentUser().getUserId();
      TokenResponse tresponse = gsuiteClient.getAuthFlow().newTokenRequest(code)
          .setRedirectUri(oauth2Redirect(request)).execute();
      gsuiteClient.getAuthFlow().createAndStoreCredential(tresponse, userid);
      response.sendRedirect("/admin/upload-case-data?" + responseUrl.getState());
    }
  }

  @GetMapping("/admin/case-data")
  public String caseDataIndex(Model model) {
    try {
      List<CaseData> caseData = adminManager.getAllCaseData();
      model.addAttribute("caseData", caseData);
    } catch (Throwable e) {
      logger.log(Level.SEVERE, "error getting cases", e);
    }
    return "admin/case-data-index";
  }

  @GetMapping("/admin/")
  public String root(Model model) {

    try {
      List<Case> cases = adminManager.getAllCases();
      model.addAttribute("cases", cases);
    } catch (Throwable e) {
      logger.log(Level.SEVERE, "error getting cases", e);
    }
    return "admin/index";
  }

  @GetMapping("/admin/update-case")
  public String newCaseShowForm(Model model,
      @RequestParam(value = "caseId", required = false) String caseId) {
    if (caseId != null) {
      model.addAttribute("case", adminManager.getCase(caseId));
    }
    return "admin/new-case";
  }

  @PostMapping("/admin/update-case")
  public String create(HttpServletRequest request, Model model) {
    List<String> errors = new ArrayList<>();
    Case newCase = caseFromRequest(request, errors);
    if (errors.isEmpty()) {
      try {
        adminManager.updateCase(newCase);
      } catch (IOException e) {
        errors.add(e.getMessage());
      }
      model.addAttribute("success", "Update Successful");
    }
    model.addAttribute("errors", errors);
    model.addAttribute("case", newCase);
    return "admin/new-case";
  }

  @GetMapping("/admin/delete-case")
  public String delete(Model model, @RequestParam(value = "caseId") String caseId,
      @RequestParam(value = "confirm", required = false) boolean confirm) {
    if (!confirm) {
      model.addAttribute("errors",
          ImmutableList.of("Add &confirm=true to url to confirm deletetion"));
      model.addAttribute("case", adminManager.getCase(caseId));
    } else {
      adminManager.deleteCase(caseId);
      model.addAttribute("success", "Case caseId=" + caseId + " is deleted");
    }
    return "admin/new-case";
  }

  private Case caseFromRequest(HttpServletRequest request, List<String> errors) {
    List<String> alternativeNames = ImmutableList.of();
    if (request.getParameter("alternativeNames") != null) {
      alternativeNames = Splitter.on(",").trimResults().omitEmptyStrings()
          .splitToList(request.getParameter("alternativeNames"));
    }
    URL imageURL = null;
    if (!Strings.isNullOrEmpty(request.getParameter("imageUrl"))) {
      try {
        imageURL = new URL(request.getParameter("imageUrl"));
      } catch (MalformedURLException e) {
        errors
            .add("Error parsing url:" + request.getParameter("imageUrl") + " : " + e.getMessage());
      }
    }
    Case newCase = new Case(request.getParameter("caseId"), request.getParameter("caseDataId"),
        request.getParameter("name"), alternativeNames, request.getParameter("category"),
        "true".equals(request.getParameter("enabled")), imageURL);
    if (Strings.isNullOrEmpty(newCase.getCaseId())) {
      errors.add("caseId can not be empty");
    }
    if (Strings.isNullOrEmpty(newCase.getName())) {
      errors.add("name can not be empty");
    }
    if (newCase.isEnabled() && Strings.isNullOrEmpty(newCase.getCaseDataId())) {
      errors.add("caseDataId required for enabled cases");
    }

    if (!newCase.getCaseDataId().isEmpty()
        && !newCase.getCaseDataId().startsWith(newCase.getCaseId())) {
      errors.add("caseDataId have to start with " + newCase.getCaseId());
    }

    return newCase;
  }
}
