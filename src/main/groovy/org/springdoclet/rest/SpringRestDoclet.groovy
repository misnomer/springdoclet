package org.springdoclet.rest

import com.sun.javadoc.Doclet
import com.sun.javadoc.LanguageVersion
import com.sun.javadoc.RootDoc
import org.springdoclet.ClassProcessor
import org.springdoclet.Configuration
import org.springdoclet.ErrorReporter
import org.springdoclet.rest.collectors.RestControllerCollector
import org.springdoclet.rest.collectors.RestSchemaCollector
import org.springdoclet.rest.writers.RestHtmlWriter
import org.springdoclet.rest.writers.RestStylesheetWriter

/**
 * More comprehensive documentation for REST specific APIs that use JAXB for marshalling
 * content.  Produces two pieces of documentation- the first for controllers, and the
 * second for schema objects.  Packages are flattened for both.
 * <p>
 * This doclet accepts an -overview-directory argument, which may be used to specify a directory
 * containing generic HTML files.  These files will be wrapped in our navigation template and included
 * within the generated documentation.  These files should be wrapped in a single &lt;body&gt; root tag.  The title
 * may be specified as a <code>title</code> attribute on that root body tag.
 */
class SpringRestDoclet extends Doclet {
  private static Configuration config = new Configuration()

  public static boolean start(RootDoc root) {
    ErrorReporter.setErrorReporter(root)
    config.options = root.options()

    def collectors = getCollectors()

    new ClassProcessor().process root.classes(), collectors

    new RestHtmlWriter().writeOutput collectors, config

    if (config.isDefaultStyleSheet()) {
      new RestStylesheetWriter().writeStylesheet config
    }

    return true
  }

  private static getCollectors() {
    return [ new RestControllerCollector(), new RestSchemaCollector() ]
  }

  public static int optionLength(String option) {
    return config.getOptionLength(option)
  }


  public static LanguageVersion languageVersion() {
    return LanguageVersion.JAVA_1_5
  }
}
