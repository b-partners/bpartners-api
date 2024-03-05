package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.InvoiceSummary;
import app.bpartners.api.model.mapper.InvoiceSummaryMapper;
import app.bpartners.api.repository.InvoiceSummaryRepository;
import app.bpartners.api.repository.jpa.InvoiceSummaryJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoiceSummary;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class InvoiceSummaryServiceImpl implements InvoiceSummaryRepository {
  private final InvoiceSummaryJpaRepository jpaRepository;
  private final InvoiceSummaryMapper summaryMapper;

  @Override
  public List<InvoiceSummary> saveAll(List<InvoiceSummary> toSave) {
    List<HInvoiceSummary> savedSummary =
        jpaRepository.saveAll(toSave.stream().map(summaryMapper::toEntity).toList());
    return savedSummary.stream().map(summaryMapper::toDomain).toList();
  }

  @Override
  public InvoiceSummary findTopByIdUser(String idUser) {
    return summaryMapper.toDomain(jpaRepository.findTopByIdUserOrderByUpdatedAtDesc(idUser));
  }
}
