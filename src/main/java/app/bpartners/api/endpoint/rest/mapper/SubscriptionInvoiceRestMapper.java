package app.bpartners.api.endpoint.rest.mapper;

import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE;
import static java.time.Instant.now;

import app.bpartners.api.endpoint.rest.model.PreSignedURL;
import app.bpartners.api.endpoint.rest.model.SubscriptionInvoice;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.payment.UserSubscriptionConf;
import app.bpartners.api.service.aws.S3Service;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionInvoiceRestMapper {
  private final InvoiceRestMapper invoiceRestMapper;
  private final S3Service s3Service;
  private final UserSubscriptionConf userSubscriptionConf;

  public SubscriptionInvoice toRest(Invoice invoice) {
    var urlExpirationSeconds = Duration.ofHours(1L).getSeconds();
    var userInvoiceOwner = userSubscriptionConf.getUserToCreditId();
    var presignedURL =
        s3Service.presignURL(INVOICE, invoice.getFileId(), userInvoiceOwner, urlExpirationSeconds);
    return new SubscriptionInvoice()
        .invoice(invoiceRestMapper.toRest(invoice))
        .fileUrl(
            new PreSignedURL()
                .value(presignedURL)
                .expirationDelay((int) urlExpirationSeconds)
                .updatedAt(now()));
  }
}
