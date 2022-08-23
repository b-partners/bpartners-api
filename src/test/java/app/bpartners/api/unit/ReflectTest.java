package app.bpartners.api.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import app.bpartners.api.repository.swan.schema.SwanUser;
import app.bpartners.api.utils.Utils;
import org.junit.jupiter.api.Test;

public class ReflectTest {
  @Test
  void getClassAttributes(){
    String expected =
        "{id firstName lastName mobilePhoneNumber birthDate identificationStatus " +
            "nationalityCCA3 idVerified }";
    assertEquals(expected, Utils.getClassField(new SwanUser()));
  }
}
