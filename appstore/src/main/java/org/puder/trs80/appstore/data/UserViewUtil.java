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

import com.google.common.base.Optional;
import org.puder.trs80.appstore.ui.Template;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Fills UI data and receives request from the UI to change data.
 */
public class UserViewUtil {
  private static final Logger LOG = Logger.getLogger("UserViewUtil");

  private final UserManagement userManagement;

  public UserViewUtil(UserManagement userManagement) {
    this.userManagement = userManagement;
  }


  public void handleAddEditRequest(HttpServletRequest req, HttpServletResponse resp) {
    String userIdStr = req.getParameter("userId");
    String firstName = req.getParameter("firstName");
    String lastName = req.getParameter("lastName");
    String email = req.getParameter("email");
    String type = req.getParameter("type");
    LOG.info("userId: " + userIdStr);
    LOG.info("firstName: " + firstName);
    LOG.info("lastName: " + lastName);
    LOG.info("email: " + email);
    LOG.info("type: " + type);

    Trs80User trs80User = new Trs80User();
    // If a user ID is given it means we change an existing user.
    if (userIdStr != null && !userIdStr.isEmpty()) {
      long userId;
      try {
        userId = Long.parseLong(userIdStr);
      } catch (NumberFormatException ex) {
        LOG.warning("ID is not valid: '" + userIdStr + "'.");
        return;
      }
      Optional<Trs80User> user = userManagement.getUserById(userId);
      if (user.isPresent()) {
        LOG.info("Editing existing user");
        trs80User = user.get();
      }
    }

    trs80User.firstName = firstName;
    trs80User.lastName = lastName;
    trs80User.email = email;
    trs80User.type = "admin".equals(type) ? Trs80User.AccountType.ADMIN : Trs80User.AccountType.PUBLISHER;
    userManagement.addOrChangeUser(trs80User);
  }

  public Template fillUserManagementView(UserManagement userManagement) throws IOException {
    List<Trs80User> users = userManagement.getAllUsers();
    StringBuilder builder = new StringBuilder();

    for (Trs80User user : users) {
      builder.append(Template.fromFile("WEB-INF/html/user/user_table_row.inc.html")
          .with("user_id", user.id)
          .with("first_name", user.firstName)
          .with("last_name", user.lastName)
          .with("e_mail", user.email)
          .with("role", user.type.name())
          .render());
    }
    return Template.fromFile("WEB-INF/html/user/manage_users.inc.html")
        .withHtml("user_rows", builder.toString());
  }
}
