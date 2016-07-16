/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sh.hyper.hyperjava.auth;

import org.apache.commons.codec.binary.Hex;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

/**
 * Example: Signing AWS Requests with Signature Version 4 in Java.
 *
 * @reference: http://docs.aws.amazon.com/general/latest/gr/sigv4_signing.html
 * @author javaQuery
 * @date 19th January, 2016
 * @Github: https://github.com/javaquery/Examples
 *
 * @modifier: xjimmyshcn
 * @modifyDate: 16th July, 2016
 * @Github: https://github.com/Jimmy-Xu/hypercli-java
 */
public class AWSV4Auth {

    private static final String HMAC_ALGORITHM              = "HYPER-HMAC-SHA256";
    private static final String AWS4_REQUEST                = "hyper_request";
    private static final String KEYPARTS_PREFIX             = "HYPER";

    private static final String HEAD_AUTHORIZATION          = "Authorization";
    /*SignedHeaders: content-type,host,x-hyper-content-sha256,x-hyper-date*/
    private static final String HEAD_CONTENTTYPE            = "Content-Type";
    private static final String HEAD_HOST                   = "Host";
    private static final String HEAD_X_HYPER_CONTENT_SHA256 = "X-Hyper-Content-Sha256";
    private static final String HEAD_X_HYPER_DATE           = "X-Hyper-Date";

    private AWSV4Auth() {
    }

    public static class Builder {

        private String accessKeyID;
        private String secretAccessKey;
        private String regionName;
        private String serviceName;
        private String httpMethodName;
        private String canonicalURI;

        private String host;

        private static final String DEFAULT_SERVICE = "hyper";
        private static final String DEFAULT_REGION = "us-west-1";

        private TreeMap<String, String> queryParametes;
        private TreeMap<String, String> awsHeaders;
        private String payload;
        private boolean debug = false;

        public Builder(String accessKeyID, String secretAccessKey) {
            this.accessKeyID = accessKeyID;
            this.secretAccessKey = secretAccessKey;
            this.regionName = this.DEFAULT_REGION;
            this.serviceName = this.DEFAULT_SERVICE;
        }

        public Builder regionName(String regionName) {
            this.regionName = regionName;
            return this;
        }

        public Builder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public Builder httpMethodName(String httpMethodName) {
            this.httpMethodName = httpMethodName;
            return this;
        }

        public Builder canonicalURI(String canonicalURI) {
            this.canonicalURI = canonicalURI;
            return this;
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder queryParametes(TreeMap<String, String> queryParametes) {
            this.queryParametes = queryParametes;
            return this;
        }

        public Builder awsHeaders(TreeMap<String, String> awsHeaders) {
            this.awsHeaders = awsHeaders;
            return this;
        }

        public Builder payload(String payload) {
            this.payload = payload;
            return this;
        }

        public Builder debug() {
            this.debug = true;
            return this;
        }

        public AWSV4Auth build() {
            return new AWSV4Auth(this);
        }
    }

    private String accessKeyID;
    private String secretAccessKey;
    private String regionName;
    private String serviceName;
    private String httpMethodName;
    private String canonicalURI;
    private String host;
    private TreeMap<String, String> queryParametes;
    private TreeMap<String, String> awsHeaders;
    private String payload;
    private boolean debug = false;

    private String strSignedHeader;
    private String xAmzDate;
    private String currentDate;

    private AWSV4Auth(Builder builder) {
        accessKeyID = builder.accessKeyID;
        secretAccessKey = builder.secretAccessKey;
        regionName = builder.regionName;
        serviceName = builder.serviceName;
        httpMethodName = builder.httpMethodName;
        canonicalURI = builder.canonicalURI;
        host = builder.host;
        queryParametes = builder.queryParametes;
        awsHeaders = builder.awsHeaders;
        payload = builder.payload;
        debug = builder.debug;

        /* Get current timestamp value.(UTC) */
        xAmzDate = getTimeStamp();
        currentDate = getDate();

        //for debug
        if (debug) {
            xAmzDate = "20160712T033826Z";
            currentDate = xAmzDate.substring(0, 8);
        }
    }

    /**
     * Task 1: Create a Canonical Request for Signature Version 4.
     *
     * @return
     */
    private String prepareCanonicalRequest() {
        StringBuilder canonicalURL = new StringBuilder("");

        /* Step 1.1 Start with the HTTP request method (GET, PUT, POST, etc.), followed by a newline character. */
        canonicalURL.append(httpMethodName).append("\n");

        /* Step 1.2 Add the canonical URI parameter, followed by a newline character. */
        canonicalURI = canonicalURI == null || canonicalURI.trim().isEmpty() ? "/" : canonicalURI;
        canonicalURL.append(canonicalURI).append("\n");

        /* Step 1.3 Add the canonical query string, followed by a newline character. */
        StringBuilder queryString = new StringBuilder("");
        if (queryParametes != null && !queryParametes.isEmpty()) {
            for (Map.Entry<String, String> entrySet : queryParametes.entrySet()) {
                String key = entrySet.getKey();
                String value = entrySet.getValue();
                queryString.append(key).append("=").append(URLEncoder.encode(value)).append("&");
            }
            queryString.append("\n");
        } else {
            queryString.append("\n");
        }
        canonicalURL.append(queryString);

        /* Step 1.4 Add the canonical headers, followed by a newline character. */
        StringBuilder signedHeaders = new StringBuilder("");
        if (awsHeaders != null && !awsHeaders.isEmpty()) {
            for (Map.Entry<String, String> entrySet : awsHeaders.entrySet()) {
                String key = entrySet.getKey();
                String value = entrySet.getValue();
                signedHeaders.append(key).append(";");
                canonicalURL.append(key).append(":").append(value).append("\n");
            }

            /* Note: Each individual header is followed by a newline character, meaning the complete list ends with a newline character. */
            canonicalURL.append("\n");
        } else {
            canonicalURL.append("\n");
        }

        /* Step 1.5 Add the signed headers, followed by a newline character. */
        strSignedHeader = signedHeaders.substring(0, signedHeaders.length() - 1); // Remove last ";"
        canonicalURL.append(strSignedHeader).append("\n");

        /* Step 1.6 Use a hash (digest) function like SHA256 to create a hashed value from the payload in the body of the HTTP or HTTPS. */
        payload = payload == null ? "" : payload;
        canonicalURL.append(generateHex(payload));

        if (debug) {
            System.out.println("##Canonical Request:\n" + canonicalURL.toString());
        }

        return canonicalURL.toString();
    }

    /**
     * Task 2: Create a String to Sign for Signature Version 4.
     *
     * @param canonicalURL
     * @return
     */
    private String prepareStringToSign(String canonicalURL) {
        String stringToSign = "";

        /* Step 2.1 Start with the algorithm designation, followed by a newline character. */
        stringToSign = HMAC_ALGORITHM + "\n";

        /* Step 2.2 Append the request date value, followed by a newline character. */
        stringToSign += xAmzDate + "\n";

        /* Step 2.3 Append the credential scope value, followed by a newline character. */
        stringToSign += currentDate + "/" + regionName + "/" + serviceName + "/" + AWS4_REQUEST + "\n";

        /* Step 2.4 Append the hash of the canonical request that you created in Task 1:
        Create a Canonical Request for Signature Version 4. */
        stringToSign += generateHex(canonicalURL);

        if (debug) {
            System.out.println("##String to sign:\n" + stringToSign);
        }

        return stringToSign;
    }

    /**
     * Task 3: Calculate the AWS Signature Version 4.
     *
     * @param stringToSign
     * @return
     */
    private String calculateSignature(String stringToSign) {
        try {
            /* Step 3.1 Derive your signing key */
            byte[] signatureKey = getSignatureKey(secretAccessKey, currentDate, regionName, serviceName);

            if (debug) {
                System.out.println("##calculateSignature:");
                System.out.printf("signatureKey:%s\n", Hex.encodeHexString(signatureKey));
                System.out.printf("stringToSign:%s\n\n", stringToSign);
            }

            /* Step 3.2 Calculate the signature. */
            byte[] signature = hmacSHA256(signatureKey, stringToSign);

            /* Step 3.2.1 Encode signature (byte[]) to Hex */
            String strHexSignature = bytesToHex(signature);
            return strHexSignature;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Task 4: Add the Signing Information to the Request. We'll return Map of
     * all headers put this headers in your request.
     *
     * @return
     */
    public Map<String, String> getHeaders() {
        //SignedHeaders: content-type,host,x-hyper-content-sha256,x-hyper-date
        awsHeaders.put(HEAD_CONTENTTYPE.toLowerCase(), "application/json");
        awsHeaders.put(HEAD_HOST.toLowerCase(), host);
        awsHeaders.put(HEAD_X_HYPER_CONTENT_SHA256.toLowerCase(), generateHex(payload));
        awsHeaders.put(HEAD_X_HYPER_DATE.toLowerCase(), xAmzDate);

        /* Execute Task 1: Create a Canonical Request for Signature Version 4. */
        String canonicalURL = prepareCanonicalRequest();

        /* Execute Task 2: Create a String to Sign for Signature Version 4. */
        String stringToSign = prepareStringToSign(canonicalURL);

        /* Execute Task 3: Calculate the AWS Signature Version 4. */
        String signature = calculateSignature(stringToSign);

        if (signature != null) {
            Map<String, String> header = new HashMap<String, String>(0);
            header.put(HEAD_CONTENTTYPE, "application/json");
            header.put(HEAD_X_HYPER_DATE, xAmzDate);
            header.put(HEAD_X_HYPER_CONTENT_SHA256, generateHex(payload));
            header.put(HEAD_HOST, host);
            header.put(HEAD_AUTHORIZATION, buildAuthorizationString(signature));

            if (debug) {
                System.out.println("##Signature:\n" + signature);
                System.out.println("##Header:");
                for (Map.Entry<String, String> entrySet : header.entrySet()) {
                    System.out.println(entrySet.getKey() + " = " + entrySet.getValue());
                }
                System.out.println("================================");
            }
            return header;
        } else {
            if (debug) {
                System.out.println("##Signature:\n" + signature);
            }
            return null;
        }
    }

    /**
     * Build string for Authorization header.
     *
     * @param strSignature
     * @return
     */
    private String buildAuthorizationString(String strSignature) {
        //should has space after comma(for Hyper_)
        return HMAC_ALGORITHM + " "
                + "Credential=" + accessKeyID + "/" + getDate() + "/" + regionName + "/" + serviceName + "/" + AWS4_REQUEST + ", "
                + "SignedHeaders=" + strSignedHeader + ", "
                + "Signature=" + strSignature;
    }

    /**
     * Generate Hex code of String.
     *
     * @param data
     * @return
     */
    private String generateHex(String data) {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(data.getBytes("UTF-8"));
            byte[] digest = messageDigest.digest();
            return String.format("%064x", new java.math.BigInteger(1, digest));
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Apply HmacSHA256 on data using given key.
     *
     * @param data
     * @param key
     * @return
     * @throws Exception
     * @reference:
     * http://docs.aws.amazon.com/general/latest/gr/signature-v4-examples.html#signature-v4-examples-java
     */
    private byte[] hmacSHA256(byte[] key, String data) throws Exception {
        String algorithm = "HmacSHA256";
        Mac mac = Mac.getInstance(algorithm);
        mac.init(new SecretKeySpec(key, algorithm));
        return mac.doFinal(data.getBytes("UTF8"));
    }

    /**
     * Generate AWS signature key.
     *
     * @param key
     * @param date
     * @param regionName
     * @param serviceName
     * @return
     * @throws Exception
     * @reference
     * http://docs.aws.amazon.com/general/latest/gr/signature-v4-examples.html#signature-v4-examples-java
     */
    private byte[] getSignatureKey(String key, String date, String regionName, String serviceName) throws Exception {
        byte[] kSecret = (KEYPARTS_PREFIX + key).getBytes("UTF8");
        byte[] kDate = hmacSHA256(kSecret, date);
        byte[] kRegion = hmacSHA256(kDate, regionName);
        byte[] kService = hmacSHA256(kRegion, serviceName);
        byte[] kSigning = hmacSHA256(kService, AWS4_REQUEST);
        if (debug) {
            System.out.println("##getSingatureKey:");
            System.out.printf(String.format(" [ key:%s ] - kSecret:%s\n", key, Hex.encodeHexString(kSecret)));
            System.out.printf(String.format(" [ date:%s ] - kDate:%s\n", date, Hex.encodeHexString(kDate)));
            System.out.printf(String.format(" [ regionName:%s ] - kRegion:%s\n", regionName, Hex.encodeHexString(kRegion)));
            System.out.printf(String.format(" [ serviceName:%s ] - kService:%s\n", serviceName, Hex.encodeHexString(kService)));
            System.out.printf(String.format(" [ AWS4_REQUEST:%s ] - kSigning:%s\n\n", AWS4_REQUEST, Hex.encodeHexString(kSigning)));
        }
        return kSigning;
    }

    protected static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    /**
     * Convert byte array to Hex
     *
     * @param bytes
     * @return
     */
    private String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars).toLowerCase();
    }

    /**
     * Get timestamp. yyyyMMdd'T'HHmmss'Z'
     *
     * @return
     */
    private String getTimeStamp() {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); //server timezone
        return dateFormat.format(new Date());
    }

    /**
     * Get date. yyyyMMdd
     *
     * @return
     */
    private String getDate() {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); //server timezone
        return dateFormat.format(new Date());
    }
}
