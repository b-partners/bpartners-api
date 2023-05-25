package app.bpartners.api.service.utils;

import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

public class EmailUtils {

  public static final String EMAIL_PATTERN = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@"
      + "[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9_-]+)*(\\.[A-Za-z]{2,})$";

  private EmailUtils() {
  }

  public static boolean isValidEmail(String email) {
    return Pattern.compile(EMAIL_PATTERN)
        .matcher(email)
        .matches();
  }

  public static boolean hasMalformedTags(String htmlString) {
    return !Jsoup.isValid(htmlString, getCustomSafelist());
  }

  public static Safelist getCustomSafelist() {
    return Safelist.relaxed()
        .addTags("del")
        .removeTags("img");
  }

  public static String allowedTags() {
    return "a, b, blockquote, br, caption, cite, code, col, colgroup, dd, "
        + "div, dl, dt, em, h1, h2, h3, h4, h5, h6, i, img, li, ol, p, pre, q, "
        + "small, span, strike, strong, sub, sup, table, tbody, td, tfoot, th, "
        + "thead, tr, u, ul, del";
  }
}
