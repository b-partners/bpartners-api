package app.bpartners.api.service.WMS;

import static app.bpartners.api.repository.ban.BanApi.getHighestFeatGeoPosition;

import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.mail.Email;
import app.bpartners.api.mail.Mailer;
import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.repository.ban.BanApi;
import app.bpartners.api.repository.ban.model.GeoPosition;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.utils.GeoUtils;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import javax.mail.MessagingException;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class TileCreator implements Function<AreaPicture, Tile> {
  private final BanApi banApi;
  private final Mailer mailer;
  private final SesService sesService;

  @Override
  public Tile apply(AreaPicture areaPicture) {
    AreaPicture refreshed = null;
    try {
      refreshed = refreshAreaPictureGeoPosition(areaPicture);
    } catch (AddressException | MessagingException | IOException e) {
      throw new RuntimeException(e);
    }
    return Tile.from(refreshed);
  }

  private AreaPicture refreshAreaPictureGeoPosition(AreaPicture areaPicture)
      throws AddressException, MessagingException, IOException {
    var geoFeatures = banApi.searchMultiplePos(areaPicture.getAddress());
    var geoPositions = geoFeatures.getFeatures().stream().map(BanApi::mapToGeoPosition).toList();
    return updateAreaPictureGeoCoordinates(areaPicture, geoPositions);
  }

  public static @NotNull Email getEmail(AreaPicture areaPicture) throws AddressException {
    User user = AuthProvider.getAuthenticatedUser();
    var toInternetAddress = new InternetAddress("hei.dinasoa@gmail.com");
    return new Email(
        toInternetAddress,
        List.of(),
        List.of(),
        "Bpartners - Adresse introuvable",
        "<p> Adresse: <strong>"
            + areaPicture.getAddress()
            + "</strong>"
            + "Client: <strong>"
            + user.getName()
            + "</strong> "
            + "Email du client: <strong>"
            + user.getEmail()
            + "</strong> "
            + "Client id: <strong>"
            + user.getId()
            + "</strong> "
            + "</p>",
        List.of());
  }

  public AreaPicture updateAreaPictureGeoCoordinates(
      AreaPicture areaPicture, List<GeoPosition> geoPositions)
      throws AddressException, MessagingException, IOException {
    NotFoundException notFoundException =
        new NotFoundException(
            "Given address "
                + areaPicture.getAddress()
                + " is not found."
                + " Check if it's not mal formed.");
    User user = AuthProvider.getAuthenticatedUser();

    var highestFeatGeoPosition = getHighestFeatGeoPosition(geoPositions);
    if (highestFeatGeoPosition.isEmpty()) {
      String body =
          "<p> Adresse: <strong>"
              + areaPicture.getAddress()
              + "</strong>"
              + "Client: <strong>"
              + user.getName()
              + "</strong> "
              + "Email du client: <strong>"
              + user.getEmail()
              + "</strong> "
              + "Client id: <strong>"
              + user.getId()
              + "</strong> "
              + "</p>";
      sesService.sendEmail(
          "hei.dinasoa@gmail.com", user.getEmail(), "Bpartners - Adresse introuvable", body);
      throw notFoundException;
    }
    areaPicture.setCurrentGeoPosition(toDomain(highestFeatGeoPosition.get()));
    areaPicture.setGeoPositions(geoPositions.stream().map(TileCreator::toDomain).toList());

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
