package org.jahia.se.modules.forms.storage;

import org.jahia.se.modules.forms.service.FormS3StorageService;
//import org.osgi.service.component.annotations.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.S3Client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;

//@Component(service = FormS3StorageService.class)
public class FormS3StorageServiceImpl implements FormS3StorageService {
    private static Logger logger = LoggerFactory.getLogger(FormS3StorageServiceImpl.class);

    private S3Client s3Client;
    private final String bucket = "forms-s3";

    public FormS3StorageServiceImpl() {
//        .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
        this.s3Client = S3Client.builder().build();//no need to specify the region .region(region), comes from env : AWS_REGION
    }

    public String upload(byte[] attachment, String fileName, String mimeType) {
        try {
//            logger.debug("Uploading file "+ fileName +", to S3 - from: "+ filePath);
            logger.debug("Uploading file "+ fileName +", to S3 ");
//            byte[] attachment = getObjectFile(filePath);
            PutObjectRequest putOb = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)//fileName
                    .contentType(mimeType)//(MediaType.APPLICATION_PDF.toString())//mimeType
                    .contentLength((long) attachment.length)
                    .build();

            PutObjectResponse response = s3Client.putObject(putOb,
                    RequestBody.fromBytes(attachment));

            final URL reportUrl = s3Client.utilities().getUrl(GetUrlRequest.builder().bucket(bucket).key(fileName).build());

            logger.debug("response = " + response);
            logger.debug("reportUrl = " + reportUrl);

            return reportUrl.toString();
        } catch (S3Exception e) {
            logger.error(e.getMessage());
//            System.exit(1);
        }
        return "";
    }

    // Return a byte array
    public byte[] getObjectFile(String filePath) {

        FileInputStream fileInputStream = null;
        byte[] bytesArray = null;

        try {
            File file = new File(filePath);
            bytesArray = new byte[(int) file.length()];
            fileInputStream = new FileInputStream(file);
            fileInputStream.read(bytesArray);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return bytesArray;
    }

//    public void setup(String bucketName) {//S3Client s3Client, Region region
//        try {
//            s3Client.createBucket(CreateBucketRequest
//                    .builder()
//                    .bucket(bucketName)
//                    .createBucketConfiguration(
//                            CreateBucketConfiguration.builder()
////                                    .locationConstraint(region.id())
//                                    .build())
//                    .build());
//            logger.debug("Creating S3 bucket: " + bucketName);
//            s3Client.waiter().waitUntilBucketExists(HeadBucketRequest.builder()
//                    .bucket(bucketName)
//                    .build());
//            logger.debug(bucketName +" is ready.");
//        } catch (S3Exception e) {
//            logger.error(e.awsErrorDetails().errorMessage());
////            System.exit(1);
//        }
//    }

}
