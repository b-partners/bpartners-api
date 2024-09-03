package app.bpartners.api.endpoint.rest.controller;

import static app.bpartners.api.endpoint.rest.model.FileType.AREA_PICTURE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

import app.bpartners.api.endpoint.rest.model.FileInfo;
import app.bpartners.api.endpoint.rest.model.FileType;
import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.file.FileWriter;
import app.bpartners.api.file.MultipartFileConverter;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.model.mapper.FileMapper;
import app.bpartners.api.service.FileService;
import app.bpartners.api.service.utils.FileInfoUtils;
import java.io.File;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@AllArgsConstructor
@Slf4j
public class FileController {
  private final FileService service;
  private final FileMapper mapper;
  private final MultipartFileConverter multipartFileConverter;
  private final FileWriter fileWriter;

  @GetMapping(value = "/accounts/{accountId}/files/{id}")
  public FileInfo getFileInfoById(
      @PathVariable(name = "accountId") String accountId,
      @PathVariable(name = "id") String fileId) {
    return mapper.toRest(service.findById(fileId));
  }

  @GetMapping(value = "/accounts/{accountId}/files/{id}/raw")
  public ResponseEntity<byte[]> downloadFile(
      @PathVariable(name = "accountId") String accountId,
      @PathVariable(name = "id") String fileId,
      @RequestParam(name = "fileType") FileType fileType) {
    String idUser =
        AuthProvider.getAuthenticatedUserId(); // TODO: should be changed when endpoint changed
    byte[] downloaded = fileWriter.writeAsByte(service.downloadFile(fileType, idUser, fileId));
    return ResponseEntity.ok()
        .contentType(FileInfoUtils.parseMediaTypeFromBytes(downloaded))
        .body(downloaded);
  }

  @PostMapping(value = "/accounts/{accountId}/files/{id}/raw")
  public ResponseEntity<byte[]> uploadFile(
      @PathVariable(name = "accountId") String accountId,
      @PathVariable(name = "id") String fileId,
      @RequestParam(name = "fileType") FileType fileType,
      @RequestBody byte[] toUpload) {
    if (AREA_PICTURE.equals(fileType)) {
      throw new NotImplementedException("Area picture upload is made through a specific endpoint");
    }
    File fileToUpload = fileWriter.apply(toUpload, null);
    service.upload(fileType, fileId, AuthProvider.getAuthenticatedUserId(), fileToUpload);
    return ResponseEntity.ok()
        .contentType(FileInfoUtils.parseMediaTypeFromBytes(toUpload))
        .body(toUpload);
  }

  @PostMapping(
      value = "/accounts/{accountId}/files/{id}/multipart",
      consumes = MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<byte[]> uploadMultipartFile(
      @PathVariable(name = "accountId") String accountId,
      @PathVariable(name = "id") String fileId,
      @RequestParam(name = "fileType") FileType fileType,
      @RequestPart(name = "file") MultipartFile file) {
    if (AREA_PICTURE.equals(fileType)) {
      throw new NotImplementedException("Area picture upload is made through a specific endpoint");
    }
    var filesAsBytes = multipartFileConverter.apply(file);
    var fileToUpload = fileWriter.apply(filesAsBytes, null);
    String idUser =
        AuthProvider.getAuthenticatedUserId(); // TODO: should be changed when endpoint changed
    service.upload(fileType, fileId, idUser, fileToUpload);
    return ResponseEntity.ok()
        .contentType(FileInfoUtils.parseMediaTypeFromBytes(filesAsBytes))
        .body(filesAsBytes);
  }
}
