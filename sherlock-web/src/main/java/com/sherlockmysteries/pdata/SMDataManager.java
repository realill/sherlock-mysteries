package com.sherlockmysteries.pdata;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.text.WordUtils;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.mystery.assets.AssetsManager;
import com.google.mystery.assets.SearchManager;
import com.google.mystery.data.model.Clue;
import com.google.mystery.data.model.DirectoryEntry;
import com.google.mystery.data.model.Hint;
import com.google.mystery.data.model.Question;
import com.google.mystery.data.model.Story;
import com.sherlockmysteries.pdata.Case.CaseData;
import com.sherlockmysteries.pdata.Chapter.ChapterData;
import com.sherlockmysteries.pdata.Chapter.ClueData;
import com.sherlockmysteries.pdata.Directory.DirectoryCategoryData;
import com.sherlockmysteries.pdata.Directory.DirectoryEntryData;
import com.sherlockmysteries.pdata.Hint.HintData;
import com.sherlockmysteries.pdata.Newspaper.ArticleData;
import com.sherlockmysteries.pdata.Question.QuestionData;

@Singleton
public class SMDataManager {
  @Inject private AssetsManager assetsManager;
  @Inject private SearchManager searchManager;
  static Splitter SPLITTER = Splitter.on(" ").trimResults().omitEmptyStrings();

  public void generateSMData(String caseId, PrintWriter logWriter, OutputStream out)
      throws IOException {
    try (ZipOutputStream zipOut = new ZipOutputStream(out)) {
      com.google.mystery.data.model.Case c = assetsManager.getCase(caseId);
      if (c == null) {
        logWriter.println("Can not find caseid=" + caseId);
      }
      CaseData.Builder caseData = CaseData.newBuilder().setCaseid(caseId).setName(c.getName());
      caseData.addAllStreets(
          assetsManager.getStreets().stream().sorted().collect(Collectors.toList()));
      caseData.addAllLocations(
          assetsManager.getLocations().stream().sorted().collect(Collectors.toList()));

      // Writing case data.
      ZipEntry caseEntry = new ZipEntry("case.pbdata");
      zipOut.putNextEntry(caseEntry);
      caseData.build().writeTo(zipOut);
      logWriter.println("case debug:" + caseData.getCaseid());

      if (c.getImageUrl() != null) {
        writeImageOrAudio(c.getImageUrl(), "case-thumbnail", zipOut, logWriter);
      }

      Map<URL, String> imagesOrAudioToStore = new HashMap<>();
      // Writing Chapters data.
      zipOut.putNextEntry(new ZipEntry("chapters.pbdata"));
      for (Story story : assetsManager.getAllStories(c.getCaseDataId())) {
        if (!Story.TIMES_ARTICLE.equals(story.getType())) {
          ChapterData.Builder storyData =
              ChapterData.newBuilder()
                  .setId(story.getId())
                  .setName(story.getTitle())
                  .setText(story.getText());
          if (!Strings.isNullOrEmpty(story.getLatlong())) {
            storyData.setLatlong(story.getLatlong());
          }
          if (story.getImageUrl() != null) {
            imagesOrAudioToStore.put(story.getImageUrl(), story.getId());
          }
          if (story.getAudioUrl() != null) {
            imagesOrAudioToStore.put(story.getAudioUrl(), story.getId());
          }

          for (String clueid : story.getClues()) {
            Clue clue = assetsManager.getClue(c.getCaseDataId(), clueid);
            if (clue == null) {
              throw new IOException(
                  "Can not find clueid=" + clueid + " for storyid=" + story.getId());
            }
            ClueData.Builder clueData =
                ClueData.newBuilder()
                    .setClueid(clueid)
                    .setName(clue.getName())
                    .setText(clue.getDescription());
            if (clue.getImageUrl() != null) {
              imagesOrAudioToStore.put(clue.getImageUrl(), clue.getId());
            }

            storyData.addClues(clueData);
          }
          logWriter.println("Storing chapter: " + story.getId());
          storyData.build().writeDelimitedTo(zipOut);
        }
      }

      // Writing Articles data.
      zipOut.putNextEntry(new ZipEntry("articles.pbdata"));
      int order = 0;
      for (Story article : assetsManager.getAllAricles(c.getCaseDataId())) {
        ArticleData.Builder articleData =
            ArticleData.newBuilder()
                .setName(article.getTitle())
                .setText(article.getText())
                .setOrder(order++);
        articleData.build().writeDelimitedTo(zipOut);
      }

      // Writing Directory Data
      zipOut.putNextEntry(new ZipEntry("directory-entries.pbdata"));
      Map<String, DirectoryCategoryData.Builder> categories = new HashMap<>();
      for (DirectoryEntry entry : searchManager.getAllEntries(c.getCaseDataId())) {
        DirectoryEntryData.Builder directoryEntry =
            DirectoryEntryData.newBuilder()
                .setLocation(entry.getLocation())
                .setName(entry.getName().trim());
        if (!Strings.isNullOrEmpty(entry.getKeywords())) {
          directoryEntry.addAllKeywords(SPLITTER.split(entry.getKeywords()));
        }
        if (Strings.isNullOrEmpty(entry.getCategory())
            || entry.getCategory().startsWith("Person")) {
          logWriter.println("Entry: " + directoryEntry.toString());
          directoryEntry.build().writeDelimitedTo(zipOut);
        } else {
          String[] splittedCategory = entry.getCategory().split(",");
          String categoryName = splittedCategory[0].trim();
          categoryName = WordUtils.capitalize(categoryName);

          DirectoryCategoryData.Builder categoryData = categories.get(categoryName);
          if (categoryData == null) {
            categoryData = DirectoryCategoryData.newBuilder().setName(categoryName);
            String keywords = null;
            if (splittedCategory.length > 1) {
              keywords = splittedCategory[1];
            }
            if (keywords != null) {
              categoryData.addAllKeywords(SPLITTER.split(keywords));
            }
            categories.put(categoryName, categoryData);
          }
          categoryData.addEntries(directoryEntry);
        }
      }

      // Writing Categories.
      zipOut.putNextEntry(new ZipEntry("directory-categories.pbdata"));
      for (DirectoryCategoryData.Builder category : categories.values()) {
        logWriter.println("Category: " + category.toString());
        category.build().writeDelimitedTo(zipOut);
      }

      // Writing Questions.
      zipOut.putNextEntry(new ZipEntry("questions.pbdata"));
      for (int i = 0; i < assetsManager.getQuestionsSize(c.getCaseDataId()); i++) {
        Question question = assetsManager.getQuestion(c.getCaseDataId(), i);
        QuestionData.Builder questionData =
            QuestionData.newBuilder()
                .setQuestion(question.getQuestion())
                .setAnswer(question.getAnswer())
                .setScore(question.getScore())
                .setOrder(question.getOrder())
                .setOptionalQuestion(question.getOrder() <= 50);
        questionData.addPossibleAnswers("1");
        questionData.addPossibleAnswers("2");
        questionData.addPossibleAnswers("3");
        logWriter.println("Question: " + questionData.toString());
        questionData.build().writeDelimitedTo(zipOut);
      }

      // Writing Hints.
      zipOut.putNextEntry(new ZipEntry("hints.pbdata"));
      for (Hint hint : assetsManager.getAllHints(c.getCaseDataId())) {
        HintData.Builder hintData = HintData.newBuilder().setText(hint.getHint());
        hintData.addAllRequiredLocations(hint.getPrecondition());
        logWriter.println("Hint: " + hintData.toString());
        hintData.build().writeDelimitedTo(zipOut);
      }

      //       Writing Images.
      for (Entry<URL, String> imageEntry : imagesOrAudioToStore.entrySet()) {
        writeImageOrAudio(imageEntry.getKey(), imageEntry.getValue(), zipOut, logWriter);
      }
    }
  }

  protected void writeImageOrAudio(
      URL image, String name, ZipOutputStream zipOut, PrintWriter logWriter) throws IOException {
    String newName;
    if (image.getPath().endsWith(".png")) {
      newName = "images/" + name + ".png";
    } else if (image.getPath().endsWith(".jpg")) {
      newName = "images/" + name + ".jpg";
    } else if (image.getPath().endsWith(".mp3")) {
      newName = "audio/" + name + ".mp3";
    } else {
      throw new IOException("Expecting .jpg, .png or .mp3 extensiong, got " + image.getPath());
    }
    newName = newName.toLowerCase();

    logWriter.println("Writing: " + newName + " from " + image.toString());
    zipOut.putNextEntry(new ZipEntry(newName));
    try (BufferedInputStream inputStream = new BufferedInputStream(image.openStream())) {
      byte data[] = new byte[1024 * 64];
      int byteContent;
      while ((byteContent = inputStream.read(data, 0, 1024)) != -1) {
        zipOut.write(data, 0, byteContent);
      }
    }
  }
}
