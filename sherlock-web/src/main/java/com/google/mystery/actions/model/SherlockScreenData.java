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

package com.google.mystery.actions.model;

/**
 * Information about what is showed on the screen: story, map, etc
 * 
 * @author ilyaplatonov
 *
 */
public class SherlockScreenData {
  /** type of show data. */
  public static enum Type {
    STORY, MAP, NEWSPAPER, MENU, BASE
  }

  private final Type type;
  private boolean showText;


  public SherlockScreenData(Type type, boolean showText) {
    this.type = type;
    this.showText = showText;
  }

  public Type getType() {
    return type;
  }

  public boolean isShowText() {
    return showText;
  }
}
