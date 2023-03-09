package app.bpartners.api.unit.utils;

import app.bpartners.api.service.utils.URLUtils;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static app.bpartners.api.service.utils.URLUtils.URLEncodeMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

class URLUtilsTest {
  public static final String SIMPLE_KEY = "a";
  public static final String SIMPLE_VALUE = "b";
  public static final String COMPLEX_KEY = "a[b]";
  public static final String COMPLEX_VALUE = "c,d";
  private static final String EXPECTED_URL_ENCODED_MAP =
      SIMPLE_KEY + "=" + SIMPLE_VALUE + "&"
          + URLEncoder.encode(COMPLEX_KEY)
          + "=" + URLEncoder.encode(COMPLEX_VALUE);

  @Test
  void encode_map_ok() {
    assertEquals(EXPECTED_URL_ENCODED_MAP, URLEncodeMap(map()));
  }

  Map<String, String> map() {
    Map<String, String> map = new HashMap<>();
    //simple key value where no encoding is needed
    map.put(SIMPLE_KEY, SIMPLE_VALUE);
    //complex (key,value) couple where both key and value need to be encoded
    map.put(COMPLEX_KEY, COMPLEX_VALUE);
    return map;
  }
}
