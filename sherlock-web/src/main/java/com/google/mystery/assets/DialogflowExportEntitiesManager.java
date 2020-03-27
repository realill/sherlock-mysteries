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
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.dialogflow.v2.EntityType;
import com.google.cloud.dialogflow.v2.EntityType.Kind;
import com.google.cloud.dialogflow.v2.EntityTypesClient;
import com.google.cloud.dialogflow.v2.EntityTypesSettings;
import com.google.cloud.dialogflow.v2.ProjectAgentName;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.mystery.config.SherlockConfig;
import com.google.mystery.data.model.Case;
import com.google.mystery.data.model.Clue;
import com.google.mystery.data.model.Story;

/**
 * Used to export entities into Dialogflow.
 *
 * @author ilyaplatonov
 */
public class DialogflowExportEntitiesManager {
  private static Logger logger = Logger.getLogger(DialogflowExportEntitiesManager.class.getName());
  @Inject
  protected AssetsManager assetsManager;
  @Inject
  protected AdminManager adminManager;
  @Inject
  protected SherlockConfig config;

  @Inject
  public DialogflowExportEntitiesManager() {}

  public void update(String projectName, PrintWriter writer) {
    try {
      writer.println("Listing existing entities");
      List<EntityType> existingEntities = ImmutableList
          .copyOf(getClient().listEntityTypes(ProjectAgentName.of(projectName)).iterateAll());
      writer.println("Updating Streets Entity");
      updateEntity(projectName, "Streets", toEntityMap(assetsManager.getStreets().iterator()),
          existingEntities);
      writer.println("Streets updated");
      writer.println("Updating Locations Entity");
      updateEntity(projectName, "Locations", toEntityMap(assetsManager.getLocations().iterator()),
          existingEntities);
      writer.println("Locations updated");

      writer.println("Updating Articles Entity");
      updateArticlesEntity(projectName, existingEntities);
      writer.println("Articles updated");

      writer.println("Updating Clues entity");
      updateClues(projectName, existingEntities);
      writer.println("Clues updated");

      writer.println("Updating Cases entity");
      updateCasesEntity(projectName, existingEntities);
      writer.println("Cases updated");
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error updating", e);
      e.printStackTrace(writer);
    }
  }

  private void updateClues(String projectName, List<EntityType> existing) throws IOException {
    Map<String, List<String>> entries = new HashMap<>();
    for (Clue clue : adminManager.getGlobalAllClues()) {
      entries.put(clue.getName(), ImmutableList.of(clue.getName()));
    }
    updateEntity(projectName, "Clues", entries, existing);
  }

  protected void updateArticlesEntity(String projectName, List<EntityType> existing)
      throws IOException {
    List<Story> articles = adminManager.getGlobalAllAricles();
    Map<String, List<String>> entries = new HashMap<>();
    for (Story article : articles) {
      entries.put(article.getTitle(), ImmutableList.of(article.getTitle()));
    }

    updateEntity(projectName, "Articles", entries, existing);
  }

  /** number list phrases that could be used to reference cases. */
  private static List<List<String>> extraCaseNumberedPhrases = Lists.newArrayList();

  static {
    extraCaseNumberedPhrases.add(ImmutableList.of("first", "first one", "first case", "one", "1"));
    extraCaseNumberedPhrases
        .add(ImmutableList.of("second", "second one", "second case", "two", "2"));
    extraCaseNumberedPhrases.add(ImmutableList.of("third", "third one", "third case", "3", "3"));
  }

  protected void updateCasesEntity(String projectName, List<EntityType> existing)
      throws IOException {
    List<Case> cases = adminManager.getAllCases();
    Map<String, List<String>> entries = new HashMap<>();
    for (ListIterator<Case> i = cases.listIterator(); i.hasNext();) {
      Case c = i.next();
      ImmutableList.Builder<String> builder = ImmutableList.builder();
      builder.add(c.getName());
      builder.addAll(c.getAlternativeNames());
      if (extraCaseNumberedPhrases.size() > i.previousIndex()) {
        builder.addAll(extraCaseNumberedPhrases.get(i.previousIndex()));
      }
      entries.put(c.getCaseId(), builder.build());
    }

    updateEntity(projectName, "Cases", entries, existing);
  }

  private Map<String, List<String>> toEntityMap(Iterator<String> values) {
    Map<String, List<String>> entries = new HashMap<>();
    while (values.hasNext()) {
      String v = values.next();
      entries.put(v, ImmutableList.of(v));
    }
    return entries;
  }

  /** Updating entity on api.ai server */
  protected void updateEntity(String projectName, String name, Map<String, List<String>> entries,
      List<EntityType> existing) throws IOException {
    EntityTypesClient client = getClient();

    EntityType.Builder entityType =
        EntityType.newBuilder().setDisplayName(name).setKind(Kind.KIND_MAP);
    for (Map.Entry<String, List<String>> e : entries.entrySet()) {
      entityType.addEntitiesBuilder().setValue(e.getKey()).addAllSynonyms(e.getValue());
    }

    for (EntityType e : existing) {
      if (e.getDisplayName().equals(name)) {
        entityType.setName(e.getName());
        client.updateEntityType(entityType.build());
        return;
      }
    }
    throw new IOException("Can not find entity with name=" + name);
  }

  protected EntityTypesClient getClient() throws IOException {
    FixedCredentialsProvider provider = FixedCredentialsProvider.create(ServiceAccountCredentials
        .fromStream(new ByteArrayInputStream(config.getDialogflowSecret().getBytes())));
    EntityTypesSettings settings =
        EntityTypesSettings.newBuilder().setCredentialsProvider(provider).build();
    EntityTypesClient client = EntityTypesClient.create(settings);
    return client;
  }
}
