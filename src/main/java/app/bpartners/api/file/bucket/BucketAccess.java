package app.bpartners.api.file.bucket;

import software.amazon.awssdk.transfer.s3.S3TransferManager;

public interface BucketAccess {
  String getBucketName();

  S3TransferManager getS3TransferManager();
}
