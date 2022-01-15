package com.sherlockmysteries.pdata;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.text.WordUtils;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.mystery.assets.AssetsManager;
import com.google.mystery.assets.SearchManager;
import com.google.mystery.assets.StorageManager;
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
  @Inject private StorageManager storageManager;
  static Splitter SPLITTER = Splitter.on(" ").trimResults().omitEmptyStrings();

  /** Generate smdata package and upload it to storage. */
  public void generateAndUpload(String caseId, String bucketName, PrintWriter logWriter)
      throws IOException {
    String filename = "case_" + caseId.replaceAll("-", "_");
    File tempFile = File.createTempFile(filename, ".zip");
    try (FileOutputStream output = new FileOutputStream(tempFile)) {
      generateSMData(caseId, bucketName, logWriter, output);
    }
    try (FileInputStream input = new FileInputStream(tempFile)) {
      logWriter.println("Uploading case to storage");
      String url =
          storageManager.uploadToBucket(
              input, bucketName, String.format("cases/%s.zip", filename), "application/zip");
      logWriter.println("Case is uploaded to " + url);
    } finally {
      tempFile.delete();
    }
  }

  /** Generating smdata package. */
  protected void generateSMData(
      String caseId, String bucketName, PrintWriter logWriter, OutputStream out)
      throws IOException {
    try (ZipOutputStream zipOut = new ZipOutputStream(out)) {
      com.google.mystery.data.model.Case c = assetsManager.getCase(caseId);
      if (c == null) {
        logWriter.println("Can not find caseid=" + caseId);
      }

      // Case Data.
      CaseData.Builder caseData = CaseData.newBuilder().setCaseid(caseId).setName(c.getName());
      if (c.getCategory() != null) {
        caseData.setDescription(c.getCategory());
      }
      if (c.getAuthor() != null) {
        caseData.setAuthor(c.getAuthor());
      }
      if (c.getVoiceActor() != null) {
        caseData.setVoiceActor(c.getVoiceActor());
      }
      if (c.getIllustrationArtist() != null) {
        caseData.setIllustrationArtist(c.getIllustrationArtist());
      }
      caseData.setMapType(c.getMapType());

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

      // Writing Directory Data.
      zipOut.putNextEntry(new ZipEntry("directory-entries.pbdata"));
      Map<String, DirectoryCategoryData.Builder> categories = new HashMap<>();
      List<DirectoryEntry> entries = searchManager.getAllEntries(c.getCaseDataId());
      Set<String> uniqueNames = new HashSet<>();

      logWriter.println("Number Entries: " + entries.size());
      for (DirectoryEntry entry : entries) {
        // Ensure we do not add the same name twice.
        if (uniqueNames.contains(entry.getName().toLowerCase())) {
          continue;
        }
        uniqueNames.add(entry.getName().toLowerCase());

        DirectoryEntryData.Builder directoryEntry =
            DirectoryEntryData.newBuilder()
                .setLocation(entry.getLocation())
                .setName(entry.getName().trim());
        if (!Strings.isNullOrEmpty(entry.getKeywords())) {
          directoryEntry.addAllKeywords(SPLITTER.split(entry.getKeywords()));
        }
        if (Strings.isNullOrEmpty(entry.getCategory())
            || entry.getCategory().startsWith("Person")) {
          directoryEntry.setName(personName(directoryEntry.getName()));
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
                .addAllPossibleAnswers(question.getPossibleAnswers())
                .setScore(question.getScore())
                .setOrder(question.getOrder())
                .setOptional(question.getScore() <= 10);
        questionData.build().writeDelimitedTo(zipOut);
      }

      // Writing Hints.
      zipOut.putNextEntry(new ZipEntry("hints.pbdata"));
      for (Hint hint : assetsManager.getAllHints(c.getCaseDataId())) {
        HintData.Builder hintData = HintData.newBuilder().setText(hint.getHint());
        hintData.addAllRequiredLocations(hint.getPrecondition());
        hintData.build().writeDelimitedTo(zipOut);
      }

      // Searching for newspaper image.
      findAndInsertNewspaper(bucketName, caseId, imagesOrAudioToStore, logWriter);

      // Writing Images.
      for (Entry<URL, String> imageEntry : imagesOrAudioToStore.entrySet()) {
        writeImageOrAudio(imageEntry.getKey(), imageEntry.getValue(), zipOut, logWriter);
      }
    }
  }

  protected void findAndInsertNewspaper(
      String bucketName, String caseId, Map<URL, String> imagesOrAudio, PrintWriter logWriter) {
    String newspaperUrl = storageManager.getStoryImageUrl(bucketName, caseId, "newspaper");
    if (newspaperUrl != null) {
      try {
        imagesOrAudio.put(new URL(newspaperUrl), "newspaper");
      } catch (MalformedURLException e) {
        e.printStackTrace(logWriter);
      }
    } else {
      logWriter.println("WARN: Newspaper image not found.");
    }
  }

  protected void writeImageOrAudio(
      URL image, String name, ZipOutputStream zipOut, PrintWriter logWriter) throws IOException {
    String newName;
    if (image.getPath().endsWith(".png")) {
      newName = "images/" + name + ".png";
    } else if (image.getPath().endsWith(".jpg") || image.getPath().endsWith(".jpeg")) {
      newName = "images/" + name + ".jpg";
    } else if (image.getPath().endsWith(".mp3")) {
      newName = "audio/" + name + ".mp3";
    } else {
      throw new IOException("Expecting .jpg, .png or .mp3 extensiong, got " + image.getPath());
    }
    newName = newName.toLowerCase();

    logWriter.println("Writing: " + newName + " from " + image.toString());
    zipOut.putNextEntry(new ZipEntry(newName));
    storageManager.downloadFromBucket(image.getPath(), zipOut);
  }

  public static String personName(String name) {
    List<String> splitted = Lists.newArrayList(Splitter.on(" ").trimResults().split(name));
    if (splitted.size() > 1) {
      int last = splitted.size() - 1;
      return splitted.get(last) + ", " + Joiner.on(" ").join(splitted.subList(0, last));
    }
    return name;
  }
}
