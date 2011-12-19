package org.springdoclet.rest.collectors

import groovy.xml.MarkupBuilder
import org.springdoclet.Annotations
import org.springdoclet.Configuration
import org.springdoclet.TextUtils
import org.springdoclet.rest.ExternalDocumentation
import org.springdoclet.rest.ParameterizedTypes
import org.springdoclet.rest.RestCollector
import org.springdoclet.rest.RestPathBuilder
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import com.sun.javadoc.*
import org.springframework.web.bind.annotation.*

class RestControllerCollector implements RestCollector {
  // class annotations
  private static String CONTROLLER_TYPE = Controller.class.name
  // class & method annotations
  private static String MAPPING_TYPE = RequestMapping.class.name
  // method annotations
  private static String RESPONSE_BODY_TYPE = ResponseBody.class.name
  // parameter annotations
  private static String REQUEST_BODY_TYPE = RequestBody.class.name
  private static String PATH_VARIABLE_TYPE = PathVariable.class.name
  private static String REQUEST_PARAM_TYPE = RequestParam.class.name

  private List<ControllerMapping> controllers = []

  void processClass(ClassDoc classDoc, AnnotationDesc[] annotations) {
    ControllerMapping controller = new ControllerMapping()
    for (annotation in annotations) {
      String annotationType = Annotations.getTypeName(annotation)
      if (annotationType == CONTROLLER_TYPE) {
        controller.className = classDoc.qualifiedTypeName()
      } else if (annotationType == MAPPING_TYPE) {
        controller.requestPath = getRequestPath(annotation)
        controller.defaultHttpMethod = getHttpMethod(annotation) ?: "GET"
      }
    }
    if (controller.className && controller.requestPath) {
      controller.comment = classDoc.commentText()
      controller.summary = TextUtils.getFirstSentence(classDoc.commentText())
      controllers << controller

      processMethods(controller, classDoc)
    }
  }

  private void processMethods(ControllerMapping controller, ClassDoc classDoc) {
    for (MethodDoc method in classDoc.methods(true)) {
      ActionMapping action = new ActionMapping()
      for (AnnotationDesc annotation in method.annotations()) {
        String annotationType = Annotations.getTypeName(annotation)
        if (annotationType == MAPPING_TYPE) {
          action.requestPath = getRequestPath(annotation)
          if (action.requestPath != null) {
            action.methodName = method.name()
            action.httpMethod = getHttpMethod(annotation) ?: controller.defaultHttpMethod
            action.comment = method.commentText()
            action.summary = TextUtils.getFirstSentence(method.commentText())
          }
        } else if (annotationType == RESPONSE_BODY_TYPE) {
//          println "\t${method.name()} return ${method.returnType().typeName()}  ${method.returnType().class}"
          action.responseBodyClassName = method.returnType().qualifiedTypeName()
          if (action.responseBodyClassName == ResponseEntity.class.name) {
            String altType = ParameterizedTypes.getFirstParameterReturnType(method)
            if (altType) {
              action.responseBodyClassName = altType
//              println "\t${method.name()}: switching return type from ${method.returnType().typeName()} to ${action.responseBodyClassName}"
            }
          }
        }
      }
      if (action.requestPath != null) {
        println "Controller ${controller.className}  Adding method ${method.name()} with path '${action.requestPath}'"
        processParameters(action, method)
        processExternalDocumentation(action, method)
        controller.actions << action
      }
    }
  }


  private void processParameters(ActionMapping action, MethodDoc method) {
    for (Parameter p in method.parameters()) {
      for (annotation in p.annotations()) {
        String annotationType = Annotations.getTypeName(annotation)
        if (annotationType == REQUEST_BODY_TYPE) {
          action.requestBodyClassName = p.type().qualifiedTypeName()
        } else if (annotationType == REQUEST_PARAM_TYPE || annotationType == PATH_VARIABLE_TYPE) {
          RequestParameterMapping m = new RequestParameterMapping()
          m.name = p.name()
          m.type = p.type().typeName()
          String req = Annotations.getValue(annotation, "required")
          m.required = req != 'false'
          m.defaultValue = Annotations.getValue(annotation, "defaultValue") ?: null
          ParamTag tag = method.paramTags().find { it.parameterName() == p.name() } as ParamTag
          if (tag)
            m.comment = tag.parameterComment()
          if (annotationType == REQUEST_PARAM_TYPE)
            action.requestParameters << m
          else
            action.pathVariables << m
        }
      }
    }
  }

  private String getRequestPath(AnnotationDesc annotation) {
    String path = Annotations.getValue(annotation, "value")
    if (path) path = path.replaceAll(/"/, "")
    return path
  }

  private String getHttpMethod(AnnotationDesc annotation) {
    String method = Annotations.getValue(annotation, "method")
    if (method) method = method.replaceAll(/^.+\./, "")
    return method
  }

  private void processExternalDocumentation(ActionMapping action, MethodDoc method) {
    File classFile = method.containingClass().position().file()
    if (! classFile) return
    File docDir = new File(classFile.getParentFile(), "doc-files")
    File docFile = new File(docDir, method.containingClass().typeName() + "_" + method.name() + ".html")
    if (docFile.exists()) {
      ExternalDocumentation doc = new ExternalDocumentation(docFile)
      action.externalDocs = doc.text
    }
  }

  
  void writeNavigation(MarkupBuilder builder, RestPathBuilder paths, Configuration config, File current) {
    builder.div(id: 'controller_nav') {
      h4 'Controllers'
      ul {
        for (ControllerMapping controller in controllers.sort { it.requestPath}) {
          li {
            a(href: paths.linkForController(controller.className, current)) {
              mkp.yield controller.requestPath
            }
          }
        }
      }
    }
  }

  void writeOutput(MarkupBuilder builder, RestPathBuilder paths, Configuration config) {
    builder.div(id: 'controllers') {
      h2 'Controllers'
      table(id:'controller_table') {
        for (ControllerMapping controller in controllers.sort { it.requestPath }) {
          tr {
            td {
              a(href: paths.linkForController(controller.className, null), controller.requestPath ?: 'none')
            }
            td { mkp.yieldUnescaped(controller.summary ?: ' ') }
          }
        }
      }
    }

    for (controller in controllers) {
      File file = paths.fileForController(controller.className)
      paths.template(file, "Controller ${controller.requestPath}", config) {MarkupBuilder localBuilder ->
        localBuilder.div {
          div('class': 'comment') { p { mkp.yieldUnescaped(controller.comment ?: '') } }
          div(id: 'action_summary') {
            h2 'Action Summary'
            table(id: 'action_summary_table') {
              for (ActionMapping action in controller.actions) {
                writeActionSummary(controller, action, localBuilder)
              }
            }
          }
          for (ActionMapping action in controller.actions) {
            writeActionDetail(controller, action, paths, localBuilder, file)
          }
        }
      }
    }
  }

  private void writeActionSummary(ControllerMapping controller, ActionMapping action, MarkupBuilder builder) {
    builder.tr {
      td {
        a(href: "#${action.methodName}", action.methodName)
      }
      td { code action.httpMethod }
      td { code "${controller.requestPath}${action.requestPath}" }
      td { mkp.yieldUnescaped(action.summary ?: ' ') }
    }
  }


  private void writeActionDetail(ControllerMapping controller, ActionMapping action, RestPathBuilder paths,
                                 MarkupBuilder builder, File current) {
    
    builder.div(id: "action_${action.methodName}") {
      a(name: action.methodName) {
        h3 action.methodName
      }
      div('class': 'comment') { p { mkp.yieldUnescaped(action.comment ?: '') } }

      table('class': 'detailTable') {
        tr {
          td('class': 'label', 'Path')
          td { code "${controller.requestPath}${action.requestPath}" }
        }
        tr {
          td('class': 'label', 'Method')
          td { code action.httpMethod }
        }
        if (action.requestBodyClassName) {
          tr {
            td('class': 'label', 'Request Body')
            td {
              paths.writeSchemaLink(action.requestBodyClassName, builder, current)
            }
          }
        }
        if (action.responseBodyClassName) {
          tr {
            td('class': 'label', 'Response Body')
            td {
             paths.writeSchemaLink(action.responseBodyClassName, builder, current)
            }
          }
        }
      }

      if (action.pathVariables) {
        h4 'Path Variables'
        table('class': 'detailTable') {
          for (RequestParameterMapping r in action.pathVariables) {
            tr {
              td('class': 'label', r.name)
              td { code r.type  }
              td(r.required ? "Required" : "Optional")
              td(r.defaultValue ? "Default: ${r.defaultValue}" : "")
              td('class': 'comment') { mkp.yieldUnescaped(r.comment ?: '') }
            }
          }
        }
      }

      if (action.requestParameters) {
        h4 'Request Parameters'
        table('class': 'detailTable') {
          for (RequestParameterMapping r in action.requestParameters) {
            tr {
              td('class': 'label', r.name)
              td { code r.type  }
              td(r.required ? "Required" : "Optional")
              td(r.defaultValue ? "Default: ${r.defaultValue}" : "")
              td('class': 'comment') { mkp.yieldUnescaped(r.comment ?: '') }
            }
          }
        }
      }

      if (action.externalDocs) {
        div {
          mkp.yieldUnescaped action.externalDocs
        }
      }
/*
      div {
        h4 'Example Request'
        div('class': 'example',
                '''<request>
some stuff here
</request>
''')
      }

      div {
        h4 'Example Response'
        div('class': 'example',
                '''GET /foo
<response>
some stuff here
</response>
''')
      }
*/
    }
  }


}

class ControllerMapping {
  String className
  String summary
  String comment
  String requestPath
  String defaultHttpMethod
  List<ActionMapping> actions = []
}

class ActionMapping {
  String methodName
  String requestPath
  String httpMethod
  List<RequestParameterMapping> pathVariables = []
  List<RequestParameterMapping> requestParameters = []
  String requestBodyClassName
  String responseBodyClassName
  String summary
  String comment
  String externalDocs
}

class RequestParameterMapping {
  String name
  String type
  boolean required
  String defaultValue
  String comment
}
