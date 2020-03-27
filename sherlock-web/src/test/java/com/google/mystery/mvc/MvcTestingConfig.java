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

package com.google.mystery.mvc;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.WebApplicationContext;
import com.google.mystery.actions.ActionsManager;
import com.google.mystery.actions.ActionsTool;
import com.google.mystery.actions.FinishActions;
import com.google.mystery.actions.NavigationActions;
import com.google.mystery.actions.SessionHolder;
import com.google.mystery.actions.SessionManager;
import com.google.mystery.actions.StartCaseActions;
import com.google.mystery.actions.SuggestionsManager;
import com.google.mystery.actions.messages.MessagesManager;
import com.google.mystery.assets.GeneralTestingConfig;
import com.google.mystery.config.SherlockConfig;
import com.google.mystery.data.CacheManager;
import com.google.mystery.web.DFV2ResponseGenerator;
import com.google.mystery.web.PublicController;
import com.google.mystery.web.WebhookControllerV2;

@Configuration
@Import(value = {GeneralTestingConfig.class, PublicController.class, WebhookControllerV2.class})
public class MvcTestingConfig {

  @Bean
  public SherlockConfig config() {
    return new SherlockConfig();
  }

  @Bean
  public DFV2ResponseGenerator dfv2ResponseGenerator() {
    return new DFV2ResponseGenerator();
  }

  @Bean
  public MysteryMvcTester mvcTester() {
    return new MysteryMvcTester();
  }

  @Bean
  public ActionsManager actionsManager() {
    return new ActionsManager();
  }

  @Bean
  public MessagesManager messagesManager() {
    return new MessagesManager();
  }

  @Bean
  public SessionManager sessionManager() {
    return new SessionManager();
  }

  @Bean
  public SuggestionsManager suggestionsManager() {
    return new SuggestionsManager();
  }

  @Bean
  @Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
  public SessionHolder sessionHolder() {
    return new SessionHolder();
  }

  @Bean
  public CacheManager cacheManager() {
    return new CacheManager();
  }

  @Bean
  public ActionsTool actionsTool() {
    return new ActionsTool();
  }

  @Bean
  public StartCaseActions startCaseActions() {
    return new StartCaseActions();
  }

  @Bean
  public NavigationActions navigationActions() {
    return new NavigationActions();
  }

  @Bean
  public FinishActions finishActions() {
    return new FinishActions();
  }
}
