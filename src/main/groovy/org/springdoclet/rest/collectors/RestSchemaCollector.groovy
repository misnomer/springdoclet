package org.springdoclet.rest.collectors

import com.sun.javadoc.AnnotationDesc
import com.sun.javadoc.ClassDoc
import com.sun.javadoc.MethodDoc
import com.sun.javadoc.ParameterizedType
import groovy.xml.MarkupBuilder
import java.beans.Introspector
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.XmlType
import org.springdoclet.Annotations
import org.springdoclet.Configuration
import org.springdoclet.rest.RestCollector
import org.springdoclet.rest.RestPathBuilder
import org.springdoclet.TextUtils
import org.springdoclet.rest.ParameterizedTypes
import javax.xml.bind.annotation.XmlElementWrapper

/**
 * 
 * 
 * @author ck
 * @version $Revision: 1.2 $ $Date: 2004/05/02 22:16:32 $
 */
public class RestSchemaCollector implements RestCollector {

  private XML_TYPE = XmlType.class.name
  private XML_ROOT_TYPE = XmlRootElement.class.name
  private XML_ELEMENT_TYPE = XmlElement.class.name
  private XML_WRAPPED_ELEMENT_TYPE = XmlElementWrapper.class.name
  private XML_ATTRIBUTE_TYPE = XmlAttribute.class.name

  List<SchemaMapping> schemas = []


  @Override
  void processClass(ClassDoc classDoc, AnnotationDesc[] annotations) {
    SchemaMapping schema = new SchemaMapping()
    for (annotation in annotations) {
      String annotationType = Annotations.getTypeName(annotation)
      if (annotationType == XML_TYPE) {
        schema.className = classDoc.qualifiedTypeName()
        schema.elementName = elementName(annotation)
      } else if (annotationType == XML_ROOT_TYPE) {
        schema.rootName = Annotations.getValue(annotation, "name")
      }
    }
    if (schema.className) {
      if (! schema.elementName) schema.elementName = classDoc.typeName()
      schema.summary = TextUtils.getFirstSentence(classDoc.commentText())
      schema.comment = classDoc.commentText()
      schemas << schema
      println "Schema ${schema.className} : adding with element ${schema.elementName}"
      processProperties(classDoc, schema)
      processEnums(classDoc, schema)
    }

//    for (ClassDoc inner in classDoc.innerClasses(true)) {
//      processClass(inner, inner.annotations())
//    }
  }


  void processProperties(ClassDoc classDoc, SchemaMapping schema) {
    for (MethodDoc method in classDoc.methods(true)) {
      SchemaProperty property = new SchemaProperty();
      String name = method.name()
      if (name.startsWith("get")) {
        property.name = name.substring(3);
      } else if (name.startsWith("is")) {
        property.name = name.substring(2);
      } else {
        continue
      }
      property.name = Introspector.decapitalize(property.name)
      property.summary = TextUtils.getFirstSentence(method.commentText())
      property.comment = method.commentText()
      AnnotationDesc xmlElement = null, xmlAttribute = null, wrappedElement = null
      for (annotation in method.annotations()) {
        String annotationType = Annotations.getTypeName(annotation)
        if (annotationType == XML_ELEMENT_TYPE) {
          xmlElement = annotation
        } else if (annotationType == XML_ATTRIBUTE_TYPE) {
          xmlAttribute = annotation
        } else if (annotationType == XML_WRAPPED_ELEMENT_TYPE) {
          wrappedElement = annotation
        }
      }

      if (xmlElement) {
        String altName = elementName(xmlElement)
        if (wrappedElement) {
          property.childName = property.name
          String wrappedName = wrappedElement ? elementName(wrappedElement) : null
          if (wrappedName)
              property.name = wrappedName
          if (altName)
            property.childName = altName
        } else if (altName) {
          property.name = altName
        }
        property.required = Annotations.getValue(xmlElement, "required") == 'true'
      } else if (xmlAttribute) {
        property.attribute = true
        String altName = elementName(xmlAttribute)
        if (altName) {
          property.name = altName
        }
        property.required = Annotations.getValue(xmlAttribute, "required") == 'true'
      }

      if (method.returnType().asParameterizedType()) {
        property.type = method.returnType().qualifiedTypeName()
        ParameterizedType pt = method.returnType().asParameterizedType()
        if (pt.qualifiedTypeName() == 'java.util.Collection' || pt.qualifiedTypeName() == 'java.util.List') {
          property.repeated = true
          String altType = ParameterizedTypes.getFirstParameterReturnType(method)
          if (altType) {
//            println "\t${method.name()}: switching return type from ${property.type} to ${altType}"
            property.type = altType
          }
        }
      } else {
        property.type = method.returnType().qualifiedTypeName()
      }
      schema.properties << property
    }
//    if (schema.properties)
//      println "\tadded ${schema.properties.size()} properties."
  }


  private String elementName(AnnotationDesc annotation) {
    String elementName = Annotations.getValue(annotation, "name")
    if (elementName) elementName = elementName.replaceAll(/"/, "")
    return elementName
  }


  void processEnums(ClassDoc classDoc, SchemaMapping schema) {
    if (classDoc.isEnum()) {
      for (c in classDoc.enumConstants()) {
        schema.enums << new SchemaEnumValue(
                name: c.name(),
                comment: c.commentText(),
                summary: TextUtils.getFirstSentence(c.commentText())
        )
      }
//      println "\tadded ${schema.enums.size()} enum values."
    }
  }


  @Override
  void writeNavigation(MarkupBuilder builder, RestPathBuilder paths, Configuration config, File current) {
    builder.div(id: 'schema_nav') {
      h4 'Schema'
      ul {
        for (SchemaMapping schema in schemas.sort { it.elementName }) {
          li {
            a(href: paths.linkForSchema(schema.className, current)) {
              mkp.yield schema.elementName
            }
          }
        }
      }
    }
  }

  @Override
  void writeOutput(MarkupBuilder builder, RestPathBuilder paths, Configuration config) {
    builder.div(id: 'schemas') {
      h2 'Schema'
      table(id:'schema_table', 'class': 'condensed-table') {
        for (SchemaMapping schema in schemas.sort { it.elementName }) {
          tr {
            td {
              a(href: paths.linkForSchema(schema.className, null), schema.elementName)
            }
            td { mkp.yieldUnescaped(schema.summary ?: ' ') }
          }
        }
      }
    }


    for (schema in schemas) {
      File file = paths.fileForSchema(schema.className)
      boolean isEnum = ! schema.enums.isEmpty()
      paths.template(file, "${isEnum ? "Enum" : "Schema"} ${schema.elementName}", config) {MarkupBuilder localBuilder ->
        localBuilder.div {
          div('class': 'comment') { p {  mkp.yieldUnescaped(schema.comment ?: '') } }
          if (schema.enums) {
            div(id: 'enum_value_summary') {
              h2 "Enum Values"
              table(id: 'enum_value_table', 'class': 'condensed-table') {
                for (e in schema.enums) {
                  tr {
                    td e.name
                    td('class': 'comment', e.summary)
                  }
                }
              }
            }
          } else {
            div(id: 'property_summary') {
              h2 "Property Summary"
              table(id: 'property_summary_table', 'class': 'condensed-table') {
                for (property in schema.properties) {
                  writePropertySummary(property, paths, localBuilder, file)
                }
              }
            }
            for (property in schema.properties) {
              writePropertyDetail(property, paths, localBuilder, file)
            }
          }
        }
      }
    }
  }


  private void writePropertySummary(SchemaProperty property, RestPathBuilder paths, MarkupBuilder builder,
                                    File current) {
    builder.tr {
      td {
        a(href: "#property_${property.name}", property.name)
      }
      td { paths.writeSchemaLink(property.type, builder, current) }
      td(property.attribute ? "Attribute" : "Element")
      td(property.required ? "Required" : "Optional")
      td(property.repeated ? "Repeated" : "Single")
    }
  }


  private void writePropertyDetail(SchemaProperty property, RestPathBuilder paths, MarkupBuilder builder,
                                   File current) {
    builder.div(id: "property_${property.name}") {
      a(name: "property_${property.name}") {
        h3 property.name
      }
      div('class': 'comment') { p { mkp.yieldUnescaped(property.comment ?: '') } }
      table('class': 'detail-table') {
        tr {
          td('class': 'name', 'Type')
          td { paths.writeSchemaLink(property.type, builder, current) }
        }
        tr {
          td('class': 'name', 'Structure')
          td(property.attribute ? "Attribute" : "Element")
        }
        tr {
          td('class': 'name', 'Required')
          td(property.required ? "Yes" : "No")
        }
        tr {
          td('class': 'name', 'Repeated')
          td {
              span(property.repeated ? "Yes" : "No")
              span(property.childName ? "(${property.childName})" : '')
          }
        }
      }
    }
  }
}

class SchemaMapping {
  String className
  String elementName
  String rootName
  String summary
  String comment
  List<SchemaProperty> properties = []
  List<SchemaEnumValue> enums = []
}


class SchemaProperty {
  String name
  String type
  boolean required
  boolean attribute
  boolean repeated
  String childName
  String summary
  String comment
}

class SchemaEnumValue {
  String name
  String summary
  String comment
}
