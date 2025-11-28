package app.bpartners.api.service.areapicture;

import static app.bpartners.api.endpoint.rest.model.FileType.ATTACHMENT;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation;
import app.bpartners.api.endpoint.rest.model.PreSignedURL;
import app.bpartners.api.file.FileWriter;
import app.bpartners.api.model.AreaPictureAnnotation;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.repository.AreaPictureAnnotationRepository;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationPDFProcessor;
import app.bpartners.api.service.aws.S3Service;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class AreaPictureAnnotationService {
  private static final String PDF_EXTENSION = ".pdf";
  private static final int ONE_HOUR_IN_SECONDS = 3600;

  private final FileWriter fileWriter;
  private final S3Service s3Service;
  private final AreaPictureAnnotationRepository repository;
  private final ExportAreaPictureAnnotationPDFProcessor exportAreaPictureAnnotationPDFProcessor;

  public AreaPictureAnnotation save(AreaPictureAnnotation areaPictureAnnotation) {
    return repository.save(areaPictureAnnotation);
  }

  private List<AreaPictureAnnotation> findAllBy(
      String idUser,
      String idAreaPicture,
      Boolean isDraft,
      PageFromOne page,
      BoundedPageSize pageSize) {
    return repository.findAllBy(
        idUser,
        idAreaPicture,
        isDraft,
        PageRequest.of(
            page.getValue() - 1,
            pageSize.getValue(),
            Sort.by(Sort.Order.desc("creationDatetime"))));
  }

  public AreaPictureAnnotation findBy(String idUser, String idAreaPicture, String id) {
    return repository
        .findBy(idUser, idAreaPicture, id)
        .orElseThrow(() -> new NotFoundException("AreaPictureAnnotation.Id = " + id + "not found"));
  }

  public List<AreaPictureAnnotation> findAllCompleted(
      String idUser, String idAreaPicture, PageFromOne page, BoundedPageSize pageSize) {
    final var isDraft = false;
    return findAllBy(idUser, idAreaPicture, isDraft, page, pageSize);
  }

  public List<AreaPictureAnnotation> findAllDraftByAccountIdAndAreaPictureId(
      String idUser, String idAreaPicture, PageFromOne page, BoundedPageSize pageSize) {
    final var isDraft = true;
    return findAllBy(idUser, idAreaPicture, isDraft, page, pageSize);
  }

  public List<AreaPictureAnnotation> findAllDraftByAccountId(
      String idUser, PageFromOne page, BoundedPageSize pageSize) {
    final var isDraft = true;
    return repository.findAllByIsDraftAndAccountId(
        idUser,
        isDraft,
        PageRequest.of(
            page.getValue() - 1,
            pageSize.getValue(),
            Sort.by(Sort.Order.desc("creationDatetime"))));
  }

  public PreSignedURL exportAreaPictureAnnotationToPdf(
      String userId, ExportAreaPictureAnnotation annotation, byte[] globalImage3D) {
    try {
      var generatedPDF = exportAreaPictureAnnotationPDFProcessor.process(annotation, globalImage3D);

      var fileToUpload = fileWriter.apply(generatedPDF, null);
      var fileId = "Rapport_d_analyse_" + randomUUID() + PDF_EXTENSION;

      s3Service.uploadFile(ATTACHMENT, fileId, userId, fileToUpload);
      var fileUrl = s3Service.presignURL(ATTACHMENT, fileId, userId, (long) ONE_HOUR_IN_SECONDS);

      return new PreSignedURL()
          .value(fileUrl)
          .expirationDelay(ONE_HOUR_IN_SECONDS)
          .updatedAt(Instant.now());
    } catch (Exception e) {
      log.error(e.getMessage());
      throw new RuntimeException(e.getMessage());
    }
  }
}
