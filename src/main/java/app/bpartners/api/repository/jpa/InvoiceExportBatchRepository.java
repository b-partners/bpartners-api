package app.bpartners.api.repository.jpa;

import app.bpartners.api.model.InvoiceExportBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceExportBatchRepository extends JpaRepository<InvoiceExportBatch, String> {}
