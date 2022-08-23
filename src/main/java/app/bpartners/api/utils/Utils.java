package app.bpartners.api.utils;

import java.lang.reflect.Field;

public class Utils {
  public static StringBuilder getClassField(Object obj) {
    String[] array = obj.getClass().toString().split(" ");
    StringBuilder result = new StringBuilder()
        .append("{");
    try {
      Class<?> test = Class.forName(array[1]);
      Field[] list = test.getDeclaredFields();
      for (Field field : list) {
        result.append(field.getName()).append(" ");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return result.append("}");
  }
}
