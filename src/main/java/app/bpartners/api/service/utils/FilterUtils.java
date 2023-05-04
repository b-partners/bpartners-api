package app.bpartners.api.service.utils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class FilterUtils {
  private FilterUtils() {
  }

  @SafeVarargs
  public static <T> Predicate<T> distinctByKeys(final Function<? super T, ?>... keyExtractors) {
    final Map<List<?>, Boolean> seen = new ConcurrentHashMap<>();
    return elt -> {
      final List<?> keys = Arrays.stream(keyExtractors)
          .map(key -> key.apply(elt))
          .collect(Collectors.toList());
      return seen.putIfAbsent(keys, Boolean.TRUE) == null;
    };
  }
}
