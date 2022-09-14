package app.bpartners.api.service;

import app.bpartners.api.model.Product;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ProductService {

  public Product resetProductRelatedInfo(Product product) {
    int grossAmount = (product.getUnitPrice() * product.getQuantity());
    int totalAmount = grossAmount * (100 - product.getReduction().getValue());
    return Product.builder()
        .id(product.getId())
        .description(product.getDescription())
        .invoice(product.getInvoice())
        .reduction(product.getReduction())
        .quantity(product.getQuantity())
        .unitPrice(product.getUnitPrice())
        .grossAmount(grossAmount)
        .totalAmount(totalAmount)
        .build();
  }

  public int computeGrossAmount(List<Product> products) {
    return products.stream()
        .mapToInt(Product::getTotalAmount)
        .sum();
  }
}
