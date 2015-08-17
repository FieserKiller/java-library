package com.urbanairship.api.schedule;

/*
 * Copyright (c) 2013-2015.  Urban Airship and Contributors
 */

import com.google.common.net.HttpHeaders;
import com.urbanairship.api.client.Request;
import com.urbanairship.api.client.RequestUtils;
import com.urbanairship.api.client.ResponseParser;
import org.apache.http.entity.ContentType;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class DeleteScheduleRequest implements Request<String> {

    private final String scheduleId;

    private DeleteScheduleRequest(String scheduleId) {
        this.scheduleId = scheduleId;
    }

    public static DeleteScheduleRequest newRequest(String scheduleId) {
        return new DeleteScheduleRequest(scheduleId);
    }

    @Override
    public ContentType getContentType() {
        return null;
    }

    @Override
    public Map<String, String> getRequestHeaders() {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.ACCEPT, UA_VERSION);
        return headers;
    }

    @Override
    public HTTPMethod getHTTPMethod() {
        return HTTPMethod.DELETE;
    }

    @Override
    public String getRequestBody() {
        return null;
    }

    @Override
    public URI getUri(URI baseUri) {
        return RequestUtils.resolveURI(baseUri, ScheduleRequest.API_SCHEDULE_PATH + scheduleId);
    }

    @Override
    public ResponseParser<String> getResponseParser() {
        return new ResponseParser<String>() {
            @Override
            public String parse(String response) throws IOException {
                return response;
            }
        };
    }
}
