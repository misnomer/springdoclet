package org.springdoclet.rest.writers

import org.springdoclet.Configuration

class RestStylesheetWriter {

  def writeStylesheet(Configuration config) {
    writeResource("bootstrap.css", new File(config.outputDirectory, "bootstrap.css"))
    writeResource("styles.css", new File(config.outputDirectory, config.styleSheet))
  }

  private void writeResource(String name, File file) {
    if (file.exists()) file.delete()
    InputStream is = getClass().getResourceAsStream(name)
    try {
      file << is
    } finally {
      try { is.close() } catch (ignore) {}
    }
  }
}
