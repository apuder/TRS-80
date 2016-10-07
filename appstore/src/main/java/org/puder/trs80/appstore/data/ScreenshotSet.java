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

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

import java.util.ArrayList;
import java.util.List;

/**
 * Screenshots are stored separately so that they can be loaded on demand.
 */
@Entity
@Cache
public class ScreenshotSet {
  public static class Screenshot {
    /**
     * Description of the screenshot, optional.
     */
    public String description;

    /**
     * Picture data, stores as a blob.
     */
    public byte[] data;
  }

  @Id
  public Long id;
  public List<Screenshot> screenshots = new ArrayList<>();
}
