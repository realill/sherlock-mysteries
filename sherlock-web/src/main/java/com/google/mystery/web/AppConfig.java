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

package com.google.mystery.web;

import java.net.MalformedURLException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;
import com.google.appengine.api.ThreadManager;
import com.google.common.collect.Maps;
import com.google.mystery.actions.ActionsManager;
import com.google.mystery.actions.ActionsTool;
import com.google.mystery.actions.FinishActions;
import com.google.mystery.actions.NavigationActions;
import com.google.mystery.actions.SessionHolder;
import com.google.mystery.actions.SessionManager;
import com.google.mystery.actions.StartCaseActions;
import com.google.mystery.actions.SuggestionsManager;
import com.google.mystery.actions.messages.MessagesManager;
import com.google.mystery.assets.AdminManager;
import com.google.mystery.assets.AssetsImportManager;
import com.google.mystery.assets.AssetsManager;
import com.google.mystery.assets.DialogflowExportEntitiesManager;
import com.google.mystery.assets.GSuiteDataSource;
import com.google.mystery.assets.SearchManager;
import com.google.mystery.assets.StorageManager;
import com.google.mystery.config.SherlockConfig;
import com.google.mystery.data.CacheManager;
import com.google.mystery.data.DataManager;
import com.google.mystery.web.admin.AdminController;
import com.google.mystery.web.admin.CasesController;

@Configuration
@EnableWebMvc
@Import({
    // Admin controllers.
    AdminController.class, CasesController.class,
    // Runtime controllers.
    PublicController.class, WebhookControllerV2.class})
public class AppConfig implements WebMvcConfigurer {
  @Inject
  private CacheManager cacheManager;
  @Inject
  private DataManager dataManager;
  private static Logger logger = Logger.getLogger(AppConfig.class.getName());


  @Bean
  @Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
  public SherlockConfig config() {
    try {
      // Using current context url
      String currentBaseUrl = ServletUriComponentsBuilder.fromCurrentRequest().replacePath("")
          .replaceQuery("").toUriString();
      Map<String, String> configParams = cacheManager.getConfig(k -> dataManager.getConfigMap());
      Map<String, String> newConfigParams = Maps.newHashMap(configParams);
      newConfigParams.put(SherlockConfig.BASE_URL, currentBaseUrl);

      return new SherlockConfig(newConfigParams);
    } catch (MalformedURLException e) {
      logger.log(Level.SEVERE, "Error getting config from database", e);
      return new SherlockConfig();
    }
  }

  @Bean
  public SearchManager searchManager() {
    return new SearchManager();
  }

  @Bean
  public DFV2ResponseGenerator dfv2ResponseGenerator() {
    return new DFV2ResponseGenerator();
  }

  @Bean
  public ActionsManager actionsManager() {
    return new ActionsManager();
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

  @Bean
  public MessagesManager messagesManager() {
    return new MessagesManager();
  }

  @Bean
  public AssetsManager assetsManager() {
    return new AssetsManager();
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
  public DataManager dataManager() {
    return new DataManager();
  }

  @Bean
  public GSuiteDataSource gsheetClient() {
    return new GSuiteDataSource();
  }

  @Bean
  public DialogflowExportEntitiesManager dialogflowExportEntitiesManager() {
    return new DialogflowExportEntitiesManager();
  }

  @Bean
  StorageManager voiceGenerationManager() {
    return new StorageManager();
  }

  @Bean
  public AssetsImportManager assetsImportManager() {
    return new AssetsImportManager();
  }

  @Bean
  public ExecutorService provideExecutor() {
    return Executors.newSingleThreadExecutor(ThreadManager.backgroundThreadFactory());
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/static/**").addResourceLocations("/WEB-INF/static/");
  }

  @Bean
  public ViewResolver viewResolver() {
    FreeMarkerViewResolver resolver = new FreeMarkerViewResolver();
    resolver.setCache(true);
    resolver.setPrefix("");
    resolver.setSuffix(".ftl");
    resolver.setContentType("text/html;charset=utf-8");
    return resolver;
  }

  @Bean
  public FreeMarkerConfigurer freemarkerConfig() {
    FreeMarkerConfigurer freeMarkerConfigurer = new FreeMarkerConfigurer();
    freeMarkerConfigurer.setTemplateLoaderPath("/WEB-INF/views/");
    return freeMarkerConfigurer;
  }

  @Bean
  public CacheManager cacheManager() {
    return new CacheManager();
  }

  @Bean
  public AdminManager adminManager() {
    return new AdminManager();
  }
}
