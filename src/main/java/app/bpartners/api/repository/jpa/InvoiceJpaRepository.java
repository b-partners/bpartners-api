package app.bpartners.api.repository.jpa;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.repository.jpa.model.HInvoice;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceJpaRepository extends JpaRepository<HInvoice, String> {

  //TODO: change this to have a more readable function
  // The principal feats are : filter by account and status, ordered by created datetime desc
  List<HInvoice> findAllByIdAccountAndStatusOrderByCreatedDatetimeDesc(
      String idAccount,
      InvoiceStatus status,
      Pageable pageable);


  List<HInvoice> findAllByIdAccountOrderByCreatedDatetimeDesc(
      String idAccount, Pageable pageable);

  Optional<HInvoice> findByIdAccountAndRef(String idAccount, String ref);

  List<HInvoice> findAllByToBeRelaunched(boolean toBeRelaunched);
}
