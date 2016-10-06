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
import com.google.common.base.Optional;
import org.puder.trs80.appstore.data.Trs80User;
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

  private static final String REQUEST_REMOVE_USER = "removeUser";
  private static final String REQUEST_ADD_EDIT_USER = "addEditUser";
  private static final String REQUEST_CREATE_ACCOUNT = "createAccount";

  private static UserService sUserService = UserServiceFactory.getUserService();
  private static UserManagement sUserManagement = new UserManagement(sUserService);
  private static UserViewUtil sUserViewUtil = new UserViewUtil(sUserManagement);

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    serveMainHtml(req, resp);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String requestParam = req.getParameter("request");

    // The following actions are only allowed for admin users.
    if (sUserManagement.isCurrentUserAdmin()) {
      if (REQUEST_ADD_EDIT_USER.equals(requestParam)) {
        sUserViewUtil.handleAddEditRequest(req, resp);
      } else if (REQUEST_CREATE_ACCOUNT.equals(requestParam)) {
        sUserViewUtil.handleAccountCreateRequest(req, resp);
      } else if (REQUEST_REMOVE_USER.equals(requestParam)) {
        sUserViewUtil.handleRemoveRequest(req, resp);
      }
    }
    serveMainHtml(req, resp);
  }

  private void serveMainHtml(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    resp.setContentType("text/html");

    String thisUrl = req.getRequestURI();

    // If the user is not logged in, forward to login page.
    if (req.getUserPrincipal() == null) {
      String html = Template.fromFile("WEB-INF/html/login_forward.html")
          .with("forwarding_url", sUserService.createLoginURL(thisUrl))
          .render();
      resp.getWriter().write(html);
      return;
    }
    LOG.info("Logout URL: " + sUserService.createLogoutURL(thisUrl));
    Optional<String> loggedInEmail = sUserManagement.getLoggedInEmail();

    // User needs to create an account first.
    if (!sUserManagement.getCurrentUser().isPresent()) {
      String content = Template.fromFile("WEB-INF/html/create_account.html")
          .with("logged_in_email", loggedInEmail.get())
          .render();
      resp.getWriter().write(content);
      return;
    }

    if ("/".equals(req.getRequestURI())) {
      Template userManagementTpl = sUserManagement.isCurrentUserAdmin() ?
          sUserViewUtil.fillUserManagementView(sUserManagement) : Template.empty();
      String content = Template.fromFile("WEB-INF/html/index.html")
          .withHtml("user_management_content", userManagementTpl.render())
          .withHtml("logged_in_email", loggedInEmail.get())
          .render();
      resp.getWriter().write(content);
    }
  }
}
