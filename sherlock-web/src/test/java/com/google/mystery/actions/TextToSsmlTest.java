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

import org.junit.Ignore;
import org.junit.Test;
import com.google.common.truth.Truth;
import com.google.mystery.actions.model.SherlockResponse;

public class TextToSsmlTest {

  public String makeSsml(String text) {
    return SherlockResponse.textToSsml(SherlockResponse.normalize(text));
  }

  @Test
  @Ignore
  public void comma_emphasis() {
    String ssml = makeSsml("he said \"hello how are you\" and blinked");
    Truth.assertThat(ssml)
        .contains("he said <emphasis>\"hello how are you\"</emphasis> and blinked");

    ssml = makeSsml("\"hello how are you\"");
    Truth.assertThat(ssml).contains("<emphasis>\"hello how are you\"</emphasis>");

    ssml = makeSsml("a \"b\" c \"d\"");
    Truth.assertThat(ssml).contains("a <emphasis>\"b\"</emphasis> c <emphasis>\"d\"</emphasis>");
  }

  @Test
  public void threeDots_break() {
    String ssml = makeSsml("hello...how..are...you");
    Truth.assertThat(ssml)
        .contains("hello<break time=\"150ms\"/>how..are<break time=\"150ms\"/>you");
  }
}
