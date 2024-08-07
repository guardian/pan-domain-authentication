package com.gu.pandomainauth

import com.amazonaws.services.s3.AmazonS3

import java.io.InputStream

/**
 * This trait provides a way to download a file from an S3 bucket, in a way that's agnostic of which
 * AWS SDK (v1 or v2) is being used. An instance of S3BucketLoader is *specific* to a particular S3 bucket.
 */
trait S3BucketLoader {
  /**
   * @param key the key of the file in the S3 bucket, not including the bucket name or a starting "/"
   */
  def inputStreamFetching(key: String): InputStream
}

object S3BucketLoader {
  /**
   * A convenience method to create an S3BucketLoader using AWS SDK v1, the version used by most of our existing code.
   * However, codebases that want to use AWS SDK v2 are able to provide their own implementation of S3BucketLoader.
   */
  def forAwsSdkV1(s3Client: AmazonS3, bucketName: String): S3BucketLoader =
    s3Client.getObject(bucketName, _).getObjectContent
}
