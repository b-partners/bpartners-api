package app.bpartners.api.mail;

import app.bpartners.api.PojaGenerated;
import jakarta.mail.internet.InternetAddress;
import java.io.File;
import java.util.List;

@PojaGenerated
@SuppressWarnings("all")
public record Email(
    InternetAddress to,
    List<InternetAddress> cc,
    List<InternetAddress> bcc,
    String subject,
    String htmlBody,
    List<File> attachments) {}
