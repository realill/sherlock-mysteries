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

package com.google.mystery.data;

import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalSearchServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.common.collect.ImmutableList;
import com.google.common.truth.Truth;
import com.google.mystery.data.model.Session;
import com.google.mystery.data.model.Session.State;
import com.google.mystery.data.model.SessionLog;

@RunWith(MockitoJUnitRunner.class)
public class DataManagerTest {
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(
          new LocalDatastoreServiceTestConfig(),
          new LocalSearchServiceTestConfig(),
          new LocalMemcacheServiceTestConfig());

  @InjectMocks private DataManager dataManager = new DataManager();
  @Spy CacheManager cacheManager;

  @Before
  public void setUp() {
    helper.setUp();
    cacheManager.init();
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test
  public void session_putGet() {
    Session session =
        Session.builder(UUID.randomUUID().toString())
            .state(State.CASE_STARTED)
            .locationsBacklog(ImmutableList.of("l1", "l2"))
            .build();
    dataManager.putSession(session);
    Truth.assertThat(dataManager.getSession(session.getSessionid())).isEqualTo(session);
  }

  @Test
  public void sessionLog_pushGet() {
    String sessionid = UUID.randomUUID().toString();
    SessionLog sessionLog1 = new SessionLog(sessionid, "storyid1", ImmutableList.of(), null);
    SessionLog sessionLog2 = new SessionLog(sessionid, "storyid2", ImmutableList.of(), "hintid2");
    SessionLog sessionLog3 =
        new SessionLog(sessionid, "storyid3", ImmutableList.of("clue1", "clue2"), "hintid3");

    dataManager.pushSessionLog(sessionLog1);
    dataManager.pushSessionLog(sessionLog2);
    dataManager.pushSessionLog(sessionLog3);
    Truth.assertThat(dataManager.getSessionLogs(sessionid))
        .isEqualTo(ImmutableList.of(sessionLog1, sessionLog2, sessionLog3));
  }

  @Test
  public void getSessionLog_time() {
    String sessionid = UUID.randomUUID().toString();
    SessionLog sessionLog1 = new SessionLog(sessionid, "storyid1", ImmutableList.of(), null);
    SessionLog sessionLog2 = new SessionLog(sessionid, "storyid2", ImmutableList.of(), "hintid2");
    SessionLog sessionLog3 =
        new SessionLog(sessionid, "storyid3", ImmutableList.of("clue1", "clue2"), "hintid3");

    dataManager.pushSessionLog(sessionLog1);
    long time = System.currentTimeMillis();
    try {
      Thread.sleep(1);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    dataManager.pushSessionLog(sessionLog2);
    dataManager.pushSessionLog(sessionLog3);
    Truth.assertThat(dataManager.getSessionLogs(sessionid, time))
        .containsExactly(sessionLog2, sessionLog3);
  }

  public void config_putAndGet() {
    Truth.assertThat(dataManager.getConfig("k2")).containsExactly();

    dataManager.putConfig("k1", ImmutableList.of());
    dataManager.putConfig("k2", ImmutableList.of("v1"));
    dataManager.putConfig("k3", ImmutableList.of("v1", "v2"));

    Truth.assertThat(dataManager.getConfig("noKey")).containsExactly();
    Truth.assertThat(dataManager.getConfig("k1")).containsExactly();
    Truth.assertThat(dataManager.getConfig("k2")).containsExactly("v1");
    Truth.assertThat(dataManager.getConfig("k3")).containsExactly("v1", "v2");
  }
}
