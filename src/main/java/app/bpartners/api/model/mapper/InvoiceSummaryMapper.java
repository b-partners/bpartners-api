package app.bpartners.api.model.mapper;

import static java.util.UUID.randomUUID;

import app.bpartners.api.model.InvoiceSummary;
import app.bpartners.api.model.Money;
import app.bpartners.api.repository.jpa.model.HInvoiceSummary;
import org.springframework.stereotype.Component;

@Component
public class InvoiceSummaryMapper {
  public InvoiceSummary toDomain(HInvoiceSummary entity) {
    return entity == null
        ? null
        : InvoiceSummary.builder()
            .paid(
                InvoiceSummary.InvoiceSummaryContent.builder()
                    .amount(Money.fromMajor(entity.getPaidAmount()))
                    .build())
            .unpaid(
                InvoiceSummary.InvoiceSummaryContent.builder()
                    .amount(Money.fromMajor(entity.getUnpaidAmount()))
                    .build())
            .proposal(
                InvoiceSummary.InvoiceSummaryContent.builder()
                    .amount(Money.fromMajor(entity.getProposalAmount()))
                    .build())
            .updatedAt(entity.getUpdatedAt())
            .build();
  }

  public HInvoiceSummary toEntity(InvoiceSummary domain) {
    return HInvoiceSummary.builder()
        .id(String.valueOf(randomUUID()))
        .updatedAt(domain.getUpdatedAt())
        .idUser(domain.getIdUser())
        .proposalAmount(domain.getProposal().getAmount().stringValue())
        .unpaidAmount(domain.getUnpaid().getAmount().stringValue())
        .paidAmount(domain.getPaid().getAmount().stringValue())
        .build();
  }
}
