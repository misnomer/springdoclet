package org.springdoclet.writers

import groovy.xml.MarkupBuilder
import org.springdoclet.Configuration
import org.springdoclet.PathBuilder

class HtmlWriter {
  void writeOutput(List collectors, Configuration config) {
    File outputFile = getOutputFile(config.outputDirectory, config.outputFileName)
    PathBuilder paths = new PathBuilder(config.linkPath)

    def builder = new MarkupBuilder(new FileWriter(outputFile))
    builder.html {
      head {
        title config.title
        link(rel: 'stylesheet', type: 'text/css', href: config.styleSheet)
      }
      body {
        h1 config.title
        hr()
        for (collector in collectors) {
          collector.writeOutput builder, paths
          hr()
        }
        mkp.yieldUnescaped config.footer
      }
    }
  }

  private File getOutputFile(String outputDirectory, String outputFileName) {
    File path = new File(outputDirectory)
    if (!path.exists())
      path.mkdirs()

    def file = new File(path, outputFileName)
    file.delete()
    file.createNewFile()

    return file
  }
}
