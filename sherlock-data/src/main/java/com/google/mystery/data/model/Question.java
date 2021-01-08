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

package com.google.mystery.data.model;

import java.util.List;

import com.google.appengine.api.datastore.Entity;
import com.google.common.collect.ImmutableList;
import com.google.mystery.data.DataManager;
import com.google.mystery.data.DataUtil;

/**
 * Questions and answers at end-game
 *
 * @author ilyaplatonov
 */
public class Question {
  private final String caseDataId;
  private final String question;
  private final String answer;
  private final int score;
  private final int order;
  private final List<String> possibleAnswers;

  public List<String> getPossibleAnswers() {
    return possibleAnswers;
  }

  public Question(
      String caseDataId,
      String question,
      String answer,
      int score,
      int order,
      List<String> possibleAnswers) {
    this.caseDataId = caseDataId;
    this.question = question;
    this.answer = answer;
    this.score = score;
    this.order = order;
    this.possibleAnswers = possibleAnswers;
  }

  public Question(String caseDataId, String question, String answer, int score, int order) {
    this(caseDataId, question, answer, score, order, ImmutableList.of());
  }

  /** @return if this is extra question */
  public boolean isExtra() {
    return score <= 60;
  }

  public String getCaseDataId() {
    return caseDataId;
  }

  public String getQuestion() {
    return question;
  }

  public String getAnswer() {
    return answer;
  }

  public int getScore() {
    return score;
  }

  public int getOrder() {
    return order;
  }

  public static Entity toEntity(Question question) {
    Entity entity = new Entity(DataManager.QUESTION_KIND);
    entity.setIndexedProperty(DataManager.CASE_DATA_ID, question.getCaseDataId());
    entity.setUnindexedProperty("question", question.getQuestion());
    entity.setUnindexedProperty("answer", question.getAnswer());
    entity.setUnindexedProperty("score", question.getScore());
    entity.setIndexedProperty("order", question.getOrder());
    DataUtil.setStringListProperty(entity, "possibleAnswers", question.getPossibleAnswers());
    return entity;
  }

  public static Question fromEntity(Entity entity) {
    List<String> possibleAnswers =
        ImmutableList.copyOf(DataUtil.getStringList(entity, "possibleAnswers"));
    return new Question(
        entity.getProperty(DataManager.CASE_DATA_ID).toString(),
        entity.getProperty("question").toString(),
        entity.getProperty("answer").toString(),
        ((Number) entity.getProperty("score")).intValue(),
        ((Number) entity.getProperty("order")).intValue(),
        possibleAnswers);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((answer == null) ? 0 : answer.hashCode());
    result = prime * result + ((caseDataId == null) ? 0 : caseDataId.hashCode());
    result = prime * result + order;
    result = prime * result + ((question == null) ? 0 : question.hashCode());
    result = prime * result + score;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    Question other = (Question) obj;
    if (answer == null) {
      if (other.answer != null) return false;
    } else if (!answer.equals(other.answer)) return false;
    if (caseDataId == null) {
      if (other.caseDataId != null) return false;
    } else if (!caseDataId.equals(other.caseDataId)) return false;
    if (order != other.order) return false;
    if (question == null) {
      if (other.question != null) return false;
    } else if (!question.equals(other.question)) return false;
    if (score != other.score) return false;
    return true;
  }

  @Override
  public String toString() {
    return "Question [caseDataId="
        + caseDataId
        + ", question="
        + question
        + ", answer="
        + answer
        + ", score="
        + score
        + ", order="
        + order
        + "]";
  }
}
