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

package com.google.mystery;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class Utils {
  /**
   * Creating URL instance from string and throwing {@link IllegalArgumentException} instead of
   * {@link MalformedURLException}
   */
  public static URL checkedURL(String url) {
    try {
      return new URL(url);
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public static String escapeUrl(String stringurl) {
    URL url = checkedURL(stringurl);
    try {
      URI uri = new URI(url.getProtocol(), url.getHost(), url.getPath(), null);
      return uri.toString();
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }

  }

  /**
   * Creating URL instance from string and throwing {@link IllegalArgumentException} instead of
   * {@link MalformedURLException}
   */
  public static URL checkedURL(URL base, String path) {
    try {
      return new URL(base, path);
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
