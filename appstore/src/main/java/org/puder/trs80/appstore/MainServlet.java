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

package org.puder.trs80.appstore;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import org.puder.trs80.appstore.data.UserManagement;
import org.puder.trs80.appstore.data.UserViewUtil;
import org.puder.trs80.appstore.ui.Template;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Enables adding/removing of users.
 */
public class MainServlet extends Trs80Servlet {
  private static final Logger LOG = Logger.getLogger("MainServlet");

  private static final String REQUEST_ADD_EDIT_USER = "addEditUser";

  private static UserManagement sUserManagement = new UserManagement();
  private static UserViewUtil sUserViewUtil = new UserViewUtil(sUserManagement);
  private static String sMainPageCache;

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    serveMainHtml(req, resp);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    if (REQUEST_ADD_EDIT_USER.equals(req.getParameter("request"))) {
      sUserViewUtil.handleAddEditRequest(req, resp);
    }

    serveMainHtml(req, resp);
  }

  private void serveMainHtml(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    UserManagement userManagement = new UserManagement();

    UserService userService = UserServiceFactory.getUserService();
    User currentUser = userService.getCurrentUser();

    if ("/".equals(req.getRequestURI())) {
      Template userManagementTpl = sUserViewUtil.fillUserManagementView(userManagement);
      String content = Template.fromFile("WEB-INF/html/index.html")
          .withHtml("user_management_content", userManagementTpl.render())
          .render();
      resp.getWriter().write(content);
    }
  }
}
