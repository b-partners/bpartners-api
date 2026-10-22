package app.bpartners.api.service.annotation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/** Emoji replaced for use with Twemoji files. By danfickle. MIT or Apache license. */
@Slf4j
public class EmojiReplacer {
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
  private final String svgDirectory;
  private final String prefix;
  private final String suffix;

  private void addToMatcher(String svg, Matcher root) {
    String[] codePointStrings = svg.split(Pattern.quote("-"));
    Matcher current = root;

    for (var codePointString : codePointStrings) {
      int cp = Integer.parseUnsignedInt(codePointString, 16);
      current = current.put(cp);
    }
  }

  private Matcher createRootMatcher(Stream<Resource> svgs) {
    Matcher root = new Matcher();

    svgs.map(Resource::getFilename)
        .filter(name -> name != null && name.endsWith(".svg"))
        .map(name -> name.substring(0, name.length() - 4))
        .forEach(svgFile -> addToMatcher(svgFile, root));

    return root;
  }

  /** Reads the emoji svgs from a classpath directory, which also works from inside a jar. */
  public EmojiReplacer(String classpathSvgDirectory, String prefix, String suffix)
      throws IOException {
    this.svgDirectory = classpathSvgDirectory;
    this.prefix = prefix;
    this.suffix = suffix;
    var svgs =
        new PathMatchingResourcePatternResolver()
            .getResources("classpath:" + classpathSvgDirectory + "/*.svg");
    this.root = createRootMatcher(Stream.of(svgs));
  }

  private String getEmoji(List<Integer> codePoints) {
    StringBuilder sb = new StringBuilder(codePoints.size() * 8);

    if (codePoints.size() == 1) {
      sb.append(Integer.toHexString(codePoints.getFirst()));
    } else {
      String joined =
          codePoints.stream().map(Integer::toHexString).collect(Collectors.joining("-"));

      sb.append(joined);
    }

    sb.append(".svg");
    String file = sb.toString();

    try {
      byte[] bytes;
      try (var inputStream =
          new ClassPathResource(this.svgDirectory + "/" + file).getInputStream()) {
        bytes = inputStream.readAllBytes();
      }
      return this.prefix
          + (new String(bytes, StandardCharsets.UTF_8)
              .replace("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>", ""))
          + this.suffix;
    } catch (IOException e) {
      log.error("Couldn't read emoji with filename: {}", file, e);
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
