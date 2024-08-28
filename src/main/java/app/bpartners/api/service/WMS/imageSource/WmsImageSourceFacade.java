package app.bpartners.api.service.WMS.imageSource;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.file.FileDownloader;
import app.bpartners.api.mail.Email;
import app.bpartners.api.mail.Mailer;
import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.service.WMS.AreaPictureMapLayerService;
import app.bpartners.api.service.WMS.Tile;
import app.bpartners.api.service.WMS.imageSource.exception.BlankImageException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.io.File;
import java.net.URI;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@Slf4j
final class WmsImageSourceFacade extends AbstractWmsImageSource {
  private final GeoserverImageSource geoserverImageSource;
  private final IGNGeoserverImageSource ignGeoserverImageSource;
  private final AreaPictureMapLayerService areaPictureMapLayerService;
  private final TileExtenderImageSource tileExtenderImageSource;
  private final ImageValidator imageValidator;
  private final Mailer mailer;

  private WmsImageSourceFacade(
      FileDownloader fileDownloader,
      GeoserverImageSource geoserverImageSource,
      IGNGeoserverImageSource ignGeoserverImageSource,
      AreaPictureMapLayerService areaPictureMapLayerService,
      TileExtenderImageSource tileExtenderImageSource,
      ImageValidator imageValidator,
      Mailer mailer) {
    super(fileDownloader);
    this.geoserverImageSource = geoserverImageSource;
    this.ignGeoserverImageSource = ignGeoserverImageSource;
    this.areaPictureMapLayerService = areaPictureMapLayerService;
    this.tileExtenderImageSource = tileExtenderImageSource;
    this.imageValidator = imageValidator;
    this.mailer = mailer;
  }

  @Override
  protected URI getURI(Tile tile, AreaPictureMapLayer areaPictureMapLayer) {
    return geoserverImageSource.getURI(tile, areaPictureMapLayer);
  }

  @Override
  @SneakyThrows
  public File downloadImage(AreaPicture areaPicture) {
    if (areaPicture.isExtended()) {
      return tileExtenderImageSource.downloadImage(areaPicture);
    }
    return cascadeRetryImageDownloadUntilValid(geoserverImageSource, areaPicture, 0);
  }

  private File cascadeRetryImageDownloadUntilValid(
      WmsImageSource wmsImageSource,
      AreaPicture areaPicture,
      @Range(from = 0, to = 2) int iteration)
      throws AddressException {
    WmsImageSource alternativeSource;
    AreaPictureMapLayer alternativeAreaPictureMapLayer;
    if (iteration == 0) {
      alternativeSource = wmsImageSource;
      alternativeAreaPictureMapLayer = areaPicture.getCurrentLayer();
    } else if (iteration == 1) {
      alternativeSource = ignGeoserverImageSource;
      alternativeAreaPictureMapLayer = areaPictureMapLayerService.getDefaultIGNLayer();
    } else {
      var toSend = getEmail(areaPicture);
      mailer.accept(toSend);
      throw new ApiException(
          SERVER_EXCEPTION, "could not find any server for " + areaPicture.describe());
    }
    try {
      areaPicture.setCurrentLayer(alternativeAreaPictureMapLayer);
      var image = alternativeSource.downloadImage(areaPicture);
      imageValidator.accept(image);
      return image;
    } catch (ApiException | BlankImageException e) {
      log.info(
          "could not resolve {} , due to exception {}", areaPicture.describe(), e.getMessage());
      return cascadeRetryImageDownloadUntilValid(alternativeSource, areaPicture, ++iteration);
    }
  }

  private static @NotNull Email getEmail(AreaPicture areaPicture) throws AddressException {
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

  @Override
  public boolean supports(AreaPicture areaPicture) {
    return true;
  }
}
