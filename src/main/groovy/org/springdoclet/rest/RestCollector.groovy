package org.springdoclet.rest;


import com.sun.javadoc.AnnotationDesc
import com.sun.javadoc.ClassDoc
import groovy.xml.MarkupBuilder
import org.springdoclet.Configuration

/**
 * @author ck
 * @version $Revision: 1.2 $ $Date: 2004/05/02 22:16:32 $
 */
public interface RestCollector {

  void processClass(ClassDoc classDoc, AnnotationDesc[] annotations)

  void writeNavigation(MarkupBuilder builder, RestPathBuilder paths, Configuration config, File current)

  void writeOutput(MarkupBuilder builder, RestPathBuilder paths, Configuration config)

}
