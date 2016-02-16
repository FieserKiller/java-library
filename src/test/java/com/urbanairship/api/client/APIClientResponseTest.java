package com.urbanairship.api.client;

import com.google.common.collect.ImmutableList;
import com.urbanairship.api.client.model.APIClientResponse;
import com.urbanairship.api.client.model.APIListTagsResponse;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class APIClientResponseTest {

    @Test
    public void testStringAPIResponse() {
        HttpResponse httpResponse = new BasicHttpResponse(new BasicStatusLine(
                new ProtocolVersion("HTTP", 1, 1), 200, "OK"));

        APIClientResponse.Builder<String> builder =
                APIClientResponse.newStringResponseBuilder()
                        .setApiResponse("StringLaLaLa")
                        .setHttpResponse(httpResponse);

        APIClientResponse<String> testResponse = builder.build();

        assertTrue("HTTP response not set properly",
                testResponse.getHttpResponse().equals(httpResponse));

        assertTrue("APIResponse not set properly",
                testResponse.getApiResponse().equals("StringLaLaLa"));
    }

    @Test
    public void testAPIListTagsResponse() {
        HttpResponse httpResponse = new BasicHttpResponse(new BasicStatusLine(
                new ProtocolVersion("HTTP", 1, 1), 200, "OK"));

        List<String> listOTags = new ArrayList<String>();
        listOTags.add("Puppies");
        listOTags.add("Kitties");

        APIListTagsResponse listTagsResponse = APIListTagsResponse.newBuilder()
                .allAllTags(listOTags)
                .build();
        APIClientResponse.Builder<APIListTagsResponse> builder =
                new APIClientResponse.Builder<APIListTagsResponse>()
                        .setApiResponse(listTagsResponse)
                        .setHttpResponse(httpResponse);
        APIClientResponse<APIListTagsResponse> testResponse = builder.build();
        assertTrue("HTTP response not set properly",
                testResponse.getHttpResponse().equals(httpResponse));

        assertTrue("APIResponse not set properly",
                testResponse.getApiResponse().equals(listTagsResponse));
    }

}
