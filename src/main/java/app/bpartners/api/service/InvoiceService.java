package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceCustomer;
import app.bpartners.api.model.PaymentRedirection;
import app.bpartners.api.model.Product;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.ProductRepository;
import app.bpartners.api.service.aws.S3Service;
import com.lowagie.text.DocumentException;
import java.io.ByteArrayOutputStream;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

@Service
@AllArgsConstructor
public class InvoiceService {
  private final InvoiceRepository repository;
  private final ProductRepository productRepository;
  private final PaymentInitiationService pis;

  private final S3Service s3Service;

  public Invoice getById(String invoiceId) {
    return refreshValues(repository.getById(invoiceId));
  }

  public Invoice crupdateInvoice(Invoice toCrupdate) {
    toCrupdate.setStatus(InvoiceStatus.CONFIRMED); //TODO: to be computed
    // s3Service.uploadFile(accountId, fileId, generateInvoicePdf(toCrupdate.getId()));
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
        //.fileId(TODO)
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

  private String parseInvoiceTemplateToString(String idInvoice) {
    //Invoice invoice = getById(idInvoice); TODO: use this with IT
    Invoice invoice = Invoice.builder()
        .invoiceCustomer(InvoiceCustomer.customerTemplateBuilder()
            .name("Client")
            .address("Mon adresse, Paris, France")
            .build())
        .build();

    ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
    templateResolver.setPrefix("/templates/");
    templateResolver.setSuffix(".html");
    templateResolver.setTemplateMode(TemplateMode.HTML);

    TemplateEngine templateEngine = new TemplateEngine();
    templateEngine.setTemplateResolver(templateResolver);

    Context context = new Context();
    context.setVariable("invoice", invoice);

    return templateEngine.process("invoice", context);
  }

  public byte[] generateInvoicePdf(String idInvoice) {
    ITextRenderer renderer = new ITextRenderer();
    renderer.setDocumentFromString(parseInvoiceTemplateToString(idInvoice));
    renderer.layout();

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try {
      renderer.createPDF(outputStream);
    } catch (DocumentException e) {
      throw new RuntimeException(e);
    }
    return outputStream.toByteArray();
  }
}
