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

import org.puder.trs80.appstore.data.AppStoreItem.CharacterColor;
import org.puder.trs80.appstore.data.AppStoreItem.KeyboardLayout;
import org.puder.trs80.appstore.data.AppStoreItem.Model;
import org.puder.trs80.appstore.ui.Template;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Fills UI data ad eceives requests from the UI for item data.
 */
public class ItemsViewUtil {
  private static final Logger LOG = Logger.getLogger("ItemsViewUtil");

  // TODO: Cache.
  public Template fillNewItemView() throws IOException {
    return Template.fromFile("WEB-INF/html/item/new_item.inc.html")
        .withHtml("model_options", genOptionsHtml(Model.values(), "model"))
        .withHtml("land_keyboard_options", genOptionsHtml(KeyboardLayout.values(), "kb_land"))
        .withHtml("port_keyboard_options", genOptionsHtml(KeyboardLayout.values(), "kb_port"))
        .withHtml("character_color", genOptionsHtml(CharacterColor.values(), "char_color"));
  }

  private String genOptionsHtml(Enum<?>[] values, String name) throws IOException {
    StringBuilder modelOptionsHtml = new StringBuilder();
    for (Enum<?> value : values) {
      modelOptionsHtml.append(
          Template.fromFile("WEB-INF/html/radio_option.inc.html")
              .with("name", name)
              .with("id", name + "_" + value.ordinal())
              .with("value", value.name())
              .with("label", value.toString())
              .with("checked", value.ordinal() == 0 ? "checked" : "")
              .render());
    }
    return modelOptionsHtml.toString();
  }
}
