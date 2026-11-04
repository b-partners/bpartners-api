package app.bpartners.api.service.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import org.junit.jupiter.api.Test;

class ForwardedQueryTest {
  private static final Set<String> ALLOWED = Set.of("bbox", "address", "humidité", "types");

  @Test
  void plain_value_is_kept_untouched() {
    assertEquals("bbox=1,2,3,4", ForwardedQuery.keeping("bbox=1,2,3,4", ALLOWED));
  }

  @Test
  void percent_encoded_comma_is_not_decoded_nor_encoded_again() {
    assertEquals("bbox=1%2C2%2C3%2C4", ForwardedQuery.keeping("bbox=1%2C2%2C3%2C4", ALLOWED));
  }

  @Test
  void percent_encoded_space_is_kept_as_received() {
    assertEquals("address=Tour%20Eiffel", ForwardedQuery.keeping("address=Tour%20Eiffel", ALLOWED));
  }

  @Test
  void plus_is_kept_as_received() {
    assertEquals(
        "address=Tour+Eiffel,+Paris",
        ForwardedQuery.keeping("address=Tour+Eiffel,+Paris", ALLOWED));
  }

  @Test
  void utf8_percent_encoded_value_is_kept_as_received() {
    assertEquals("address=Caf%C3%A9", ForwardedQuery.keeping("address=Caf%C3%A9", ALLOWED));
  }

  @Test
  void encoded_name_is_matched_on_its_decoded_form_and_kept_as_received() {
    assertEquals("humidit%C3%A9=3", ForwardedQuery.keeping("humidit%C3%A9=3", ALLOWED));
  }

  @Test
  void names_that_are_not_allowed_are_dropped() {
    assertEquals(
        "bbox=1&address=a",
        ForwardedQuery.keeping("sld_body=x&bbox=1&evil&address=a&other=2", ALLOWED));
  }

  @Test
  void api_key_query_param_is_dropped() {
    assertEquals("address=a", ForwardedQuery.keeping("x-api-key=secret&address=a", ALLOWED));
  }

  @Test
  void null_and_empty_give_an_empty_query() {
    assertEquals("", ForwardedQuery.keeping(null, ALLOWED));
    assertEquals("", ForwardedQuery.keeping("", ALLOWED));
  }

  @Test
  void encoded_ampersand_in_a_value_cannot_smuggle_a_new_param() {
    assertEquals(
        "address=a%26sld_body=x", ForwardedQuery.keeping("address=a%26sld_body=x", ALLOWED));
  }

  @Test
  void repeated_param_is_kept_in_order() {
    assertEquals(
        "types=IMAGERY&types=LIDAR", ForwardedQuery.keeping("types=IMAGERY&types=LIDAR", ALLOWED));
  }

  @Test
  void pair_with_malformed_percent_encoding_in_its_name_is_dropped() {
    assertEquals("address=a", ForwardedQuery.keeping("%zz=1&address=a", ALLOWED));
  }
}
