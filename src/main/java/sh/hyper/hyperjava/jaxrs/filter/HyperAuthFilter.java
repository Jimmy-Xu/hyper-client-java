package sh.hyper.hyperjava.jaxrs.filter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.hyper.hyperjava.auth.AWSV4Auth;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 * @author Jimmy Xu(xjimmyshcn@gmail.com)
 */
public class HyperAuthFilter implements ClientRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(HyperAuthFilter.class.getName());

    private static JsonFactory jsonFactory = new JsonFactory();
    private static ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        LOGGER.info(String.format("Add Hyper Auth Header: method(%s) - path(:%s)\n",
                requestContext.getMethod(),
                requestContext.getUri().getPath()
        ));

        MultivaluedMap<String, Object> header = requestContext.getHeaders(); //original HEADER
        MultivaluedMap<String, String> newHeaderMap = generateHyperAuthHeader(requestContext); //new HEADER
        for (Map.Entry<String, List<String>> entrySet : newHeaderMap.entrySet()) {
            String key = entrySet.getKey();
            List<String> value = entrySet.getValue();
            header.putSingle(key, value.get(0));
        }
    }

    // generate auth Header for Hyper_
    private MultivaluedHashMap<String, String> generateHyperAuthHeader(ClientRequestContext requestContext) throws IOException {

        URI uri = requestContext.getUri();
        String method = requestContext.getMethod();
        String postData = null;
        boolean prettyPrinter = false;
        boolean debugMode = true;

        String accessKey = System.getProperty("HYPER_ACCESS_KEY");
        String secretKey = System.getProperty("HYPER_SECRET_KEY");
        TreeMap<String, String> awsHeaders = new TreeMap<String, String>();

        if (debugMode) {
            //output header
            LOGGER.debug(String.format("##generateHyperAuthHeader:header\nHost:%s\nPath:%s\nregionName:%s\ncanonicalURI:%s\n",
                    uri.getHost(),
                    uri.getPath(),
                    uri.getHost().split("\\.")[0],
                    uri.getPath().substring(1)
            ));
        }

        if (requestContext.hasEntity()) {
            //convert object to jsonString
            StringWriter sw = new StringWriter();
            JsonGenerator jg = jsonFactory.createGenerator(sw);
            if (prettyPrinter) {
                jg.useDefaultPrettyPrinter();
            }
            objectMapper.writeValue(jg, requestContext.getEntity());
            postData = sw.toString();
            if (debugMode) {
                //output postData
                LOGGER.debug(String.format("##generateHyperAuthHeader:postData\n%s\n", postData));
            }
        }

        //add Sign v4 Header for Hyper_
        AWSV4Auth.Builder builder = new AWSV4Auth.Builder(accessKey, secretKey)
                .host(uri.getHost())
                .regionName(uri.getHost().split("\\.")[0])
                .serviceName("hyper")
                .httpMethodName(method)                     //GET, PUT, POST, DELETE, etc...
                .canonicalURI(uri.getPath().substring(1))   //end point (first char is not '/' for Hyper_)
                .queryParametes(null)                       //query parameters if any
                .awsHeaders(awsHeaders)                     //aws header parameters
                .payload(postData);                         // payload if any

        AWSV4Auth awsV4Auth;
        if (debugMode) {
            awsV4Auth = builder.debug().build(); //turn on the debug mode
        } else {
            awsV4Auth = builder.build();
        }

        /* Get header calculated for request */
        Map<String, String> header = awsV4Auth.getHeaders();
        MultivaluedHashMap<String, String> result = new MultivaluedHashMap<String, String>();
        String curlCmd = "curl -v -k \\\n";
        for (Map.Entry<String, String> entrySet : header.entrySet()) {
            String key = entrySet.getKey();
            String value = entrySet.getValue();
            result.add(key, value);
            if (debugMode) {
                curlCmd += String.format(" -H \"%s: %s\" \\\n", key, value);
            }
        }

        /* print curl command line */
        if (debugMode) {
            if (postData != null) {
                curlCmd += String.format(" -d '%s'\\\n", postData);
            }
            curlCmd += String.format(" -X %s \\\n%s\n", method, uri.toString());
            System.out.printf("##curl command line:%s", curlCmd);
        }

        return result;
    }

}


