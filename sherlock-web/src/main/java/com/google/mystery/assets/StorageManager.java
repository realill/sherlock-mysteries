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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

import javax.inject.Singleton;

import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.AudioEncoding;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import com.google.protobuf.ByteString;

@Singleton
public class StorageManager {
  private static Logger logger = Logger.getLogger(StorageManager.class.getSimpleName());
  private Storage storageInstance = StorageOptions.newBuilder().build().getService();

  public StorageManager() {}

  /**
   * Generating new audio out of given ssml.
   *
   * @param caseId case to generate audio for.
   * @param id id of audio file
   * @param ssml ssml to generate tts from.
   */
  public String generateTTSAudio(
      String bucketName, String caseId, String id, String ssml, PrintWriter writer)
      throws IOException {
    try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create()) {
      SynthesisInput input = SynthesisInput.newBuilder().setSsml(ssml).build();
      // Build the voice request
      VoiceSelectionParams voice =
          VoiceSelectionParams.newBuilder()
              .setLanguageCode("en-US")
              .setName("en-US-Wavenet-D")
              .build();
      AudioConfig audioConfig =
          AudioConfig.newBuilder().setAudioEncoding(AudioEncoding.MP3).setPitch(-4).build();
      SynthesizeSpeechResponse response =
          textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);
      writer.println(response.getAudioContent().toString());

      return uploadMp3ToBucket(bucketName, response.getAudioContent(), audioPath(caseId, id));
    }
  }

  public String getStoryImageUrl(String bucketName, String caseId, String id) {
    // try jpg and png.
    String jpg = getStoryImageUrl(bucketName, caseId, id, "jpg");
    if (jpg != null) {
      return jpg;
    }
    return getStoryImageUrl(bucketName, caseId, id, "png");
  }

  private String getStoryImageUrl(String bucketName, String caseId, String id, String ext) {
    id = AssetsManager.normalizeLocation(id);
    String path = String.format("images/%s/%s.%s", caseId, id, ext);
    Storage storage = storage();
    if (storage != null) {
      BlobId blobId = BlobId.of(bucketName, path);
      try {
        Blob blob = storage.get(blobId);
        return blob == null ? null : blobUrl(blob);
      } catch (StorageException ex) {
        storageInstance = null;
        logger.warning("No access to storage");
      }
    }

    return null;
  }

  /** @return story media link or <code>null</code> if does not exist in storage */
  public String getStoryMediaLink(String bucketName, String caseId, String storyName) {
    storyName = AssetsManager.normalizeLocation(storyName);
    Storage storage = storage();
    if (storage != null) {
      String path = audioPath(caseId, storyName);
      BlobId blobId = BlobId.of(bucketName, path);
      try {
        Blob blob = storage.get(blobId);

        return blob == null ? null : blobUrl(blob);
      } catch (StorageException ex) {
        storageInstance = null;
        logger.warning("No access to storage");
      }
    }

    return null;
  }

  private String blobUrl(BlobInfo blob) {
    return "https://storage.cloud.google.com/" + blob.getBucket() + "/" + blob.getName();
  }

  protected String audioPath(String caseId, String storyName) {
    return String.format("audio/%s/%s.mp3", caseId, storyName);
  }

  protected String uploadMp3ToBucket(String bucketName, ByteString in, String name) {
    name = AssetsManager.normalizeLocation(name);
    Storage storage = storage();
    BlobInfo blobInfo =
        storage.create(
            BlobInfo.newBuilder(bucketName, name).setContentType("audio/mp3").build(),
            in.toByteArray());
    // return the public download link
    return blobUrl(blobInfo);
  }

  public String uploadToBucket(
      InputStream input, String bucketName, String name, String contentType) throws IOException {
    name = AssetsManager.normalizeLocation(name);
    Storage storage = storage();
    @SuppressWarnings("deprecation")
    BlobInfo blobInfo =
        storage.create(
            BlobInfo.newBuilder(bucketName, name).setContentType(contentType).build(), input);
    // return the public download link
    return blobUrl(blobInfo);
  }

  public void downloadFromBucket(String path, OutputStream out) throws IOException {
    Storage storage = storage();

    String bucketName = path.split("/")[1];

    Blob blob = storage.get(BlobId.of(bucketName, path.substring(bucketName.length() + 2)));

    ReadChannel reader = blob.reader();

    ByteBuffer bytes = ByteBuffer.allocate(2048);
    while (reader.read(bytes) > 0) {
      bytes.flip();
      out.write(bytes.array());
      bytes.clear();
    }
  }

  private Storage storage() {
    return storageInstance;
  }
}
