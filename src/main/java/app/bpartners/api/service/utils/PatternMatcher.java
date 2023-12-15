package app.bpartners.api.service.utils;

public class PatternMatcher {
  private PatternMatcher() {}

  public static String filterCharacters(String input, String pattern) {
    return input.replaceAll(pattern, "");
  }
}
