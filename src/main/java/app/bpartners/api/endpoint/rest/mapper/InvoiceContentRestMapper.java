package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.InvoiceContent;
import app.bpartners.api.endpoint.rest.model.PriceReduction;
import org.springframework.stereotype.Component;

@Component
public class InvoiceContentRestMapper {
  public InvoiceContent toRest(app.bpartners.api.model.InvoiceContent domain) {
    return new InvoiceContent()
        .description(domain.getDescription())
        .quantity(domain.getQuantity())
        .price(domain.getPrice())
        .reduction(new PriceReduction()
            .description(domain.getReduction().getDescription())
            .value(domain.getReduction().getValue()))
        .totalPrice(domain.getPrice());
  }

  public app.bpartners.api.model.InvoiceContent toDomain(InvoiceContent invoiceContent) {
    return app.bpartners.api.model.InvoiceContent.builder()
        .description(invoiceContent.getDescription())
        .quantity(invoiceContent.getQuantity())
        .price(invoiceContent.getPrice())
        .reduction(app.bpartners.api.model.PriceReduction.builder()
            .description(invoiceContent.getReduction().getDescription())
            .value(invoiceContent.getReduction().getValue())
            .build())
        .build();
  }
}
