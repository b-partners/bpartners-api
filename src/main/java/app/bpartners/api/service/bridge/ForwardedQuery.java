package app.bpartners.api.service.bridge;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;

public final class ForwardedQuery {
  private static final String PAIR_SEPARATOR = "&";
  private static final char NAME_VALUE_SEPARATOR = '=';

  private ForwardedQuery() {}

  public static String keeping(String rawQuery, Set<String> allowedNames) {
    if (!StringUtils.hasLength(rawQuery)) {
      return "";
    }
    return Arrays.stream(StringUtils.delimitedListToStringArray(rawQuery, PAIR_SEPARATOR))
        .filter(pair -> isAllowed(pair, allowedNames))
        .collect(Collectors.joining(PAIR_SEPARATOR));
  }

  private static boolean isAllowed(String pair, Set<String> allowedNames) {
    try {
      return allowedNames.contains(UriUtils.decode(rawNameOf(pair), UTF_8));
    } catch (IllegalArgumentException malformedEncoding) {
      return false;
    }
  }

  private static String rawNameOf(String pair) {
    var separatorIndex = pair.indexOf(NAME_VALUE_SEPARATOR);
    return separatorIndex < 0 ? pair : pair.substring(0, separatorIndex);
  }
}
