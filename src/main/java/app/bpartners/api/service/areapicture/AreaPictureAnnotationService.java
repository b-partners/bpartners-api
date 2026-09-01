package app.bpartners.api.service.areapicture;

import static app.bpartners.api.endpoint.rest.model.FileType.ATTACHMENT;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;

import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation;
import app.bpartners.api.endpoint.rest.model.PreSignedURL;
import app.bpartners.api.file.FileWriter;
import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.AreaPictureAnnotation;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.prospect.Prospect;
import app.bpartners.api.repository.AreaPictureAnnotationRepository;
import app.bpartners.api.repository.ProspectRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.model.AreaPictureAnnotationCriteria;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationPDFProcessor;
import app.bpartners.api.service.aws.S3Service;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
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
  private final UserRepository userRepository;
  private final AreaPictureService areaPictureService;
  private final ProspectRepository prospectRepository;

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

  public List<AreaPictureAnnotation> findAllByCriteria(
      String idUser,
      String idAreaPicture,
      String prospectName,
      String address,
      Instant creationFrom,
      Instant creationTo,
      PageFromOne page,
      BoundedPageSize pageSize) {
    if (prospectName != null || address != null) {
      var areaPicture = areaPictureService.findBy(idUser, idAreaPicture);
      if (address != null && !containsIgnoreCase(areaPicture.getAddress(), address)) {
        return List.of();
      }
      if (prospectName != null) {
        var idProspect = areaPicture.getIdProspect();
        if (idProspect == null
            || !containsIgnoreCase(
                prospectRepository.getById(idProspect).getName(), prospectName)) {
          return List.of();
        }
      }
    }
    return repository.findAllByCriteria(
        AreaPictureAnnotationCriteria.builder()
            .idUser(idUser)
            .idAreaPicture(idAreaPicture)
            .isDraft(true)
            .creationFrom(creationFrom)
            .creationTo(creationTo)
            .page(page.getValue() - 1)
            .pageSize(pageSize.getValue())
            .build());
  }

  public List<AreaPictureAnnotation> findAllDraftByAccountId(
      String idUser,
      String prospectName,
      String address,
      Instant creationFrom,
      Instant creationTo,
      PageFromOne page,
      BoundedPageSize pageSize) {
    List<String> idAreaPictureIds = null;
    if (prospectName != null || address != null) {
      var areaPictures =
          address != null
              ? areaPictureService.findAllByAddress(idUser, address)
              : areaPictureService.findAllByIdUser(idUser);
      if (prospectName != null) {
        var idProspects =
            areaPictures.stream().map(AreaPicture::getIdProspect).filter(Objects::nonNull).toList();
        var matchingIdProspects =
            prospectRepository.findAllByIds(idProspects).stream()
                .filter(prospect -> containsIgnoreCase(prospect.getName(), prospectName))
                .map(Prospect::getId)
                .collect(Collectors.toSet());
        idAreaPictureIds =
            areaPictures.stream()
                .filter(
                    areaPicture ->
                        areaPicture.getIdProspect() != null
                            && matchingIdProspects.contains(areaPicture.getIdProspect()))
                .map(AreaPicture::getId)
                .toList();
      } else {
        idAreaPictureIds = areaPictures.stream().map(AreaPicture::getId).toList();
      }
      if (idAreaPictureIds.isEmpty()) {
        return List.of();
      }
    }
    return repository.findAllByCriteria(
        AreaPictureAnnotationCriteria.builder()
            .idUser(idUser)
            .idAreaPictureIds(idAreaPictureIds)
            .isDraft(true)
            .creationFrom(creationFrom)
            .creationTo(creationTo)
            .page(page.getValue() - 1)
            .pageSize(pageSize.getValue())
            .build());
  }

  public PreSignedURL exportAreaPictureAnnotationToPdf(
      String userId, ExportAreaPictureAnnotation annotation, byte[] globalImage3D) {
    try {
      var user = userRepository.getById(userId);
      var generatedPDF =
          exportAreaPictureAnnotationPDFProcessor.process(user, annotation, globalImage3D);

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
