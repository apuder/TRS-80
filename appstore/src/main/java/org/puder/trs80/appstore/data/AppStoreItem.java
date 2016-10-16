/*
 * Copyright 2016, Sascha Häberling
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.puder.trs80.appstore.data;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An app store item (TPK) such as a game or application.
 */
@Entity
@Cache
public class AppStoreItem {
  public enum Model {
    MODEL_I("Model I"),
    MODEL_III("Model III"),
    MODEL_4("Model 4"),
    MODEL_4P("Model 4P");

    private final String readableName;

    Model(String readableName) {
      this.readableName = checkNotNull(readableName);
    }

    @Override
    public String toString() {
      return readableName;
    }
  }

  public enum KeyboardLayout {
    ORIGINAL("Original"),
    COMPACT("Compact"),
    JOYSTICK("Joystick"),
    GAME("Game"),
    TILT("Tilt");

    private final String readableName;

    KeyboardLayout(String readableName) {
      this.readableName = readableName;
    }

    @Override
    public String toString() {
      return readableName;
    }

  }

  public enum CharacterColor {
    GREEN("Green"), WHITE("White");

    private final String readableName;

    CharacterColor(String readableName) {
      this.readableName = readableName;
    }

    @Override
    public String toString() {
      return readableName;
    }
  }

  // E.g. Casette or disk image.
  public static class MediaImage {
    public String type;
    // Will become a GAE blob structure.
    public byte[] data;
  }

  public enum ListingCategory {
    GAME("Game"),
    OFFICE("Office"),
    OTHER("Other");

    private final String readableName;

    ListingCategory(String readableName) {
      this.readableName = readableName;
    }

    @Override
    public String toString() {
      return readableName;
    }

  }

  /**
   * Configuration data.
   */
  public static class Configuration {
    public Model model;

    // Disk 1-4 + casette (type/extension + data)
    public MediaImage disk1;
    public MediaImage disk2;
    public MediaImage disk3;
    public MediaImage disk4;
    public MediaImage casette;

    boolean soundMuted;
    public KeyboardLayout layoutLandscape;
    public KeyboardLayout layoutPortrait;
    public CharacterColor characterColor;
  }

  /**
   * Listing data.
   */
  public static class Listing {
    public String name;
    public String versionString;
    public int version;
    public Set<ListingCategory> categories = new HashSet<>();
    public String licenseName;
    public String licenseUrl;
    public int uploadTime;
    public int publishTime;
    public int lastUpdateTime;
    public int authorId;
    public String publisherEmail;
  }

  AppStoreItem() {
  }

  @Id
  public Long id;
  public Configuration configuration;
  public Listing listing;
  @Load
  Ref<ScreenshotSet> screenshots;

}
