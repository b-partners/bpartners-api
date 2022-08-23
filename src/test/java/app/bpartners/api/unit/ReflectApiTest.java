package app.bpartners.api.unit;

import app.bpartners.api.repository.swan.schema.SwanAccount;
import app.bpartners.api.repository.swan.schema.SwanUser;
import app.bpartners.api.utils.ReflectApi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ReflectApiTest {
  @Test
  public void getPublicAttributes(){
    String expected = "{user { idVerified nationalityCCA3 identificationStatus birthDate mobilePhoneNumber lastName firstName id }}";
    String expected2 = "{account { BIC IBAN number name id }}";

    Assertions.assertEquals(expected,ReflectApi.getFields("user", SwanUser.class));
    Assertions.assertEquals(expected2,ReflectApi.getFields("account",SwanAccount.class));
  }
}
