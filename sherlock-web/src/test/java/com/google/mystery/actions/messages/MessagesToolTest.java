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

package com.google.mystery.actions.messages;

import org.junit.Test;
import com.google.common.truth.Truth;
import com.google.mystery.assets.AssetsManager;
import com.google.mystery.config.SherlockConfig;

public class MessagesToolTest {

  @Test
  public void money() {
    MessagesTool tools = new MessagesTool(new AssetsManager(), new SherlockConfig());
    Truth.assertThat(tools.money(0)).isEqualTo("0 pence");
    Truth.assertThat(tools.money(11)).isEqualTo("11 pence");
    Truth.assertThat(tools.money(12)).isEqualTo("1 shilling");
    Truth.assertThat(tools.money(13)).isEqualTo("1 shilling and 1 pence");
    Truth.assertThat(tools.money(24)).isEqualTo("2 shillings");
    Truth.assertThat(tools.money(26)).isEqualTo("2 shillings and 2 pence");
    Truth.assertThat(tools.money(240)).isEqualTo("1 pound");
    Truth.assertThat(tools.money(252)).isEqualTo("1 pound and 1 shilling");
    Truth.assertThat(tools.money(255)).isEqualTo("1 pound 1 shilling and 3 pence");
    Truth.assertThat(tools.money(495)).isEqualTo("2 pounds 1 shilling and 3 pence");
  }
}
