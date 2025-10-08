package app.bpartners.api.service.annotation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Emoji replaced for use with Twemoji files. By danfickle. MIT or Apache license. */
public class EmojReplacer {
  private static class Matcher {
    private Map<Integer, Matcher> next;

    private Matcher put(int cp) {
      if (next == null) {
        next = new HashMap<>();
      }

      if (!next.containsKey(cp)) {
        Matcher put = new Matcher();
        next.put(cp, put);
        return put;
      }

      return next.get(cp);
    }
  }

  private final Matcher root;
  private final Path svgDirectory;
  private final String prefix;
  private final String suffix;

  private static <T> Predicate<T> not(Predicate<T> inner) {
    return (toTest) -> !inner.test(toTest);
  }

  private void addToMatcher(String svg, Matcher root) {
    String[] codePointStrings = svg.split(Pattern.quote("-"));
    Matcher current = root;

    for (int i = 0; i < codePointStrings.length; i++) {
      int cp = Integer.parseUnsignedInt(codePointStrings[i], 16);
      current = current.put(cp);
    }
  }

  private Matcher createRootMatcher(Stream<Path> svgs) {
    Matcher root = new Matcher();

    svgs.filter(not(Files::isDirectory))
        .map(Path::getFileName)
        .map(Path::toString)
        .filter(path -> path.endsWith(".svg"))
        .map(path -> path.substring(0, path.length() - 4))
        .forEach(svgFile -> addToMatcher(svgFile, root));

    return root;
  }

  public EmojReplacer(Path pathToSvgs, String prefix, String suffix) throws IOException {
    this.svgDirectory = pathToSvgs;
    this.prefix = prefix;
    this.suffix = suffix;
    try (Stream<Path> svgs = Files.list(pathToSvgs)) {
      this.root = createRootMatcher(svgs);
    }
  }

  private String getEmoji(List<Integer> codePoints) {
    StringBuilder sb = new StringBuilder(codePoints.size() * 8);

    if (codePoints.size() == 1) {
      sb.append(Integer.toHexString(codePoints.get(0)));
    } else {
      String joined =
          codePoints.stream().map(Integer::toHexString).collect(Collectors.joining("-"));

      sb.append(joined);
    }

    sb.append(".svg");
    String file = sb.toString();

    try {
      byte[] bytes = Files.readAllBytes(this.svgDirectory.resolve(file));
      return this.prefix
          + (new String(bytes, StandardCharsets.UTF_8)
              .replace("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>", ""))
          + this.suffix;
    } catch (IOException e) {
      System.err.println("Couldn't read emoji with filename: " + file);
      e.printStackTrace();
      return "";
    }
  }

  public String replaceEmoji(String input) {
    Matcher current = root;
    List<Integer> emojiCodePoints = new ArrayList<>();
    StringBuilder sb = new StringBuilder(input.length());

    for (int i = 0; i < input.length(); ) {
      int cp = input.codePointAt(i);
      Matcher next = current.next == null ? null : current.next.get(cp);
      boolean consumed;

      if (next != null) {
        // At the start or middle of an emoji character sequence...
        emojiCodePoints.add(cp);
        current = next;
        consumed = true;
      } else if (!emojiCodePoints.isEmpty()) {
        // At the end of an emoji...
        // TODO: Leace alone variant.
        String svgCode = getEmoji(emojiCodePoints);
        sb.append(svgCode);

        emojiCodePoints.clear();
        current = root;

        consumed = false;
      } else if (current == this.root) {
        // Not an emoji character...
        sb.appendCodePoint(cp);
        consumed = true;
      } else {
        // Shouldn't happen...
        consumed = false;
      }

      if (consumed) {
        i += Character.charCount(cp);
      }
    }

    if (!emojiCodePoints.isEmpty()) {
      String svgCode = getEmoji(emojiCodePoints);
      sb.append(svgCode);
    }

    return sb.toString();
  }
}
