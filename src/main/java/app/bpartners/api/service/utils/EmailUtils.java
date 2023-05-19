package app.bpartners.api.service.utils;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

public class EmailUtils {
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
