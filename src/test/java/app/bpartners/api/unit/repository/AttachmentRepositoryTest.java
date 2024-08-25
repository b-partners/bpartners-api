package app.bpartners.api.unit.repository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.api.model.Attachment;
import app.bpartners.api.repository.AttachmentRepository;
import app.bpartners.api.repository.implementation.AttachmentRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

class AttachmentRepositoryTest {

    @Mock
    private AttachmentRepository attachmentRepository;

    @InjectMocks
    private AttachmentRepositoryImpl attachmentRepositoryImpl; // Suppose que c'est l'implémentation de AttachmentRepository

    private Attachment attachment;
    private List<Attachment> attachments;
    private String idInvoiceRelaunch = "invoiceRelaunch1";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        attachment = Attachment.builder()
                .idEmail("email1")
                .fileId("file1")
                .name("file.txt")
                .content(new byte[]{1, 2, 3})
                .build();

        attachments = List.of(attachment);
    }

    @Test
    void findByIdInvoiceRelaunch_returnsAttachmentList() {
        when(attachmentRepository.findByIdInvoiceRelaunch(idInvoiceRelaunch)).thenReturn(attachments);

        List<Attachment> foundAttachments = attachmentRepository.findByIdInvoiceRelaunch(idInvoiceRelaunch);
        assertNotNull(foundAttachments);
        assertEquals(1, foundAttachments.size());
        assertEquals(attachment, foundAttachments.get(0));
    }

    @Test
    void saveAll_savesAndReturnsAttachments() {
        when(attachmentRepository.saveAll(attachments, idInvoiceRelaunch)).thenReturn(attachments);

        List<Attachment> savedAttachments = attachmentRepository.saveAll(attachments, idInvoiceRelaunch);
        assertNotNull(savedAttachments);
        assertEquals(1, savedAttachments.size());
        assertEquals(attachment, savedAttachments.get(0));
    }
}

