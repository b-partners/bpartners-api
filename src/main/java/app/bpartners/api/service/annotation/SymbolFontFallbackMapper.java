package app.bpartners.api.service.annotation;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Set;
import org.apache.fontbox.FontBoxFont;
import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.font.CIDFontMapping;
import org.apache.pdfbox.pdmodel.font.FontMapper;
import org.apache.pdfbox.pdmodel.font.FontMappers;
import org.apache.pdfbox.pdmodel.font.FontMapping;
import org.apache.pdfbox.pdmodel.font.PDCIDSystemInfo;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;
import org.springframework.core.io.ClassPathResource;

/**
 * openhtmltopdf's PdfBoxFontResolver unconditionally instantiates PDFBox's standard-14 "Symbol" and
 * "ZapfDingbats" fonts on every render, even though this app never draws a glyph with them. On
 * systems without OS-level substitutes for those PostScript fonts, PDFBox logs a WARN and falls
 * back to LiberationSans. This mapper answers those two lookups directly with an already-bundled
 * font so PDFBox never needs to fall back (and never logs the warning).
 */
final class SymbolFontFallbackMapper implements FontMapper {
  private static final Set<String> SUBSTITUTED_BASE_FONTS = Set.of("Symbol", "ZapfDingbats");

  private final FontMapper delegate;
  private final TrueTypeFont substituteFont;

  private SymbolFontFallbackMapper(FontMapper delegate, String substituteFontResourcePath) {
    this.delegate = delegate;
    this.substituteFont = loadTrueTypeFont(substituteFontResourcePath);
  }

  static synchronized void installOnce(String substituteFontResourcePath) {
    var current = FontMappers.instance();
    if (current instanceof SymbolFontFallbackMapper) {
      return;
    }
    FontMappers.set(new SymbolFontFallbackMapper(current, substituteFontResourcePath));
  }

  private static TrueTypeFont loadTrueTypeFont(String resourcePath) {
    try (InputStream inputStream = new ClassPathResource(resourcePath).getInputStream()) {
      return new TTFParser().parse(new RandomAccessReadBuffer(inputStream));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public FontMapping<TrueTypeFont> getTrueTypeFont(
      String baseFont, PDFontDescriptor fontDescriptor) {
    return delegate.getTrueTypeFont(baseFont, fontDescriptor);
  }

  @Override
  public FontMapping<FontBoxFont> getFontBoxFont(String baseFont, PDFontDescriptor fontDescriptor) {
    if (SUBSTITUTED_BASE_FONTS.contains(baseFont)) {
      return new FontMapping<>(substituteFont, false);
    }
    return delegate.getFontBoxFont(baseFont, fontDescriptor);
  }

  @Override
  public CIDFontMapping getCIDFont(
      String baseFont, PDFontDescriptor fontDescriptor, PDCIDSystemInfo cidSystemInfo) {
    return delegate.getCIDFont(baseFont, fontDescriptor, cidSystemInfo);
  }
}
