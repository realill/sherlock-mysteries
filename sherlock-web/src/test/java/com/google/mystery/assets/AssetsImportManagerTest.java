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

import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import com.google.common.truth.Truth;
import com.google.mystery.assets.GSuiteDataSource.DocEntity;
import com.google.mystery.data.model.StoryData;

@RunWith(JUnit4.class)
public class AssetsImportManagerTest {

  @Test
  public void extractDocumentId_id() {
    String id = UUID.randomUUID().toString();
    Truth.assertThat(GSuiteDataSource.extractDocumentId(id)).isEqualTo(id);
  }

  @Test
  public void extractDocumentId_url() {
    String id = UUID.randomUUID().toString();
    String url = "https://docs.google.com/spreadsheets/d/" + id + "/edit#gid=1818721384";
    Truth.assertThat(GSuiteDataSource.extractDocumentId(url)).isEqualTo(id);
  }

  @Test
  public void extractDocumentId_invalid() {
    String id = UUID.randomUUID().toString();
    String url = "https://docs.google.com/spreadsheets/asdasdas/" + id + "/edit#gid=1818721384";
    Truth.assertThat(GSuiteDataSource.extractDocumentId(url)).isNull();
  }

  @Test
  public void docEntityToStory() {
    StoryData story = AssetsImportManager.docEntityToStory(new DocEntity("header", "My content"));
    Truth.assertThat(story).isEqualTo(new StoryData(null, "header", null, null, "My content"));

    story = AssetsImportManager.docEntityToStory(new DocEntity("header", "ID:myid\n\nMy content"));
    Truth.assertThat(story).isEqualTo(new StoryData("myid", "header", null, null, "My content"));

    story = AssetsImportManager.docEntityToStory(
        new DocEntity("header", "ID:myid\nclues: clue1, clue2 \nLatLong: 111, 222\nMy content"));
    Truth.assertThat(story)
        .isEqualTo(new StoryData("myid", "header", "clue1, clue2", "111, 222", "My content"));

    story =
        AssetsImportManager.docEntityToStory(new DocEntity("header", "\n\nID:myid\n\nMy content"));
    Truth.assertThat(story).isEqualTo(new StoryData("myid", "header", null, null, "My content"));

    story = AssetsImportManager.docEntityToStory(new DocEntity("header", "ID:myid\nMy content"));
    Truth.assertThat(story).isEqualTo(new StoryData("myid", "header", null, null, "My content"));
  }

  @Test
  public void splitWithLimit() {
    Truth.assertThat(AssetsImportManager.splitWithLimit("123456\n123", 7)).containsExactly("123456",
        "123");
    Truth.assertThat(AssetsImportManager.splitWithLimit("123456\n123", 20))
        .containsExactly("123456\n123");

    Truth.assertThat(AssetsImportManager.splitWithLimit("123456\n123\n123456\n123456789", 13))
        .containsExactly("123456\n123", "123456", "123456789");
  }
}
