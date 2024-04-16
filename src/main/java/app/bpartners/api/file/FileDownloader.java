package app.bpartners.api.file;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.api.model.exception.ApiException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.function.BiFunction;

public class FileDownloader implements BiFunction<String, URI, File> {
  private final HttpClient httpClient;

  public FileDownloader(HttpClient httpClient) {
    this.httpClient = httpClient;
  }

  @Override
  public File apply(String filename, URI uri) {
    HttpRequest request = HttpRequest.newBuilder().uri(uri).GET().build();
    try {
      var file = Files.createTempFile(filename, null);
      var response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofFile(file.toAbsolutePath()));
      if (response.statusCode() / 100 != 2) {
        throw new ApiException(SERVER_EXCEPTION, "error during download of " + uri);
      }
      return response.body().toFile();
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}