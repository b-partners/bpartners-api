package app.bpartners.api.model.mapper;

import app.bpartners.api.model.PriceReduction;
import app.bpartners.api.repository.jpa.model.HPriceReduction;
import org.springframework.stereotype.Component;

@Component
public class PriceReductionMapper {
  public PriceReduction toDomain(HPriceReduction entity) {
    return PriceReduction.builder()
        .id(entity.getId())
        .description(entity.getDescription())
        .value(entity.getValue())
        .build();
  }

  public HPriceReduction toEntity(PriceReduction reduction) {
    return HPriceReduction.builder()
        .id(reduction.getId())
        .description(reduction.getDescription())
        .value(reduction.getValue())
        .build();
  }
}
