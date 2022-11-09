package app.bpartners.api.service;

import app.bpartners.api.endpoint.event.model.gen.InvoiceCrupdated;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.FileInfo;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.DocumentException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.Base64;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE;
import static app.bpartners.api.endpoint.rest.model.FileType.LOGO;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.service.utils.FileInfoUtils.JPG_FORMAT_NAME;
import static app.bpartners.api.service.utils.FileInfoUtils.PDF_EXTENSION;
import static com.google.zxing.BarcodeFormat.QR_CODE;
import static java.util.UUID.randomUUID;

@Service
@AllArgsConstructor
//TODO: add InvoiceCrupdatedServiceTest
public class InvoiceCrupdatedService implements Consumer<InvoiceCrupdated> {
  public static final String INVOICE_TEMPLATE = "invoice";
  public static final String DRAFT_TEMPLATE = "draft";
  private final FileService fileService;
  private final InvoiceJpaRepository invoiceJpaRepository;

  @Transactional
  @Override
  public void accept(InvoiceCrupdated invoiceCrupdated) {
    AccountHolder accountHolder = invoiceCrupdated.getAccountHolder();
    String logoFileId = invoiceCrupdated.getLogoFileId();
    Invoice invoice = invoiceCrupdated.getInvoice();
    String accountId = invoice.getAccount().getId();
    String fileId =
        invoice.getFileId() == null ? randomUUID() + PDF_EXTENSION : invoice.getFileId();
    byte[] fileAsBytes =
        invoice.getStatus().equals(CONFIRMED) || invoice.getStatus().equals(PAID)
            ? generatePdf(invoice, accountHolder, logoFileId, INVOICE_TEMPLATE)
            : generatePdf(invoice, accountHolder, logoFileId, DRAFT_TEMPLATE);

    FileInfo fileInfo = fileService.upload(fileId, INVOICE, accountId, fileAsBytes);
    invoiceJpaRepository.save(HInvoice.builder()
        .id(invoice.getId())
        .fileId(fileInfo.getId())
        .comment(invoice.getComment())
        .ref(invoice.getRef())
        .title(invoice.getTitle())
        .idAccount(invoice.getAccount().getId())
        .sendingDate(invoice.getSendingDate())
        .toPayAt(invoice.getToPayAt())
        .status(invoice.getStatus())
        .build());
  }

  private String parseInvoiceTemplateToString(
      Invoice invoice, AccountHolder accountHolder, String logoFileId, String template) {
    TemplateEngine templateEngine = configureTemplate();
    Context context = configureContext(invoice, accountHolder, logoFileId);
    return templateEngine.process(template, context);
  }

  private byte[] generatePdf(Invoice invoice, AccountHolder accountHolder,
                             String logoFileId, String template) {
    ITextRenderer renderer = new ITextRenderer();
    loadStyle(renderer, invoice, accountHolder, logoFileId, template);
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
                         String logoFileId, String template) {
    //TODO: the built apps is a JAR so this does not work, change it
    String baseUrl = FileSystems.getDefault()
        .getPath("src/main", "resources", "templates")
        .toUri()
        .toString();
    renderer.setDocumentFromString(parseInvoiceTemplateToString(invoice, accountHolder,
            logoFileId, template),
        baseUrl);
  }

  private Context configureContext(Invoice invoice, AccountHolder accountHolder,
                                   String logoFileId) {
    Context context = new Context();
    Account account = invoice.getAccount();
    byte[] logoAsBytes =
        fileService.downloadFile(LOGO, account.getId(), logoFileId);
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

  //TODO: move this in the appropriate utils
  public byte[] generateQrCode(String textOrLink) {
    int width = 1000; //TODO-skip-for-now: make this size parameterizable
    int height = 1000; //TODO-skip-for-now: make this size parameterizable
    try {
      QRCodeWriter writer = new QRCodeWriter();
      BitMatrix bitMatrix = writer.encode(textOrLink, QR_CODE, width, height);
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      ImageIO.write(MatrixToImageWriter.toBufferedImage(bitMatrix), JPG_FORMAT_NAME, os);
      return os.toByteArray();
    } catch (WriterException | IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  //TODO: move this in the appropriate utils
  public String base64Image(byte[] image) {
    return Base64.getEncoder().encodeToString(image);
  }

}
