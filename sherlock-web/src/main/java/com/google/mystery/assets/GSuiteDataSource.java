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

package com.google.mystery.assets;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.DocsScopes;
import com.google.api.services.docs.v1.model.Document;
import com.google.api.services.docs.v1.model.ParagraphElement;
import com.google.api.services.docs.v1.model.StructuralElement;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.mystery.config.SherlockConfig;

/**
 * Imports case assets from google GSuite.
 *
 * @author ilyaplatonov
 */
@Singleton
public class GSuiteDataSource implements IImportDataSource {
  /** Application name. */
  private static final String APPLICATION_NAME = "Sherlock Mysteries";
  /** Global instance of the JSON factory. */
  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
  /**
   * Permission scopes
   */
  public static final List<String> SCOPES =
      Arrays.asList(SheetsScopes.SPREADSHEETS_READONLY, DocsScopes.DOCUMENTS_READONLY);

  /** Stores user credentials */
  private static DataStoreFactory dataStoreFactory;

  /** Global instance of the HTTP transport. */
  private static HttpTransport httpTransport;
  /** auth flow configuration. */
  private static GoogleAuthorizationCodeFlow authFlow = null;

  private Logger logger = Logger.getLogger(this.getClass().getName());

  @Inject
  private SherlockConfig config;

  @PostConstruct
  public void init() {
    try {
      httpTransport = GoogleNetHttpTransport.newTrustedTransport();
      dataStoreFactory = new MemoryDataStoreFactory();

    } catch (Throwable t) {
      logger.log(Level.SEVERE, "Error initializing GSuite transport classes", t);
    }
  }

  public synchronized GoogleAuthorizationCodeFlow getAuthFlow() throws IOException {
    if (authFlow == null) {
      // Load client secrets.
      InputStream in =
          new ByteArrayInputStream(config.getDocsSecret().getBytes(StandardCharsets.UTF_8));
      GoogleClientSecrets clientSecrets =
          GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

      authFlow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets,
          SCOPES).setDataStoreFactory(dataStoreFactory).setAccessType("online").build();
    }

    return authFlow;
  }

  /**
   * Build and return an authorized Sheets API client service.
   *
   * @return an authorized Sheets API client service
   * @throws IOException
   */
  protected Sheets getSheetsService(HttpRequestInitializer credential) throws IOException {
    return new Sheets.Builder(httpTransport, JSON_FACTORY, credential)
        .setApplicationName(APPLICATION_NAME).build();
  }

  @Override
  public List<List<Object>> getData(final HttpRequestInitializer credential, String id,
      String range) throws IOException {
    Sheets service = getSheetsService(credential);
    String spreadsheetId = extractDocumentId(id);
    ValueRange sheetsResponse = service.spreadsheets().values().get(spreadsheetId, range).execute();
    return sheetsResponse.getValues();
  }

  public static class DocEntity {
    public DocEntity(String header, String content) {
      this.header = header;
      this.content = content;
    }

    private String header;
    private String content;

    public String getHeader() {
      return header;
    }

    public String getContent() {
      return content;
    }
  }

  public List<DocEntity> loadDocsData(final HttpRequestInitializer credential, String idOrUrl)
      throws IOException {
    List<DocEntity> result = new ArrayList<>();
    Docs service = new Docs.Builder(httpTransport, JSON_FACTORY, credential)
        .setApplicationName(APPLICATION_NAME).build();
    String docId = extractDocumentId(idOrUrl);;
    Document doc = service.documents().get(docId).execute();

    String header = null;
    StringBuilder content = new StringBuilder();
    for (StructuralElement e : doc.getBody().getContent()) {
      if (e.getParagraph() != null && e.getParagraph().getParagraphStyle() != null) {
        // Header
        if (e.getParagraph().getParagraphStyle().getNamedStyleType().startsWith("HEADING")) {
          if (header != null && !header.trim().isEmpty() && !content.toString().trim().isEmpty()) {
            addEntity(result, header, content);
          }
          StringBuilder headerBuilder = new StringBuilder();
          for (ParagraphElement pe : e.getParagraph().getElements()) {
            if (pe != null && pe.getTextRun() != null && pe.getTextRun().getContent() != null) {
              headerBuilder.append(pe.getTextRun().getContent());
            }
          }
          header = headerBuilder.toString();
          content = new StringBuilder();
        } else if (e.getParagraph().getParagraphStyle().getNamedStyleType().equals("NORMAL_TEXT")) {
          for (ParagraphElement pe : e.getParagraph().getElements()) {
            if (pe != null && pe.getTextRun() != null) {
              if (pe.getTextRun().getTextStyle() != null
                  && pe.getTextRun().getTextStyle().getBold() != null
                  && pe.getTextRun().getTextStyle().getBold()) {
                String trimmedContent = pe.getTextRun().getContent().trim();
                if (!trimmedContent.isEmpty()) {
                  content.append(pe.getTextRun().getContent().replace(trimmedContent,
                      "[" + trimmedContent + "]"));
                } else {
                  content.append(pe.getTextRun().getContent());
                }
              } else {
                content.append(pe.getTextRun().getContent());
              }
            }
          }
        }
      }
    }
    if (!content.toString().isEmpty()) {
      addEntity(result, header, content);
    }
    return result;
  }

  public void addEntity(List<DocEntity> result, String header, StringBuilder content) {
    // Hack to avoid consequential highlights.
    String finalContent = content.toString().trim().replace("][", "").replace("] [", " ");
    result.add(new DocEntity(header.trim(), finalContent));
  }

  protected static String extractDocumentId(String urlOrId) {
    if (urlOrId.contains("/")) {
      Pattern r = Pattern.compile("\\/d\\/([^\\/]+)\\/");
      Matcher matcher = r.matcher(urlOrId);
      if (matcher.find()) {
        return matcher.group(1);
      }
    } else {
      return urlOrId;
    }
    return null;
  }
}
