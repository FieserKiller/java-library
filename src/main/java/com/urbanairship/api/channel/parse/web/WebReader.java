package com.urbanairship.api.channel.parse.web;

import com.urbanairship.api.channel.model.web.Subscription;
import com.urbanairship.api.channel.model.web.WebSettings;
import com.urbanairship.api.common.parse.APIParsingException;
import com.urbanairship.api.common.parse.JsonObjectReader;
import org.codehaus.jackson.JsonParser;

import java.io.IOException;

public class WebReader implements JsonObjectReader<WebSettings> {

    private final WebSettings.Builder builder;

    public WebReader() {
        this.builder = WebSettings.newBuilder();
    }

    public void readSubscription(JsonParser parser) throws IOException {
        builder.setSubscription(parser.readValueAs(Subscription.class));
    }

    public WebSettings validateAndBuild() throws IOException {
        try {
            return builder.build();
        } catch (Exception e) {
            throw new APIParsingException(e.getMessage(), e);
        }
    }
}
