package app.bpartners.api.service.wms.imageSource;

import app.bpartners.api.model.AreaPicture;
import java.io.File;

public interface WmsImageSource {
  File downloadImage(AreaPicture areaPicture);

  boolean supports(AreaPicture areaPicture);
}
