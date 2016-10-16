package org.puder.trs80.appstore;

import com.google.common.io.ByteStreams;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Class that deals with finding ans erving graphics resources.
 */
class GfxServingUtil {
  boolean serve(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String requestUri = req.getRequestURI();
    if (requestUri == null || !req.getRequestURI().startsWith("/gfx")) {
      return false;
    }
    try {
      InputStream fileStream = new FileInputStream(new File("WEB-INF" + requestUri));
      if (requestUri.toLowerCase().endsWith(".png")) {
        resp.setContentType("image/png");
      }
      ByteStreams.copy(fileStream, resp.getOutputStream());
    } catch (FileNotFoundException ex) {
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
    return true;
  }
}