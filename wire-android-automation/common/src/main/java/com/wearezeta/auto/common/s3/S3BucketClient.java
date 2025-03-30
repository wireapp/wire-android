package com.wearezeta.auto.common.s3;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class S3BucketClient {
    private final String bucketName;
    private final TransferManager xferMgr;

    public S3BucketClient(String bucketName, String accessKey, String secretKey) {
        this.bucketName = bucketName;
        final AmazonS3 s3Client = AmazonS3ClientBuilder
                .standard()
                .withRegion(Regions.EU_WEST_1)
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
                .build();
        this.xferMgr = TransferManagerBuilder
                .standard()
                .withS3Client(s3Client)
                .build();
    }

    public void uploadFile(File srcFile, String dstPath) {
        try (final InputStream is = new FileInputStream(srcFile)) {
            final ObjectMetadata fileMetadata = new ObjectMetadata();
            fileMetadata.setContentLength(srcFile.length());
            fileMetadata.setCacheControl("max-age=0,no-cache,no-store,must-revalidate");
            xferMgr.upload(bucketName, dstPath, is, fileMetadata)
                    .waitForCompletion();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void downloadFile(String srcPath, File dstFile) {
        try {
            xferMgr.download(bucketName, srcPath, dstFile)
                    .waitForCompletion();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
