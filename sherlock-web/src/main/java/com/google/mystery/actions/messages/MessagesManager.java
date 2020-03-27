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

import java.io.IOException;
import java.io.StringWriter;
import java.util.Locale;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import com.google.common.collect.ImmutableMap;
import com.google.mystery.assets.AssetsManager;
import com.google.mystery.config.SherlockConfig;
import freemarker.cache.WebappTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

/**
 * Works with templated messages used for answers
 *
 * @author ilyaplatonov
 */
public class MessagesManager {
  private Configuration freemarkerCfg = null;
  @Inject
  private ServletContext context;
  @Inject
  private AssetsManager assetsManager;
  @Inject
  private SherlockConfig config;

  @PostConstruct
  public void init() {
    WebappTemplateLoader templateLoader = new WebappTemplateLoader(context, "WEB-INF/messages/");
    freemarkerCfg = new Configuration(Configuration.VERSION_2_3_26);
    freemarkerCfg.setTemplateLoader(templateLoader);
    freemarkerCfg.setDefaultEncoding("UTF-8");
    freemarkerCfg.setLocale(Locale.US);
    freemarkerCfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
  }

  public String message(String id) throws IOException, MessageException {
    return message(id, ImmutableMap.of());
  }

  public String message(String id, Map<String, Object> context)
      throws IOException, MessageException {
    if (!context.containsKey("random")) {
      ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
      builder.putAll(context);
      builder.put("tool", new MessagesTool(assetsManager, config));
      context = builder.build();
    }
    Template template = freemarkerCfg.getTemplate(id + ".ftl");
    StringWriter output = new StringWriter();
    try {
      template.process(context, output);
    } catch (TemplateException e) {
      throw new MessageException("Freemarker error", e);
    }
    return output.toString();
  }
}
