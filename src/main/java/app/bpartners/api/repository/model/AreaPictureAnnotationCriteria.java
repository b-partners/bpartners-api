package app.bpartners.api.repository.model;

import java.time.Instant;
import lombok.Builder;

@Builder(toBuilder = true)
public record AreaPictureAnnotationCriteria(
    String idUser,
    String idAreaPicture,
    Boolean isDraft,
    Instant creationFrom,
    Instant creationTo,
    Integer page,
    Integer pageSize) {}
