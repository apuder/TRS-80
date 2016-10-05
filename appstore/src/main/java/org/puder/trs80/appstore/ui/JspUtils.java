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

import com.google.common.base.Preconditions;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * Utilities for serving HTML/JSP data.
 */
public class JspUtils {
  public static void writeResponse(ServletContext context,
                                   HttpServletRequest request,
                                   HttpServletResponse response,
                                   String path,
                                   TemplateModel model) throws ServletException, IOException {
    Preconditions.checkNotNull(context, "Context necessary");
    Preconditions.checkNotNull(request, "Request necessary");
    Preconditions.checkNotNull(response, "Response necessary");
    Preconditions.checkNotNull(path, "Path necessary");

    if (model == null) {
      model = new TemplateModel();
    }
    request.setAttribute("model", model.getData());
    context.getRequestDispatcher(path).forward(request, response);
  }
}
