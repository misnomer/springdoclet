package org.springdoclet.rest

import com.sun.javadoc.MethodDoc
import com.sun.javadoc.ParameterizedType

/**
 * 
 * 
 * @author ck
 * @version $Revision: 1.2 $ $Date: 2004/05/02 22:16:32 $
 */
public class ParameterizedTypes {


  public static String getFirstParameterReturnType(MethodDoc method) {
    if (method.returnType().asParameterizedType()) {
//      println "${method.containingClass().typeName()} : ${method.name()} : ${method.returnType().asParameterizedType().typeArguments() as List}"
      ParameterizedType pt = method.returnType().asParameterizedType()
      List types = pt.typeArguments()
//      List types = method.sym.type.restype.typarams_field
      if (types) {
        return types[0] as String
      }
    }
    return null
  }
}