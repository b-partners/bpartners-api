package app.bpartners.api.file;

import java.io.File;
import java.io.Serializable;
import java.net.URI;

public sealed interface FileDownloader permits FileDownloaderImpl {
  File get(String filename, URI uri);

  File postJson(String filename, URI uri, Serializable body, boolean isBase64Encoded);
}
