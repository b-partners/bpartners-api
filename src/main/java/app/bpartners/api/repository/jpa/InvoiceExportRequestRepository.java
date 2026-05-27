package app.bpartners.api.repository.jpa;

import app.bpartners.api.model.InvoiceExportRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceExportRequestRepository
    extends JpaRepository<InvoiceExportRequest, String> {}
