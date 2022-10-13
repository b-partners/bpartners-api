package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.PaymentRedirection;
import app.bpartners.api.model.Product;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.ProductRepository;
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
import javax.imageio.ImageIO;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.service.utils.FileInfoUtils.JPG_FORMAT_NAME;
import static com.google.zxing.BarcodeFormat.QR_CODE;

@Service
@AllArgsConstructor
public class InvoiceService {
  public static final String LOGO_JPEG = "logo.jpeg";
  private final InvoiceRepository repository;
  private final ProductRepository productRepository;
  private final PaymentInitiationService pis;
  private final FileService fileService;
  private final AccountHolderService holderService;

  public List<Invoice> getInvoices(String accountId, PageFromOne page, BoundedPageSize pageSize) {
    int pageValue = page.getValue() - 1;
    int pageSizeValue = pageSize.getValue();
    return repository.findAllByAccountId(accountId, pageValue, pageSizeValue)
        .stream()
        .map(this::refreshValues)
        .collect(Collectors.toUnmodifiableList());
  }

  public Invoice getById(String invoiceId) {
    return refreshValues(repository.getById(invoiceId));
  }

  public Invoice crupdateInvoice(Invoice toCrupdate) {
    toCrupdate.setStatus(InvoiceStatus.CONFIRMED);
    //TODO: uncomment when localstak is set
    //Invoice refreshedInvoice = refreshValues(repository.crupdate(toCrupdate));
    //fileService.uploadFile(refreshedInvoice.getAccount().getId(),
    //refreshedInvoice.getFileId(), generateInvoicePdf(refreshedInvoice));
    return refreshValues(repository.crupdate(toCrupdate));
  }

  private Invoice refreshValues(Invoice invoice) {
    List<Product> products = invoice.getProducts();
    if (products.isEmpty()) {
      products =
          productRepository.findByIdInvoice(invoice.getId());
    }
    Invoice initializedInvoice = Invoice.builder()
        .id(invoice.getId())
        .ref(invoice.getRef())
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
    PaymentRedirection paymentRedirection = pis.initiateInvoicePayment(initializedInvoice);
    initializedInvoice.setPaymentUrl(paymentRedirection.getRedirectUrl());
    return initializedInvoice;
  }

  private int computeTotalVat(List<Product> products) {
    if (products == null) {
      return 0;
    }
    return products.stream()
        .mapToInt(Product::getTotalVat)
        .sum();
  }

  private int computeTotalPriceWithoutVat(List<Product> products) {
    if (products == null) {
      return 0;
    }
    return products.stream()
        .mapToInt(Product::getTotalWithoutVat)
        .sum();
  }

  private int computeTotalPriceWithVat(List<Product> products) {
    if (products == null) {
      return 0;
    }
    return products.stream()
        .mapToInt(Product::getTotalPriceWithVat)
        .sum();
  }

  private String parseInvoiceTemplateToString(Invoice invoice) {
    TemplateEngine templateEngine = configureTemplate();
    Context context = configureContext(invoice);
    return templateEngine.process("invoice", context);
  }

  public byte[] generateInvoicePdf(Invoice invoice) {
    ITextRenderer renderer = new ITextRenderer();
    loadStyle(renderer, invoice);
    renderer.layout();

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try {
      renderer.createPDF(outputStream);
    } catch (DocumentException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
    return outputStream.toByteArray();
  }

  private void loadStyle(ITextRenderer renderer, Invoice invoice) {
    String baseUrl = FileSystems.getDefault()
        .getPath("src/main", "resources", "templates")
        .toUri()
        .toString();
    renderer.setDocumentFromString(parseInvoiceTemplateToString(invoice), baseUrl);
  }

  private Context configureContext(Invoice invoice) {
    Account account = invoice.getAccount();
    AccountHolder accountHolder = holderService.getAccountHolderByAccountId(account.getId());
    Context context = new Context();
    byte[] qrCodeBytes = generateQrCode(invoice.getPaymentUrl());
    byte[] logoBytes = fileService.downloadFile(account.getId(), LOGO_JPEG);
    context.setVariable("invoice", invoice);
    context.setVariable("qrcode", base64Image(qrCodeBytes));
    context.setVariable("logo", base64Image(logoBytes));
    context.setVariable("account", account);
    context.setVariable("accountHolder", accountHolder);
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

  private String base64Image(byte[] image) {
    return Base64.getEncoder().encodeToString(image);
  }
}