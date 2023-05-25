package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.model.ProductStatus;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.InvoiceProduct;
import app.bpartners.api.repository.jpa.model.HInvoiceProduct;
import org.springframework.stereotype.Component;

import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

@Component
public class InvoiceProductMapper {
  public InvoiceProduct toDomain(HInvoiceProduct entity) {
    Fraction unitPrice = parseFraction(entity.getUnitPrice());
    Fraction vatPercent = parseFraction(entity.getVatPercent());
    return InvoiceProduct.builder()
        .id(entity.getId())
        .idInvoice(entity.getIdInvoice())
        .description(entity.getDescription())
        .quantity(entity.getQuantity())
        .unitPrice(unitPrice)
        .vatPercent(vatPercent)
        .status(entity.getStatus() == null ? ProductStatus.ENABLED : entity.getStatus())
        .build();
  }

  public HInvoiceProduct toEntity(InvoiceProduct domain) {
    return HInvoiceProduct.builder()
        .id(domain.getId())
        .idInvoice(domain.getIdInvoice())
        .description(domain.getDescription())
        .quantity(domain.getQuantity())
        .unitPrice(domain.getUnitPrice().toString())
        .vatPercent(domain.getVatPercent().toString())
        .status(domain.getStatus() == null ? ProductStatus.ENABLED : domain.getStatus())
        .build();
  }
}
