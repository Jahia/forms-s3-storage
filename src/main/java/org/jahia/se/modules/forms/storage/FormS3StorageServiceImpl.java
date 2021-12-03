package org.jahia.se.modules.forms.storage;

//import com.amazonaws.util.DateUtils;
import org.jahia.se.modules.forms.service.FormS3StorageService;
//import org.osgi.service.component.annotations.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.S3Client;

import com.amazonaws.services.cloudfront.CloudFrontUrlSigner;
import com.amazonaws.services.cloudfront.util.SignerUtils.Protocol;

//import org.jahia.se.modules.forms.storage.auth.AWS4SignerBase;
//import org.jahia.se.modules.forms.storage.auth.AWS4SignerForQueryParameterAuth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
//import java.security.Security;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
//import java.util.HashMap;
//import java.util.Map;

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

            //Public url access
//            final String reportUrl = s3Client.utilities().getUrl(GetUrlRequest.builder().bucket(bucket).key(fileName).build()).toString();

            //7 days S3 private access
//            final URL endpointUrl = s3Client.utilities().getUrl(GetUrlRequest.builder().bucket(bucket).key(fileName).build());
//            String AWS_ACCESS_KEY = System.getenv("AWS_ACCESS_KEY_ID");
//            String AWS_SECRET_KEY = System.getenv("AWS_SECRET_ACCESS_KEY");
//            final String reportUrl = getPresignedUrlToS3Object(endpointUrl,AWS_ACCESS_KEY,AWS_SECRET_KEY);

            //1Years cloud front access
            final String reportUrl = getCloudFrontURLSigned(fileName);

            logger.debug("response = " + response);
            logger.debug("reportUrl = " + reportUrl);

            return reportUrl;
        } catch (S3Exception e) {
            logger.error(e.getMessage());
        } catch (InvalidKeySpecException e) {
            logger.error(e.getMessage());
//            e.printStackTrace();
        } catch (IOException e) {
            logger.error(e.getMessage());
//            e.printStackTrace();
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



    /**
     * Cloud Front 1 Year URL access with
     * the CloudFrontUrlSigner utility class in the AWS SDK for Java (version 1)
     */
    private String getCloudFrontURLSigned(String s3ObjectKey) throws InvalidKeySpecException, IOException {
        Protocol protocol = Protocol.https;
        String distributionDomain = System.getenv("AWS_CLOUDFRONT_DOMAIN");
        File privateKeyFile = new File(System.getenv("AWS_CLOUDFRONT_PEM_PATH"));
        String keyPairId =System.getenv("AWS_CLOUDFRONT_KEY_PAIR_ID");
        Date currentDate = new Date();
        LocalDateTime localDateTime = currentDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        localDateTime = localDateTime.plusYears(1);
        Date dateLessThan = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());

        String signedURLWithCannedPolicy = CloudFrontUrlSigner.getSignedURLWithCannedPolicy(
                protocol, distributionDomain, privateKeyFile,
                s3ObjectKey, keyPairId, dateLessThan);

//        Date dateLessThan = DateUtils.parseISO8601Date("2011-11-14T22:20:00.000Z");
//        Date dateGreaterThan = new Date(System.currentTimeMillis()+1000*60*60*24*365);
//        String ipRange = "0.0.0.0/0";//"192.168.0.1/24";

//        String url2 = CloudFrontUrlSigner.getSignedURLWithCustomPolicy(
//                protocol, distributionDomain, privateKeyFile,
//                s3ObjectKey, keyPairId, dateLessThan,
//                dateGreaterThan, ipRange);

        return signedURLWithCannedPolicy;
    }

//    /**
//     * Cloud Front 1 Year URL access with the AWS SDK for Java (version 2)
//     * NOTE to be review doesn't work as it is
//     */
//    private String getCloudFrontURLSigned(String s3ObjectKey){
//        String distributionDomain = System.getenv("AWS_CLOUDFRONT_DOMAIN");
//        String privateKeyFilePath = System.getenv("AWS_CLOUDFRONT_PEM_PATH");
//        String policyResourcePath = distributionDomain + "/" + s3ObjectKey;
//        String keyPairId = System.getenv("AWS_ACCESS_KEY_ID");
//
//// Convert your DER file into a byte array.
//
//        byte[] derPrivateKey = ServiceUtils.readInputStreamToBytes(new
//                FileInputStream(privateKeyFilePath));
//
//// Generate a "canned" signed URL to allow access to a
//// specific distribution and file
//
//        String signedUrlCanned = CloudFrontService.signUrlCanned(
//                "https://" + distributionDomain + "/" + s3ObjectKey, // Resource URL or Path
//                keyPairId,     // Certificate identifier,
//                // an active trusted signer for the distribution
//                derPrivateKey, // DER Private key data
//                ServiceUtils.parseIso8601Date("2011-11-14T22:20:00.000Z") // DateLessThan
//        );
//        System.out.println(signedUrlCanned);
//
//// Build a policy document to define custom restrictions for a signed URL.
//
//        String policy = CloudFront Service.buildPolicyForSignedUrl(
//                // Resource path (optional, can include '*' and '?' wildcards)
//                policyResourcePath,
//                // DateLessThan
//                ServiceUtils.parseIso8601Date("2011-11-14T22:20:00.000Z"),
//                // CIDR IP address restriction (optional, 0.0.0.0/0 means everyone)
//                "0.0.0.0/0",
//                // DateGreaterThan (optional)
//                ServiceUtils.parseIso8601Date("2011-10-16T06:31:56.000Z")
//        );
//
//// Generate a signed URL using a custom policy document.
//
//        String signedUrl = CloudFrontService.signUrl(
//                // Resource URL or Path
//                "https://" + distributionDomain + "/" + s3ObjectKey,
//                // Certificate identifier, an active trusted signer for the distribution
//                keyPairId,
//                // DER Private key data
//                derPrivateKey,
//                // Access control policy
//                policy
//        );
//        return signedUrl;
//    }




//    /**
//     * Construct a basic presigned url to the endpointUrl in the
//     * given bucket and region using path-style object addressing. The signature
//     * V4 authorization data is embedded in the url as query parameters.
//     */
//    private String getPresignedUrlToS3Object(URL endpointUrl, String awsAccessKey, String awsSecretKey) {
//
//        // construct the query parameter string to accompany the url
//        Map<String, String> queryParams = new HashMap<>();
//        String regionName = System.getenv("AWS_REGION");
//
//        // for SignatureV4, the max expiry for a presigned url is 7 days,
//        // expressed in seconds
//        int expiresIn = 7 * 24 * 60 * 60;
//        queryParams.put("X-Amz-Expires", "" + expiresIn);
//
//        // we have no headers for this sample, but the signer will add 'host'
//        Map<String, String> headers = new HashMap<String, String>();
//
//        AWS4SignerForQueryParameterAuth signer = new AWS4SignerForQueryParameterAuth(
//                endpointUrl, "GET", "s3", regionName);
//        String authorizationQueryParameters = signer.computeSignature(headers,
//                queryParams,
//                AWS4SignerBase.UNSIGNED_PAYLOAD,
//                awsAccessKey,
//                awsSecretKey);
//
//        // build the presigned url to incorporate the authorization elements as query parameters
//        String presignedUrl = endpointUrl.toString() + "?" + authorizationQueryParameters;
//        return presignedUrl;
//    }
}
