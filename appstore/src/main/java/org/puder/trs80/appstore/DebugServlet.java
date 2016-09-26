package org.puder.trs80.appstore;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

public class DebugServlet extends HttpServlet {
  private static final Logger LOG = Logger.getLogger("MainServlet");

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
        IOException {
    resp.setContentType("text/html");
    resp.getWriter().println("<h1>Debug area</h1>\nWelcome");
    resp.getWriter().close();
  }

}
