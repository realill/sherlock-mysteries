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

import java.util.concurrent.ExecutorService;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.mystery.assets.cvs.CSVDataSource;
import com.google.mystery.config.SherlockConfig;
import com.google.mystery.data.CacheManager;
import com.google.mystery.data.DataManager;
import com.sherlockmysteries.pdata.SMDataManager;

@Configuration
public class GeneralTestingConfig {
  @Bean
  public LongOperationsManager assetsImportManager() {
    return new LongOperationsManager();
  }

  @Bean
  public IImportDataSource dataSource() {
    return new CSVDataSource();
  };

  @Bean
  public DataManager dataManager() {
    return new DataManager();
  }

  @Bean
  public ExecutorService executorService() {
    return MoreExecutors.newDirectExecutorService();
  }

  @Bean
  public AssetsManager assetsManager() {
    return new AssetsManager();
  }

  @Bean
  public StorageManager storageManager() {
    return Mockito.mock(StorageManager.class);
  }

  @Bean
  public SherlockConfig sherlockConfig() {
    return new SherlockConfig();
  }

  @Bean
  public SearchManager searchManager() {
    return new SearchManager();
  }

  @Bean
  public CacheManager cacheManageR() {
    return new CacheManager();
  }

  @Bean
  public SMDataManager smDataManager() {
    return new SMDataManager();
  }
}
