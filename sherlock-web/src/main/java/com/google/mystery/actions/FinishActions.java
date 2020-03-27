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
package com.google.mystery.actions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.inject.Inject;
import com.google.common.collect.ImmutableMap;
import com.google.mystery.actions.messages.MessageException;
import com.google.mystery.actions.messages.MessagesManager;
import com.google.mystery.actions.model.SherlockAction;
import com.google.mystery.actions.model.SherlockRequest;
import com.google.mystery.actions.model.SherlockResponseBuilder;
import com.google.mystery.assets.AssetsManager;
import com.google.mystery.data.model.Question;
import com.google.mystery.data.model.Session;
import com.google.mystery.data.model.Session.State;
import com.google.mystery.data.model.Story;

/**
 * Actions to finish case and answer and validate questions.
 *
 * @author ilyaplatonov
 */
public class FinishActions {

  @Inject
  private ActionsTool actionsTool;
  @Inject
  private AssetsManager assetsManager;
  @Inject
  private MessagesManager messages;
  @Inject
  private SessionManager sessionManager;
  private static final Logger logger = Logger.getLogger(FinishActions.class.getName());

  @SherlockAction(value = "finish-validate-repeat", requireCase = true, state = State.ANSWERS)
  public void validateRepeat(SherlockRequest request, SherlockResponseBuilder response)
      throws MessageException, IOException {
    Session session = sessionManager.getSession();
    int answerIndex = session.getAnswersResults().size();
    Question nextQuestion = getQuestion(request.getCaseDataId(), answerIndex);
    String answer = session.getAnswers().get(answerIndex);
    response.storyText(messages.message("validateAnswerRepeat",
        ImmutableMap.of("nextQuestion", nextQuestion, "answer", answer)));
    response.confirmRejectSuggestions();
    response.addOutContext("validate-answer-confirm", 3);
  }

  @SherlockAction(value = "finish-answer-repeat", requireCase = true, state = State.QUESTIONS)
  public void answerRepeat(SherlockRequest request, SherlockResponseBuilder response)
      throws MessageException, IOException {
    Session session = sessionManager.getSession();
    int answerIndex = session.getAnswers().size();
    int questionsSize = getQuestionsSize(request.getCaseDataId());
    if (answerIndex <= questionsSize) {
      askQuestion(request.getCaseDataId(), response, answerIndex, questionsSize);
    }
  }

  @SherlockAction(value = "validate-answer-confirm", requireCase = true, state = State.ANSWERS)
  public void validateAnswerConfirmed(SherlockRequest request, SherlockResponseBuilder response)
      throws MessageException, IOException {
    validateAnswer(request, response, true);
  }

  @SherlockAction(value = "validate-answer-reject", requireCase = true, state = State.ANSWERS)
  public void validateAnswerReject(SherlockRequest request, SherlockResponseBuilder response)
      throws MessageException, IOException {
    validateAnswer(request, response, false);
  }

  private void validateAnswer(SherlockRequest request, SherlockResponseBuilder response,
      boolean correct) throws MessageException, IOException {
    Session session = sessionManager.getSession();
    int answerIndex = session.getAnswersResults().size();
    int questionsSize = getQuestionsSize(request.getCaseDataId());

    if (answerIndex < questionsSize) {
      sessionManager.addAnswerResult(correct);
    } else {
      logger.severe("validate-answer after it there are no answers");
      finishWelcome(request, response);
    }
    Question question = getQuestion(request.getCaseDataId(), answerIndex);
    // finish
    if (answerIndex + 1 == questionsSize) {
      sessionManager.sessionState(State.FINISH);
      FinalStatus stats = stats(request.getCaseDataId(), sessionManager.getSession());
      response.storyText(messages.message("validateAnswerFinal",
          ImmutableMap.of("correct", correct, "question", question, "stats", stats)));
      response.afterstoryText(messages.message("validateAnswerFinalAfter"));
      response.addOutContext("validate-answer-confirm", 0);
      actionsTool.showADLink(response);
      sessionManager.pushFinalSessionLog();
    } else {
      // next question validation
      Question nextQuestion = getQuestion(request.getCaseDataId(), answerIndex + 1);
      String answer = session.getAnswers().get(answerIndex + 1);
      response.storyText(messages.message("validateAnswerNext", ImmutableMap.of("correct", correct,
          "question", question, "nextQuestion", nextQuestion, "answer", answer)));
      response.confirmRejectSuggestions();
      response.addOutContext("validate-answer-confirm", 3);
    }
  }

  protected FinalStatus stats(String caseDataId, Session session) {
    int score = 0;
    int correctAnswers = 0;
    for (int i = 0; i < session.getAnswersResults().size(); i++) {
      if (session.getAnswersResults().get(i)) {
        Question question = getQuestion(caseDataId, i);
        score += question.getScore();
        correctAnswers++;
      }
    }
    return new FinalStatus(score, correctAnswers, session.getAnswersResults().size());
  }

  @SherlockAction(value = "answer-reject", requireCase = true, state = State.QUESTIONS)
  public void answerReject(SherlockRequest request, SherlockResponseBuilder response)
      throws IOException, MessageException {
    response.storyText(messages.message("answerReject"));
  }

  @SherlockAction(value = "skip-question", requireCase = true, state = State.QUESTIONS)
  public void skipQuestion(SherlockRequest request, SherlockResponseBuilder response)
      throws MessageException, IOException {
    doAddAnswer(request, response, "");
  }

  @SherlockAction(value = "answer-confirm", requireCase = true, state = State.QUESTIONS)
  public void answerConfirmed(SherlockRequest request, SherlockResponseBuilder response)
      throws MessageException, IOException {
    String answer = request.getParameter("answer");

    doAddAnswer(request, response, answer);
  }

  /**
   * Records player answer.
   * 
   * @param request
   * @param response
   * @param answer answer can be empty, which means no answer
   * @throws IOException
   * @throws MessageException
   */
  protected void doAddAnswer(SherlockRequest request, SherlockResponseBuilder response,
      String answer) throws IOException, MessageException {
    Session session = sessionManager.getSession();
    if (answer != null) {
      int answerIndex = session.getAnswers().size();
      int questionsSize = getQuestionsSize(request.getCaseDataId());
      if (answerIndex < questionsSize) {
        sessionManager.addAnswer(answer);
        session = sessionManager.getSession();
        if (answerIndex + 1 < questionsSize) {
          // we have more questions
          askQuestion(request.getCaseDataId(), response, answerIndex + 1, questionsSize);
        } else {
          // it is final solution time
          doFinalSolution(request, response, session);
        }
      }
    } else {
      logger.severe("got answer action without answer parameter, fallback to welcome");
      finishWelcome(request, response);
    }
  }

  /**
   * Showing final solition.
   */
  protected void doFinalSolution(SherlockRequest request, SherlockResponseBuilder response,
      Session session) throws IOException, MessageException {
    sessionManager.sessionState(State.ANSWERS);
    response.prestoryText(messages.message("finishPresolution"));
    Story story = actionsTool.getStory(request.getCaseDataId(), Story.FINAL_SOLUTION_ID);
    response.story(story);
    Question question = getQuestion(request.getCaseDataId(), 0);
    String playerAnswer = session.getAnswers().get(0);
    response.afterstoryText(messages.message("finishAftersolution",
        ImmutableMap.of("nextQuestion", question, "answer", playerAnswer)));
    if (!request.isCanvas()) {
      response.continueSuggestions();
    } else {
      // empty suggestions
      response.suggestions();
    }

    sessionManager.pushSessionLog(story, response);
  }

  @SherlockAction(value = "answer", requireCase = true, state = State.QUESTIONS)
  public void answer(SherlockRequest request, SherlockResponseBuilder response)
      throws MessageException, IOException {
    Session session = sessionManager.getSession();
    String answer = request.getParameter("answer");
    if (answer == null) {
      answer = request.getInput();
    }
    int answerIndex = session.getAnswers().size();
    Question question = getQuestion(request.getCaseDataId(), answerIndex);
    actionsTool.message(response, "finishAnswerConfirm",
        ImmutableMap.of("question", question, "answer", answer));
    response.confirmSuggestions();
    response.addParameter("answer", answer);
    response.addOutContext("answer-confirm");
  }

  @SherlockAction(value = "finish-confirm", requireCase = true)
  public void finishConfirm(SherlockRequest request, SherlockResponseBuilder response)
      throws MessageException, IOException {
    if (sessionManager.getSession().getState() != State.QUESTIONS) {
      sessionManager.sessionState(State.QUESTIONS);
      int questionsSize = getQuestionsSize(request.getCaseDataId());
      askQuestion(request.getCaseDataId(), response, 0, questionsSize);
    }
  }

  private void askQuestion(String caseDataId, SherlockResponseBuilder response, int questionIndex,
      int questionsSize) throws IOException, MessageException {
    Question question = getQuestion(caseDataId, questionIndex);
    if (questionIndex == 0) {
      response.prestoryText(messages.message("finishQuestionFirstPrompt"));
    }
    response.storyText(messages.message("finishQuestion", ImmutableMap.of("question", question,
        "questionIndex", questionIndex, "questionsSize", questionsSize)));
  }

  @SherlockAction(value = "finish", requireCase = true)
  public void finish(SherlockRequest request, SherlockResponseBuilder response)
      throws MessageException, IOException {
    if (sessionManager.getSession().getState() == State.CASE_STARTED) {
      actionsTool.message(response, "finishConfirm");
      response.confirmSuggestions();
    } else if (sessionManager.getSession().getState() == State.ANSWERS
        || sessionManager.getSession().getState() == State.FINISH) {
      actionsTool.finalSolution(request, response);
    } else {
      finishWelcome(request, response);
    }
  }

  @SherlockAction(value = "status", requireCase = true)
  public void status(SherlockRequest request, SherlockResponseBuilder response)
      throws IOException, MessageException {
    Session session = sessionManager.getSession();
    List<Story> visited = new ArrayList<>();
    for (String location : session.getLocationsBacklog()) {
      Story story = actionsTool.getStory(request.getCaseDataId(), location);
      if (story != null) {
        visited.add(story);
      }
    }
    logger.info("Our visited:" + visited);
    actionsTool.message(response, "status",
        ImmutableMap.of("backlog", visited, "size", visited.size()));
  }

  protected void finishWelcome(SherlockRequest request, SherlockResponseBuilder response)
      throws IOException, MessageException {
    Session session = sessionManager.getSession();
    if (session.getState() == State.QUESTIONS) {
      response.prestoryText(messages.message("welcomeQuestions"));
      askQuestion(request.getCaseDataId(), response, session.getAnswers().size(),
          getQuestionsSize(request.getCaseDataId()));
    } else if (session.getState() == State.ANSWERS) {
      int answerIndex = session.getAnswersResults().size();
      Question question = getQuestion(request.getCaseDataId(), answerIndex);
      String answer = session.getAnswers().get(answerIndex);
      actionsTool.message(response, "welcomeAnswers",
          ImmutableMap.of("nextQuestion", question, "answer", answer));
      response.confirmRejectSuggestions();
      response.addOutContext("validate-answer-confirm", 3);
    }
  }

  protected Question getQuestion(String caseDataId, int answerIndex) {
    return assetsManager.getQuestion(caseDataId, answerIndex);
  }

  protected int getQuestionsSize(String caseDataId) {
    return assetsManager.getQuestionsSize(caseDataId);
  }

  public void setActionsTool(ActionsTool actionsTool) {
    this.actionsTool = actionsTool;
  }
}
