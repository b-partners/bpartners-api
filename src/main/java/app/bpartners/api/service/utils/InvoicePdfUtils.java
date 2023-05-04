package app.bpartners.api.service.utils;

import app.bpartners.api.model.Account;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.exception.ApiException;
import com.lowagie.text.DocumentException;
import java.io.ByteArrayOutputStream;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.service.utils.FileUtils.base64Image;
import static app.bpartners.api.service.utils.QrCodeUtils.generateQrCode;

public class InvoicePdfUtils {

  public byte[] generatePdf(Invoice invoice, AccountHolder accountHolder,
                            byte[] logoAsBytes, String template) {
    ITextRenderer renderer = new ITextRenderer();
    loadStyle(renderer, invoice, accountHolder, logoAsBytes, template);
    renderer.layout();

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try {
      renderer.createPDF(outputStream);
    } catch (DocumentException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
    return outputStream.toByteArray();
  }

  private void loadStyle(ITextRenderer renderer, Invoice invoice, AccountHolder accountHolder,
                         byte[] logoAsBytes, String template) {
    renderer.setDocumentFromString(parseInvoiceTemplateToString(invoice, accountHolder,
        logoAsBytes, template));
  }

  private String parseInvoiceTemplateToString(
      Invoice invoice, AccountHolder accountHolder, byte[] logoAsBytes, String template) {
    TemplateEngine templateEngine = configureTemplate();
    Context context = configureContext(invoice, accountHolder, logoAsBytes);
    return templateEngine.process(template, context);
  }


  private Context configureContext(Invoice invoice, AccountHolder accountHolder,
                                   byte[] logoAsBytes) {
    Context context = new Context();
    Account account = invoice.getActualAccount();

    context.setVariable("invoice", invoice);
    context.setVariable("logo", base64Image(logoAsBytes));
    context.setVariable("account", account);
    context.setVariable("accountHolder", accountHolder);
    if (invoice.getPaymentUrl() != null) {
      byte[] qrCodeBytes = generateQrCode(invoice.getPaymentUrl());
      context.setVariable("qrcode", base64Image(qrCodeBytes));
    }
    return context;
  }

  private TemplateEngine configureTemplate() {
    ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
    templateResolver.setPrefix("/templates/");
    templateResolver.setSuffix(".html");
    templateResolver.setCharacterEncoding("UTF-8");
    templateResolver.setTemplateMode(TemplateMode.HTML);

    TemplateEngine templateEngine = new TemplateEngine();
    templateEngine.setTemplateResolver(templateResolver);
    return templateEngine;
  }
}
