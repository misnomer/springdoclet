package org.springdoclet.rest

import groovy.xml.MarkupBuilder
import org.springdoclet.Configuration
import org.springdoclet.rest.collectors.RestSchemaCollector

/**
 * @author ck
 * @version $Revision: 1.2 $ $Date: 2004/05/02 22:16:32 $
 */
public class RestPathBuilder {
  File baseDirectory
  List<RestCollector> collectors
  Configuration config
  Map<String,String> leftNav = [:]
  Map<String,String> schemaToElement
  List<ExternalDocumentation> overviewFiles


  String linkForController(String controllerClass, File current) {
    return "${pathToBase(current)}controllers/${simpleClass(controllerClass)}.html"
  }

  File fileForController(String controllerClass) {
    return new File(baseDirectory, linkForController(controllerClass, null));
  }

  boolean isSchema(String className) {
    loadSchemas()
    return schemaToElement.containsKey(className)
  }

  String elementForSchema(String schemaClass) {
    loadSchemas()
    String ret = schemaToElement[schemaClass]
    if (ret) return ret
    return simpleClass(schemaClass)
  }

  private void loadSchemas() {
    if (! schemaToElement) {
      RestSchemaCollector c = collectors.find { it instanceof RestSchemaCollector }
      schemaToElement = [:]
      for (schema in c.schemas) {
        schemaToElement[schema.className] = schema.elementName
      }
    }
  }

  def writeSchemaLink(String className, MarkupBuilder builder, File current) {
    if (isSchema(className)) {
      builder.a(href: linkForSchema(className, current), elementForSchema(className))
    } else {
      builder.mkp.yield simpleClass(className)
    }
  }

  String linkForSchema(String schemaClass, File current) {
    return "${pathToBase(current)}schemas/${simpleClass(schemaClass)}.html"
  }


  File fileForSchema(String schemaClass) {
    return new File(baseDirectory, linkForSchema(schemaClass, null));
  }


  String linkForOverview(String name, File current) {
    return "${pathToBase(current)}overview/${name}"
  }

  File fileForOverview(String name) {
    return new File(baseDirectory, linkForOverview(name, null))
  }


  String pathToBase(File file) {
    if (! file) return ""
    StringBuilder sb = new StringBuilder(16)
//    println "finding path to base for ${file.canonicalPath}.  Base is ${baseDirectory}"
    for (File f = file.parentFile; f && f.canonicalPath != baseDirectory.canonicalPath; f = f.parentFile) {
      sb.append('../')
//      println "\t${sb}\t${f}"
    }
    return sb.toString()
  }

  private String leftNav(File current) {
    String base = pathToBase(current)
    String cached = leftNav[base]
    if (cached) return cached
    System.out.println "Generating left nav for ${base}"
    StringWriter sw = new StringWriter()
    def navBuilder = new MarkupBuilder(sw)

    navBuilder.div {
      h4 'Summary'
      ul {
        li {
          a(href: pathToBase(current) + config.outputFileName, "Overview")
        }
        for (doc in overviewFiles) {
          li {
            a(href: linkForOverview(doc.name, current), doc.title)
          }
        }
      }

      for (RestCollector collector in collectors) {
        collector.writeNavigation(navBuilder, this, config, current)
      }
    }
    leftNav[base] = sw.toString()
    return sw.toString()
  }


  void template(File file, String titleString, Configuration config, Closure closure) {
    System.out.println "Generating ${file.canonicalPath - baseDirectory.canonicalPath}"
    file.parentFile.mkdirs()
    file.withWriter {Writer writer ->
      def builder = new MarkupBuilder(writer)
      builder.html {
        head {
          title titleString ?: config.title
          link(rel: 'stylesheet', type: 'text/css', href: "${pathToBase(file)}${config.styleSheet}")
        }
        body {
          div(id: 'mainWrapper') {
            div(id: 'leftNav') {
              mkp.yieldUnescaped leftNav(file)
            }
            div(id: 'mainContent') {
              h1 titleString ?: config.title
              closure.call(builder)
            }
            div(id: 'mainFooterPush', '')
          }
          div(id: 'mainFooter') {
            mkp.yieldUnescaped config.footer
          }
        }
      }
    }

  }


  static String simpleClass(String className) {
    return className.replaceAll(/^.+\./, "")
  }

}
