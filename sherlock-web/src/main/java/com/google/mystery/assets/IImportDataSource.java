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

import java.io.IOException;
import java.util.List;

import com.google.api.client.http.HttpRequestInitializer;

/**
 * Abstraction to import game data from spreadsheets like datasource
 *
 * @author ilyaplatonov
 */
public interface IImportDataSource {
  public List<List<Object>> getData(
      final HttpRequestInitializer credential, String id, String range) throws IOException;
}
