package com.urbanairship.api.reports;

import com.google.common.net.HttpHeaders;
import com.urbanairship.api.client.Request;
import com.urbanairship.api.client.ResponseParser;
import com.urbanairship.api.reports.model.SinglePushInfoResponse;
import com.urbanairship.api.reports.parse.ReportsObjectMapper;
import org.apache.http.entity.ContentType;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Created by maxdelgiudice on 10/5/15.
 */
public class SinglePushInfoRequestTest {
    ObjectMapper mapper = ReportsObjectMapper.getInstance();

    SinglePushInfoRequest singlePushInfoRequest = SinglePushInfoRequest.newRequest("uuid");

    @Test
    public void testContentType() throws Exception {
        assertEquals(singlePushInfoRequest.getContentType(), ContentType.APPLICATION_JSON);
    }

    @Test
    public void testMethod() throws Exception {
        assertEquals(singlePushInfoRequest.getHttpMethod(), Request.HttpMethod.GET);
    }

    @Test
    public void testBody() throws Exception {
        assertEquals(singlePushInfoRequest.getRequestBody(), null);
    }

    @Test
    public void testHeaders() throws Exception {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, Request.CONTENT_TYPE_JSON);
        headers.put(HttpHeaders.ACCEPT, Request.UA_VERSION);

        assertEquals(singlePushInfoRequest.getRequestHeaders(), headers);
    }

    @Test
    public void testURI() throws Exception {
        URI baseURI = URI.create("https://go.urbanairship.com");

        URI expectedURI = URI.create("https://go.urbanairship.com/api/reports/responses/uuid");
        assertEquals(singlePushInfoRequest.getUri(baseURI), expectedURI);
    }

    @Test
    public void testPushParser() throws Exception {
        ResponseParser responseParser = new ResponseParser<SinglePushInfoResponse>() {
            @Override
            public SinglePushInfoResponse parse(String response) throws IOException {
                return mapper.readValue(response, SinglePushInfoResponse.class);
            }
        };

        String response = "{  \n" +
                "  \"push_uuid\":\"5e42ddfc-fa2d-11e2-9ca2-90e2ba025cd0\",\n" +
                "  \"push_time\":\"2013-07-31 22:05:53\",\n" +
                "  \"push_type\":\"BROADCAST_PUSH\",\n" +
                "  \"direct_responses\":4,\n" +
                "  \"sends\":176,\n" +
                "  \"group_id\":\"5e42ddfc-fa2d-11e2-9ca2-90e2ba025cd0\"\n" +
                "}";

        assertEquals(singlePushInfoRequest.getResponseParser().parse(response), responseParser.parse(response));
    }
}
