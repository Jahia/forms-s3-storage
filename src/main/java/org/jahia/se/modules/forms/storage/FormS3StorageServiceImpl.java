package org.jahia.se.modules.forms.storage;

import com.amazonaws.util.DateUtils;
import org.jahia.se.modules.forms.service.FormS3StorageService;
//import org.osgi.service.component.annotations.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.S3Client;

import com.amazonaws.services.cloudfront.CloudFrontUrlSigner;
import com.amazonaws.services.cloudfront.util.SignerUtils.Protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;

//@Component(service = FormS3StorageService.class)
public class FormS3StorageServiceImpl implements FormS3StorageService {
    private static Logger logger = LoggerFactory.getLogger(FormS3StorageServiceImpl.class);
//    Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

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

            final URL reportUrl = s3Client.utilities().getUrl(GetUrlRequest.builder().bucket(bucket).key(fileName).build());

            logger.debug("response = " + response);
            logger.debug("reportUrl = " + reportUrl);

            return reportUrl.toString();
        } catch (S3Exception e) {
            logger.error(e.getMessage());
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


    private String getCloudFrontURLSigned(String s3ObjectKey, String awsAccessKey, String awsSecretKey) throws InvalidKeySpecException, IOException {
        Protocol protocol = Protocol.http;
        String distributionDomain = System.getenv("AWS_CLOUDFRONT_DOMAIN");
        File privateKeyFile = new File(System.getenv("AWS_CLOUDFRONT_PEM_PATH"));
        String keyPairId = System.getenv("AWS_ACCESS_KEY_ID");//"APKAJCEOKRHC3XIVU5NA";
        Date dateLessThan = new Date(System.currentTimeMillis()+1000*60*60*24*365);
//        Date dateLessThan = DateUtils.parseISO8601Date("2011-11-14T22:20:00.000Z");
//        Date dateGreaterThan = DateUtils.parseISO8601Date("2011-11-14T22:20:00.000Z");
//        String ipRange = "0.0.0.0/0";//"192.168.0.1/24";

        String url1 = CloudFrontUrlSigner.getSignedURLWithCannedPolicy(
                protocol, distributionDomain, privateKeyFile,
                s3ObjectKey, keyPairId, dateLessThan);

//        String url2 = CloudFrontUrlSigner.getSignedURLWithCustomPolicy(
//                protocol, distributionDomain, privateKeyFile,
//                s3ObjectKey, keyPairId, dateLessThan,
//                dateGreaterThan, ipRange);

        return url1;
    }

//    /**
//     * Construct a basic presigned url to the object '/ExampleObject.txt' in the
//     * given bucket and region using path-style object addressing. The signature
//     * V4 authorization data is embedded in the url as query parameters.
//     */
//    private void getPresignedUrlToS3Object(String endpointUrl, String awsAccessKey, String awsSecretKey) {
//
//        // construct the query parameter string to accompany the url
//        Map<String, String> queryParams = new HashMap<>();
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
//        System.out.println("--------- Computed presigned url ---------");
//        System.out.println(presignedUrl);
//        System.out.println("------------------------------------------");
//    }
}
