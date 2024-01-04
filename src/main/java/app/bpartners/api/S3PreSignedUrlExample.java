package app.bpartners.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

public class S3PreSignedUrlExample {

  public static void main(String[] args) {
    // Remplacez ces valeurs par vos propres informations d'identification AWS
    String accessKeyId = "VOTRE_ACCESS_KEY_ID";
    String secretKey = "VOTRE_SECRET_KEY";
    String region = "VOTRE_REGION_AWS";

    // Remplacez ces valeurs par vos propres informations S3
    String bucketName = "VOTRE_BUCKET_NAME";
    String sourceFolderPath = "chemin/vers/dossier/source";
    String outputZipFileName = "output.zip";

    // Initialisation du client S3
    S3Client s3Client = S3Client.builder().build();
    S3Presigner s3Presigner = S3Presigner.builder().build();

    // Création du fichier ZIP en mémoire
    byte[] zipFileBytes = createZipFile(sourceFolderPath);

    // Uploader le fichier ZIP sur S3
    uploadZipFileToS3(s3Client, bucketName, outputZipFileName, zipFileBytes);

    // Générer l'URL pré-signée pour le téléchargement
    String preSignedUrl = generatePreSignedUrl(s3Presigner, bucketName, outputZipFileName);

    System.out.println(
        "Le fichier ZIP a été téléchargé sur S3. Lien de téléchargement pré-signé : "
            + preSignedUrl);
  }

  private static byte[] createZipFile(String sourceFolderPath) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos)) {

      // Obtenez la liste des fichiers dans le dossier source
      // (vous devrez probablement utiliser l'API S3 pour obtenir la liste des objets dans un seau)
      // Pour cet exemple, supposons que vous avez déjà la liste des noms de fichiers
      String[] fileNames = {"file1.txt", "file2.txt", "file3.txt"};

      for (String fileName : fileNames) {
        // Lire chaque fichier depuis S3 et l'ajouter au fichier ZIP en mémoire
        ResponseInputStream<GetObjectResponse> objectContent =
            getObjectContentFromS3(sourceFolderPath, fileName);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zos.putNextEntry(zipEntry);

        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = objectContent.read(buffer)) > 0) {
          zos.write(buffer, 0, bytesRead);
        }

        objectContent.close();
        zos.closeEntry();
      }

      zos.finish();
      zos.flush();

      return baos.toByteArray();

    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  private static ResponseInputStream<GetObjectResponse> getObjectContentFromS3(
      String bucketName, String key) {
    // Utilisez l'API S3 pour obtenir le contenu d'un objet
    S3Client s3Client = S3Client.builder().build();
    GetObjectRequest getObjectRequest =
        GetObjectRequest.builder().bucket(bucketName).key(key).build();

    return s3Client.getObject(getObjectRequest);
  }

  private static void uploadZipFileToS3(
      S3Client s3Client, String bucketName, String objectKey, byte[] zipFileBytes) {
    PutObjectRequest putObjectRequest =
        PutObjectRequest.builder().bucket(bucketName).key(objectKey).build();

    s3Client.putObject(putObjectRequest, RequestBody.fromBytes(zipFileBytes));
  }

  private static String generatePreSignedUrl(
      S3Presigner s3Presigner, String bucketName, String objectKey) {
    // Générer l'URL pré-signée pour le fichier ZIP

    Instant now = Instant.now();
    Instant expirationInstant = now.plusSeconds(3600);
    Duration expirationDuration = Duration.between(now, expirationInstant);

    GetObjectRequest getObjectRequest =
        GetObjectRequest.builder().bucket(bucketName).key(objectKey).build();

    PresignedGetObjectRequest presignRequest =
        s3Presigner.presignGetObject(
            GetObjectPresignRequest.builder()
                .signatureDuration(expirationDuration)
                .getObjectRequest(getObjectRequest)
                .build());

    return presignRequest.url().toString();
  }
}
