package app.bpartners.api.utils;

import java.lang.reflect.Field;

public class ReflectApi<T> {
  public String getFields(Object object) {
    Field[] fields = object.getClass().getFields();
    StringBuilder fieldString = new StringBuilder("{");
    for (Field field : fields) {
      fieldString.append(field.getName());
      if (field != fields[fields.length - 1]) {
        fieldString.append(" ");
      }
    }
    fieldString.append("}");
    return fieldString.toString();
  }
  public static void main(String[] args) {
    System.out.println();
  }
}
