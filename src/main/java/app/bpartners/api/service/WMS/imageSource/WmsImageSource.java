package app.bpartners.api.service.WMS.imageSource;

import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.AreaPicture;
import java.io.File;

public interface WmsImageSource {
  File downloadImage(AreaPicture areaPicture, AccountHolder accountHolder);

  File downloadImage(AreaPicture areaPicture);

  boolean supports(AreaPicture areaPicture);
}
