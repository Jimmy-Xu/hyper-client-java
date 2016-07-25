package sh.hyper.hyperjava.auth;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;

public class HyperAWSV4AuthTest {

    @Test
    public void testAuthForVersion() throws IOException {

        String host = "us-west-1.hyper.sh";
        TreeMap<String, String> awsHeaders = new TreeMap<String, String>();
        String accessKey = System.getenv("HYPER_ACCESS_KEY");
        String secretKey = System.getenv("HYPER_SECRET_KEY");
        AWSV4Auth awsV4Auth = new AWSV4Auth.Builder(accessKey, secretKey)
                .host(host)
                .regionName("us-west-1")
                .serviceName("hyper")
                .httpMethodName("GET") //GET, PUT, POST, DELETE, etc...
                .canonicalURI("v1.23/version") //end point (first char is not '/' for Hyper_)
                .queryParametes(null) //query parameters if any
                .awsHeaders(awsHeaders) //aws header parameters
                .payload(null) // payload if any
                //.debug() // turn on the debug mode
                .build();


        /* Attach header in your request */
        /* Simple get request */
        String url = "https://" + host + "/v1.23/version";
        HttpGet request = new HttpGet(url);

        /* Get header calculated for request */
        Map<String, String> header = awsV4Auth.getHeaders();
        System.out.printf("curl -v -k \\\n");
        for (Map.Entry<String, String> entrySet : header.entrySet()) {
            String key = entrySet.getKey();
            String value = entrySet.getValue();
            System.out.printf(String.format(" -H \"%s: %s\" \\\n", key,value));

            request.addHeader(key, value);
        }

        System.out.printf("-X GET \\\n%s\n", url);

        // send request
        HttpClient client = HttpClientBuilder.create().build();
        HttpResponse response = null;
        try {
            response = client.execute(request);
            System.out.printf("Response Code : %d\n", response.getStatusLine().getStatusCode());
            BufferedReader rd = new BufferedReader(
                    new InputStreamReader((response.getEntity().getContent())));
            StringBuffer result = new StringBuffer();
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            System.out.println("Result:");
            System.out.println(result.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    @Test
    public void testAuthForListContainer() throws IOException {

        String host = "us-west-1.hyper.sh";
        TreeMap<String, String> awsHeaders = new TreeMap<String, String>();
        TreeMap<String, String> queryParameters = new TreeMap<String, String>();
        String accessKey = System.getenv("HYPER_ACCESS_KEY");
        String secretKey = System.getenv("HYPER_SECRET_KEY");

        String method = "GET";
        String postData = null;
        queryParameters.put("all","true");

        AWSV4Auth awsV4Auth = new AWSV4Auth.Builder(accessKey, secretKey)
                .host(host)
                .regionName("us-west-1")
                .serviceName("hyper")
                .httpMethodName(method) //GET, PUT, POST, DELETE, etc...
                .canonicalURI("v1.23/containers/json") //end point (first char is not '/' for Hyper_)
                .queryParametes(queryParameters) //query parameters if any
                .awsHeaders(awsHeaders) //aws header parameters
                .payload(postData) // payload if any
                //.debug() // turn on the debug mode
                .build();


        /* Attach header in your request */
        /* Simple get request */
        String url = "https://" + host + "/v1.23/containers/json?all=true";
        HttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest(method, url);

        /* Get header calculated for request */
        Map<String, String> header = awsV4Auth.getHeaders();
        for (Map.Entry<String, String> entrySet : header.entrySet()) {
            String key = entrySet.getKey();
            String value = entrySet.getValue();
            request.addHeader(key, value);
        }

        // send request
        try {
            String result = sendRequest(
                    method,
                    request.getRequestLine().getUri(),
                    awsV4Auth.getHeaders(),
                    postData
            );
            System.out.println("Result:");
            System.out.println(result.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private String sendRequest(String method, String requestUrl, Map<String, String> header, String payload) {

        //output curl command line
        System.out.println("----------------------------------------");
        System.out.printf("curl -v -k \\\n");

        for (Map.Entry<String, String> entrySet : header.entrySet()) {
            System.out.printf(String.format(" -H \"%s: %s\" \\\n", entrySet.getKey(), entrySet.getValue()));
        }
        if (payload!=null) {
            System.out.printf(" -d '%s' \\\n", payload);
        }
        System.out.printf(" -X %s \\\n%s\n", method, requestUrl);
        System.out.println("----------------------------------------");


        StringBuffer jsonString = new StringBuffer();
        try {
            URL url = new URL(requestUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setDoInput(true);
            connection.setDoOutput(true);

            //set header
            for (Map.Entry<String, String> entrySet : header.entrySet()) {
                connection.setRequestProperty(entrySet.getKey(), entrySet.getValue());
            }

            connection.setRequestMethod(method);
            if (payload!=null) {
                //post
                OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
                writer.write(payload);
                writer.close();
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String line;
            while ((line = br.readLine()) != null) {
                jsonString.append(line);
            }
            br.close();
            connection.disconnect();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
        return jsonString.toString();
    }

}