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

import java.util.List;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Functionality to manage users.
 */
public class UserManagement {

  /**
   * @return Whether an admin exists in the system.
   */
  public boolean hasAdmin() {
    List<Trs80User> users = ofy().load().type(Trs80User.class).list();
    for (Trs80User user : users) {
      if (user.type == Trs80User.AccountType.ADMIN) {
        return true;
      }
    }
    return false;
  }

  /**
   * Adds a new user if one with the given ID does not exist yet, otherwise changes the existing user with the given ID.
   */
  public void addOrChangeUser(Trs80User user) {
    ofy().save().entity(user).now();
  }

  /**
   * Remote the user with the given ID.
   *
   * @param user the user to delete.
   */
  public void removeUser(Trs80User user) {
    ofy().delete().entities(user).now();
  }

  /**
   * @return A list of all users in the system.
   */
  public List<Trs80User> getAllUsers() {
    return ofy().load().type(Trs80User.class).list();
  }

  public Optional<Trs80User> getUserById(long id) {
    return Optional.fromNullable(ofy().load().type(Trs80User.class).id(id).now());
  }
}
