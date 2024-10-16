package app.bpartners.api.service;

import app.bpartners.api.model.AreaPictureAnnotation;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.repository.AreaPictureAnnotationRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AreaPictureAnnotationService {
  private final AreaPictureAnnotationRepository repository;

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
}
