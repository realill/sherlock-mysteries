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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import org.apache.commons.text.WordUtils;
import com.google.common.base.Joiner;
import com.google.mystery.actions.model.SherlockResponse;
import com.google.mystery.assets.AssetsManager;
import com.google.mystery.config.SherlockConfig;

public class MessagesTool {
  private AssetsManager assetsManager;
  private Random random;
  private SherlockConfig config;

  public MessagesTool(AssetsManager assetsManager, SherlockConfig config) {
    this.random = new Random();
    this.assetsManager = assetsManager;
    this.config = config;
  }

  public String money(int pence) {
    int pound = pence / 240;
    int shilling = (pence % 240) / 12;
    pence = pence % 12;
    List<String> elements = new ArrayList<>(3);
    if (pound == 1) {
      elements.add(String.format("%d pound", pound));
    } else if (pound > 1) {
      elements.add(String.format("%d pounds", pound));
    }

    if (shilling == 1) {
      elements.add(String.format("%d shilling", shilling));
    } else if (shilling > 1) {
      elements.add(String.format("%d shillings", shilling));
    }

    if (pence > 0) {
      elements.add(String.format("%d pence", pence));
    }

    if (elements.isEmpty()) {
      return "0 pence";
    }

    int size = elements.size();
    if (size == 1) {
      return elements.get(0);
    }
    return Joiner.on(" ").join(elements.subList(0, size - 1)) + " and " + elements.get(size - 1);
  }

  public String capitalize(String string) {
    return WordUtils.capitalize(string);
  }

  public int random(int max) {
    return random.nextInt(max);
  }

  public boolean isAddress(String location) {
    return assetsManager.checkAddress(location) != null;
  }

  public String textToHtml(String text) {
    return SherlockResponse.textToHtml(text);
  }

  public String time() {
    return String.valueOf(new Date().getTime());
  }

  public String footerScripts() {
    return config.getFooterScripts();
  }
}
