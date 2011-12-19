package org.springdoclet.rest.writers

import groovy.xml.MarkupBuilder
import org.springdoclet.Configuration
import org.springdoclet.rest.RestCollector
import org.springdoclet.rest.RestPathBuilder
import org.springdoclet.rest.ExternalDocumentation

class RestHtmlWriter {
  void writeOutput(List collectors, Configuration config) {
    List<ExternalDocumentation> overviewFiles = []
    if (config.getOverviewDirectory()) {
      File overviewDir = config.getOverviewDirectory() as File
      if (! overviewDir.isDirectory())
        throw new IllegalArgumentException("Overview Directory ${overviewDir.canonicalPath} does not exist")
      for (File file in overviewDir.listFiles().sort { it.name }) {
        if (file.isFile() && file.name.endsWith(".html")) {
          println "Adding overview file ${file}"
          overviewFiles << new ExternalDocumentation(file)
        }
      }
    }

    RestPathBuilder paths = new RestPathBuilder(
            baseDirectory: config.outputDirectory as File,
            collectors: collectors,
            config: config,
            overviewFiles: overviewFiles
    )

    for (doc in overviewFiles) {
      paths.template(paths.fileForOverview(doc.name), doc.title, config) {MarkupBuilder builder ->
        builder.mkp.yieldUnescaped doc.text
      }
    }

    File outputFile = new File(config.outputDirectory, config.outputFileName)

    paths.template(outputFile, config.title, config) {MarkupBuilder builder ->
      builder.div {
        for (RestCollector collector in collectors) {
          collector.writeOutput builder, paths, config
        }
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
