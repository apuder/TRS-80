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

import java.util.HashMap;
import java.util.Map;

/**
 * Template Model.
 */
public class TemplateModel {
  private final Map<String, Object> data = new HashMap<>();

  /**
   * Set the given template value.
   */
  public void put(String key, Object value) {
    this.data.put(key, value);
  }

  /**
   * @return The internal data map to be passed to the JSP.
   */
  public Map<String, Object> getData() {
    return this.data;
  }
}
