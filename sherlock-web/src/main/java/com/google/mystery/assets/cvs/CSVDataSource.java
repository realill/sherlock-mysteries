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

/** */
package com.google.mystery.assets.cvs;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.mystery.assets.IImportDataSource;

/** @author Ilya Platonov */
public class CSVDataSource implements IImportDataSource {
  @Override
  public List<List<Object>> getData(HttpRequestInitializer credential, String id, String range)
      throws IOException {
    String[] split = range.split("!");
    if (split.length == 0) {
      throw new IllegalArgumentException("Invalid range " + range);
    }
    String sheet = split[0];
    Iterable<CSVRecord> records =
        CSVFormat.DEFAULT.parse(
            new InputStreamReader(
                this.getClass().getResourceAsStream(String.format("/%s/%s.csv", id, sheet)),
                Charsets.UTF_8));
    List<List<Object>> result = new ArrayList<>();
    for (CSVRecord record : records) {
      List<String> row = Lists.newArrayList(record.iterator());
      for (ListIterator<String> i = row.listIterator(row.size()); i.hasPrevious(); ) {
        String prev = i.previous();
        if (prev == null || prev.isEmpty()) {
          i.remove();
        } else {
          break;
        }
      }
      result.add((List) row);
    }

    return result;
  }
}
