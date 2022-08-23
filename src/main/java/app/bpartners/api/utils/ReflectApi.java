package app.bpartners.api.utils;

import java.lang.reflect.Field;

public class ReflectApi {
  public static String getFields(String cl,Class<?> className){
    Field[] fields = className.getDeclaredFields();
    StringBuilder fieldString = new StringBuilder("{").append(cl).append(" { ");
    for (int i = fields.length - 1; i >= 0; i--) {
      fieldString
          .append(fields[i].getName())
          .append(" ");
    }
    fieldString.append("}}");
    return fieldString.toString();
  }
}