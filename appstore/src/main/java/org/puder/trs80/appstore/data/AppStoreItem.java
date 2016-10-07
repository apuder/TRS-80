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

/**
 * An app store item (TPK) such as a game or application.
 */
@Entity
@Cache
public class AppStoreItem {
  public enum Model {
    Model_I, Model_III, Model_4, MMODEL_4P
  }

  public enum KeyboardLayout {
    ORIGINAL, COMPACT, JOYSTICK, GAME, TILT
  }

  public enum CharacterColor {
    GREEN, WHITE
  }

  // E.g. Casette or disk image.
  public static class MediaImage {
    public String type;
    // Will become a GAE blob structure.
    public byte[] data;
  }

  public enum ListingCategory {
    GAME, OFFICE, OTHER
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
