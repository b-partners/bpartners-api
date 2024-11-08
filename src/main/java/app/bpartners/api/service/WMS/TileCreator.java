package app.bpartners.api.service.WMS;

import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.repository.ban.BanApi;
import app.bpartners.api.repository.ban.model.GeoPosition;
import app.bpartners.api.repository.google.geocode.GeoCodeApi;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.utils.GeoUtils;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import ch.qos.logback.classic.Level;
import com.google.maps.errors.ApiException;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import javax.mail.MessagingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;

@AllArgsConstructor
@Component
@Slf4j
public class TileCreator implements Function<AreaPicture, Tile> {
  private final BanApi banApi;
  private final SesService sesService;
  private final TemplateResolverEngine templateResolverEngine;
  private static final String AREA_PICTURE_NOT_FOUND_EMAIL_TEMPLATE =
      "area_picture_not_found_email";
  private final GeoCodeApi geoCodeApi;

  @Override
  public Tile apply(AreaPicture areaPicture) {
    AreaPicture refreshed = null;
    try {
      refreshed = refreshAreaPictureGeoPosition(areaPicture);
    } catch (InterruptedException e) {
      log.info("{}, Interrupted!, {}", Level.WARN, e);
      Thread.currentThread().interrupt();
    } catch (MessagingException | IOException | ApiException e) {
      throw new RuntimeException(e);
    }
    return Tile.from(refreshed);
  }

  private AreaPicture refreshAreaPictureGeoPosition(AreaPicture areaPicture)
      throws IOException, ApiException, MessagingException, InterruptedException {
    var geoPosition = geoCodeApi.searchGeoPositionFromAddress(areaPicture.getAddress());
    return updateAreaPictureGeoCoordinates(areaPicture, geoPosition);
  }

  public Context setAreaPictureContext(AreaPicture areaPicture, User user) {
    Context context = new Context();
    context.setVariable("user", user);
    context.setVariable("areaPicture", areaPicture);
    return context;
  }

  public AreaPicture updateAreaPictureGeoCoordinates(
      AreaPicture areaPicture, app.bpartners.api.endpoint.rest.model.GeoPosition geoPosition)
      throws MessagingException, IOException {
    NotFoundException notFoundException =
        new NotFoundException(
            "Given address "
                + areaPicture.getAddress()
                + " is not found."
                + " Check if it's not mal formed.");
    User user = AuthProvider.getAuthenticatedUser();
    if (geoPosition == null) {
      Context context = setAreaPictureContext(areaPicture, user);
      String emailBody =
          templateResolverEngine.parseTemplateResolver(
              AREA_PICTURE_NOT_FOUND_EMAIL_TEMPLATE, context);
      assert user != null;
      sesService.sendEmail(
          "sofiane@bpartners.app", user.getEmail(), "Bpartners - Adresse introuvable", emailBody);
      throw notFoundException;
    }
    areaPicture.setCurrentGeoPosition(geoPosition);
    areaPicture.setGeoPositions(List.of(geoPosition));

    return areaPicture;
  }

  private static app.bpartners.api.endpoint.rest.model.GeoPosition toDomain(
      GeoPosition geoPosition) {
    GeoUtils.Coordinate geoPositionCoordinates = geoPosition.getCoordinates();
    return new app.bpartners.api.endpoint.rest.model.GeoPosition()
        .longitude(geoPositionCoordinates.getLongitude())
        .latitude(geoPositionCoordinates.getLatitude())
        .score(geoPosition.getScore());
  }
}
