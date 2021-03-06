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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.google.mystery.config.SherlockConfig;
import com.google.mystery.data.CacheManager;
import com.google.mystery.data.DataManager;

@Configuration
public class AdminManagerTestingConfig {

  @Bean
  public AdminManager adminManager() {
    return new AdminManager();
  }

  @Bean
  public DataManager dataManager() {
    return new DataManager();
  }

  @Bean
  public SherlockConfig sherlockConfig() {
    return new SherlockConfig();
  }

  @Bean
  public SearchManager searchManageR() {
    return new SearchManager();
  }

  @Bean
  public CacheManager cacheManager() {
    return new CacheManager();
  }

  @Bean
  AssetsManager assetsManager() {
    return new AssetsManager();
  }
}
