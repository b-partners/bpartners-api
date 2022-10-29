package app.bpartners.api.service;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.TypedFileUploaded;
import app.bpartners.api.endpoint.event.model.TypedMailSent;
import app.bpartners.api.endpoint.rest.model.FileType;
import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.endpoint.rest.security.principal.PrincipalProvider;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.PaymentRedirection;
import app.bpartners.api.model.Product;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.validator.InvoiceValidator;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.ProductRepository;
import app.bpartners.api.service.aws.SesService;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.DocumentException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apfloat.Aprational;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.DRAFT;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PROPOSAL;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.service.utils.FileInfoUtils.JPG_FORMAT_NAME;
import static app.bpartners.api.service.utils.FileInfoUtils.PDF_EXTENSION;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;
import static app.bpartners.api.service.utils.FractionUtils.toAprational;
import static com.google.zxing.BarcodeFormat.QR_CODE;

@Service
@AllArgsConstructor
@Slf4j
public class InvoiceService {
  private final InvoiceRepository repository;
  private final ProductRepository productRepository;
  private final PaymentInitiationService pis;
  private final FileService fileService;
  private final AccountHolderService holderService;
  private final PrincipalProvider auth;
  private final InvoiceValidator validator;
  private final SesService sesService;
  private final EventProducer eventProducer;

  public List<Invoice> getInvoices(String accountId, PageFromOne page, BoundedPageSize pageSize,
                                   InvoiceStatus status) {
    int pageValue = page.getValue() - 1;
    int pageSizeValue = pageSize.getValue();
    List<Invoice> invoices = repository.findAllByAccountId(accountId, pageValue, pageSizeValue);
    if (status != null) {
      invoices = repository.findAllByAccountIdAndStatus(accountId, status,
          pageValue,
          pageSizeValue);
    }
    return invoices.stream()
        .map(this::refreshValues)
        .collect(Collectors.toUnmodifiableList());
  }

  public Invoice getById(String invoiceId) {
    return refreshValues(repository.getById(invoiceId));
  }

  public Invoice crupdateInvoice(Invoice toCrupdate) {
    validator.accept(toCrupdate);

    Invoice refreshedInvoice = refreshValues(repository.crupdate(toCrupdate));

    byte[] pdfAsBytes;
    if (refreshedInvoice.getStatus().equals(CONFIRMED)) {
      pdfAsBytes = generateInvoicePdf(refreshedInvoice);
    } else {
      pdfAsBytes = generateDraftPdf(refreshedInvoice);
    }

    eventProducer.accept(List.of(getFileUploadedEvent(refreshedInvoice, pdfAsBytes)));

    return refreshedInvoice;
  }

  public void sendInvoice(
      String invoiceId, String subject, String emailMessage) {
    Invoice invoice = getById(invoiceId);
    byte[] fileAsBytes;
    if (invoice.getStatus().equals(CONFIRMED)) {
      fileAsBytes = generateInvoicePdf(invoice);
    } else {
      fileAsBytes = generateDraftPdf(invoice);
    }
    if (!invoice.getStatus().equals(DRAFT)) {
      eventProducer.accept(List.of(getMailSentEvent(invoice, subject, emailMessage, fileAsBytes)));
    } else {
      throw new BadRequestException(
          "Invoice." + invoiceId + " can not be sent because status is " + invoice.getStatus());
    }
  }

  public Invoice persistFileId(String invoiceId) {
    Invoice invoice = getById(invoiceId);
    String fileId = invoice.getRef() + PDF_EXTENSION; //TODO: Other natural ID or UUID ?
    invoice.setFileId(fileId);
    return repository.crupdate(invoice);
  }

  private Invoice refreshValues(Invoice invoice) {
    List<Product> products = invoice.getProducts();
    if (products.isEmpty()) {
      products =
          productRepository.findByIdInvoice(invoice.getId());
    }
    Invoice initializedInvoice = Invoice.builder()
        .id(invoice.getId())
        .comment(invoice.getComment())
        .updatedAt(invoice.getUpdatedAt())
        .title(invoice.getTitle())
        .invoiceCustomer(invoice.getInvoiceCustomer())
        .account(invoice.getAccount())
        .status(invoice.getStatus())
        .totalVat(computeTotalVat(products))
        .totalPriceWithoutVat(computeTotalPriceWithoutVat(products))
        .totalPriceWithVat(computeTotalPriceWithVat(products))
        .products(products)
        .toPayAt(invoice.getToPayAt())
        .sendingDate(invoice.getSendingDate())
        .build();
    if (invoice.getStatus().equals(CONFIRMED) || invoice.getStatus().equals(PAID)) {
      PaymentRedirection paymentRedirection = pis.initiateInvoicePayment(initializedInvoice);
      initializedInvoice.setPaymentUrl(paymentRedirection.getRedirectUrl());
      initializedInvoice.setRef(invoice.getRef());
    } else {
      initializedInvoice.setPaymentUrl(null);
      initializedInvoice.setRef(invoice.getRef() + "-TMP");
    }
    return initializedInvoice;
  }

  public List<Invoice> getAllInvoices() {
    return repository.findAll();
  }

  private Fraction computeTotalVat(List<Product> products) {
    return computeSum(products, products.stream()
        .map(Product::getTotalVat));
  }

  private Fraction computeTotalPriceWithoutVat(List<Product> products) {
    return computeSum(products, products.stream()
        .map(Product::getTotalWithoutVat));
  }

  private Fraction computeTotalPriceWithVat(List<Product> products) {
    return computeSum(products, products.stream()
        .map(Product::getTotalPriceWithVat));
  }

  private Fraction computeSum(List<Product> products, Stream<Fraction> fractionStream) {
    if (products == null) {
      return new Fraction();
    }
    Aprational aprational = fractionStream
        .map(a -> toAprational(a.getNumerator(), a.getDenominator()))
        .reduce(new Aprational(0), Aprational::add);
    return parseFraction(aprational);
  }

  private String parseInvoiceTemplateToString(Invoice invoice, String template) {
    TemplateEngine templateEngine = configureTemplate();
    Context context = configureContext(invoice);
    return templateEngine.process(template, context);
  }

  public byte[] generateInvoicePdf(Invoice invoice) {
    ITextRenderer renderer = new ITextRenderer();
    loadStyle(renderer, invoice, "invoice");
    renderer.layout();

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try {
      renderer.createPDF(outputStream);
    } catch (DocumentException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
    return outputStream.toByteArray();
  }

  public byte[] generateDraftPdf(Invoice invoice) {
    ITextRenderer renderer = new ITextRenderer();
    loadStyle(renderer, invoice, "draft");
    renderer.layout();

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try {
      renderer.createPDF(outputStream);
    } catch (DocumentException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
    return outputStream.toByteArray();
  }

  private void loadStyle(ITextRenderer renderer, Invoice invoice, String template) {
    String baseUrl = FileSystems.getDefault()
        .getPath("src/main", "resources", "templates")
        .toUri()
        .toString();
    renderer.setDocumentFromString(parseInvoiceTemplateToString(invoice, template), baseUrl);
  }

  private Context configureContext(Invoice invoice) {
    Context context = new Context();
    Account account = invoice.getAccount();
    AccountHolder accountHolder = holderService.getAccountHolderByAccountId(account.getId());
    byte[] logoBytes =
        fileService.downloadFile(FileType.LOGO, account.getId(), userLogoFileId());
    context.setVariable("invoice", invoice);
    context.setVariable("logo", base64Image(logoBytes));
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

  private byte[] generateQrCode(String link) {
    int width = 1000; //TODO: make this size parameterizable
    int height = 1000; //TODO: make this size parameterizable
    try {
      QRCodeWriter writer = new QRCodeWriter();
      BitMatrix bitMatrix = writer.encode(link, QR_CODE, width, height);
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      ImageIO.write(MatrixToImageWriter.toBufferedImage(bitMatrix), JPG_FORMAT_NAME, os);
      return os.toByteArray();
    } catch (WriterException | IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  private String userLogoFileId() {
    return ((Principal) auth.getAuthentication().getPrincipal()).getUser().getLogoFileId();
  }

  private String base64Image(byte[] image) {
    return Base64.getEncoder().encodeToString(image);
  }

  private TypedMailSent getMailSentEvent(
      Invoice invoice, String subject, String emailMessage, byte[] pdf) {
    if (!invoice.getStatus().equals(DRAFT)) {
      return toTypedEvent(invoice, subject, emailMessage, pdf);
    }
    throw new BadRequestException("Invoice." + invoice.getId() + " can not be sent because "
        + "status is " + invoice.getStatus());
  }

  private TypedFileUploaded getFileUploadedEvent(Invoice invoice, byte[] pdfAsBytes) {
    return fileService.toTypedEvent(INVOICE, invoice.getAccount().getId(), invoice.getFileId(),
        pdfAsBytes, invoice.getId());
  }

  //TODO: set it again in template resolver when the load style baseUrl is set
  private String emailBody(String emailMessage, Invoice invoice) {
    AccountHolder accountHolder =
        holderService.getAccountHolderByAccountId(invoice.getAccount().getId());
    return "<html>\n"
        + "    <body style=\"font-family: 'Gill Sans'\">\n"
        + "        <h2 style=color:#8d2158;>" + accountHolder.getName() + "</h2>\n"
        + emailMessage
        + "        <p>Bien à vous et merci pour votre confiance.</p>\n"
        + "    </body>\n"
        + "</html>";
  }

  //TODO: persist the default email message and get it instead
  private String defaultEmailMessage(String type, Invoice invoice) {
    return "        <p>Bonjour,</p>\n"
        + "        <p>\n"
        + "            Retrouvez-ci joint votre " + type + " enregistré à la référence "
        + invoice.getRef() + "\n"
        + "        </p>\n";
  }

  private TypedMailSent toTypedEvent(Invoice invoice,
                                     String subject, String emailMessage, byte[] pdf) {
    String type = getStatusValue(invoice.getStatus());
    if (subject == null) {
      //TODO: check if the invoice has already been relaunched then change this
      subject = type + " " + invoice.getRef();
    }
    if (emailMessage == null) {
      emailMessage = defaultEmailMessage(type.toLowerCase(), invoice);
    }
    String recipient = invoice.getInvoiceCustomer().getEmail();
    return sesService.toTypedEvent(
        recipient, subject, emailBody(emailMessage, invoice), subject + PDF_EXTENSION, pdf);
  }

  private String getStatusValue(InvoiceStatus status) {
    if (status.equals(PROPOSAL) || status.equals(DRAFT)) {
      return "Devis";
    }
    if (status.equals(CONFIRMED) || status.equals(PAID)) {
      return "Facture";
    }
    throw new BadRequestException("Unknown status : " + status);
  }
}