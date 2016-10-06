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

package org.puder.trs80.appstore.ui;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.html.HtmlEscapers;
import com.google.common.io.CharStreams;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple string/replace based templating mechanism.
 */
public class Template {
  private static final String KEY_FORMAT = "<!--TRS %s TRS-->";
  private final String content;
  private final Map<String, String> values = new HashMap<>();

  private Template(String content) {
    this.content = content;
  }

  public static Template empty() {
    return new Template("");
  }

  public static Template fromFile(String filename) throws IOException {
    // TODO: need to cache file contents.
    InputStream fileStream = new FileInputStream(new File(filename));
    Preconditions.checkNotNull(fileStream, "Cannot load file " + filename);
    return new Template(CharStreams.toString(new InputStreamReader(fileStream, Charsets.UTF_8)));
  }

  public Template with(String key, String value) {
    // Escape by default to prevent injection attacks.
    value = HtmlEscapers.htmlEscaper().escape(value);
    values.put(key, value != null ? value : "");
    return this;
  }

  public Template withHtml(String key, String value) {
    values.put(key, value != null ? value : "");
    return this;
  }

  public Template with(String key, long value) {
    values.put(key, String.valueOf(value));
    return this;
  }

  public String render() {
    String result = content;
    for (Map.Entry<String, String> entry : values.entrySet()) {
      result = result.replace(String.format(KEY_FORMAT, entry.getKey()), entry.getValue());
    }
    return result;
  }
}