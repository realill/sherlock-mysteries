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

package com.google.mystery.config;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import com.google.mystery.Utils;

public class SherlockConfig implements Serializable {
  private static final long serialVersionUID = -5231863125114895405L;

  public static final String FOOTER_SCRIPTS = "footerScripts";

  public static final String BUCKET_NAME = "bucketName";

  public static final String DOCS_SECRET = "docsSecret";

  public static final String DIALOGFLOW_SECRET = "dialogflowSecret";

  public static final String ASSISTANT_DIRECTORY_URL = "assistantDirectoryUrl";

  public static final String BASE_URL = "baseUrl";

  private URL baseUrl;
  private URL assistantDirectoryUrl;
  private String docsSecret;
  private String bucketName;
  private String footerScripts;
  private String dialogflowSecret;

  public SherlockConfig() {
    baseUrl = null;
    assistantDirectoryUrl = null;
    docsSecret = null;
    bucketName = null;
    footerScripts = null;
    dialogflowSecret = null;
  }

  public SherlockConfig(Map<String, String> namedParams) throws MalformedURLException {
    baseUrl = namedParams.get(BASE_URL) != null ? new URL(namedParams.get(BASE_URL)) : null;
    assistantDirectoryUrl = namedParams.get(ASSISTANT_DIRECTORY_URL) != null
        ? new URL(namedParams.get(ASSISTANT_DIRECTORY_URL))
        : null;
    docsSecret = namedParams.get(DOCS_SECRET);
    bucketName = namedParams.get(BUCKET_NAME);
    footerScripts = namedParams.get(FOOTER_SCRIPTS);
    dialogflowSecret = namedParams.get(DIALOGFLOW_SECRET);
  }

  public String getDocsSecret() {
    return docsSecret != null ? docsSecret : "";
  }

  public String getBucketName() {
    return bucketName != null ? bucketName : "PUT-YOUR-CLOUD-BUCKET-HERE";
  }

  public URL getBaseUrl() {
    return baseUrl != null ? baseUrl : Utils.checkedURL("https://example.org");
  }


  public URL getAssistantDirectoryUrl() {
    return assistantDirectoryUrl != null ? assistantDirectoryUrl
        : Utils.checkedURL("https://assistant.google.com/services/r/uid/000000b0ff87b6b1");
  }

  public URL getPlaceholderUrl() {
    return getUrl("/static/logo.jpg");
  }


  public String getFooterScripts() {
    return footerScripts != null ? footerScripts : "";
  }

  public String getDialogflowSecret() {
    return dialogflowSecret != null ? dialogflowSecret : "";
  }

  public URL getSessionURL(String sessionid) {
    return getSessionURL(sessionid, null);
  }

  public URL getSessionURL(String sessionid, String anchor) {
    try {
      if (anchor == null) {
        return new URL(getBaseUrl(), "/session/" + sessionid + "/");
      } else {
        return new URL(getBaseUrl(), "/session/" + sessionid + "/#" + anchor);
      }
    } catch (MalformedURLException e) {
      // should never happen
      throw new IllegalArgumentException(e);
    }
  }

  public URL getURL(String sessionid, String path, String anchor) {
    try {
      if (anchor == null) {
        return new URL(getBaseUrl(), "/session/" + sessionid + "/" + path);
      } else {
        return new URL(getBaseUrl(), "/session/" + sessionid + "/" + path + "#" + anchor);
      }
    } catch (MalformedURLException e) {
      // should never happen
      throw new IllegalArgumentException(e);
    }
  }

  public URL getCluesURL(String sessionid, String anchor) {
    return getURL(sessionid, "clues", anchor);
  }

  public URL getUrl(String path) {
    return Utils.checkedURL(getBaseUrl(), path);
  }

}
