package com.urbanairship.api.push.parse.notification.ios;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.urbanairship.api.common.parse.APIParsingException;
import com.urbanairship.api.common.parse.BooleanFieldDeserializer;
import com.urbanairship.api.common.parse.JsonObjectReader;
import com.urbanairship.api.common.parse.StringFieldDeserializer;
import com.urbanairship.api.push.model.notification.ios.IOSSoundData;

import java.io.IOException;

public class IOSSoundDataReader implements JsonObjectReader<IOSSoundData> {
    private final IOSSoundData.Builder builder;

    public IOSSoundDataReader() { this.builder = IOSSoundData.newBuilder(); }

    @Override
    public IOSSoundData validateAndBuild() throws IOException {
        try {
            return builder.build();
        } catch (Exception e) {
            throw new APIParsingException(e.getMessage(), e);
        }
    }

    public void readCritical(JsonParser parser, DeserializationContext context) throws IOException {
        builder.setCritical(BooleanFieldDeserializer.INSTANCE.deserialize(parser, "critical"));
    }

    public void readVolume(JsonParser parser, DeserializationContext context) throws IOException {
        builder.setVolume(parser.readValueAs(Double.TYPE));
    }

    public void readName(JsonParser parser, DeserializationContext context) throws IOException {
        builder.setName(StringFieldDeserializer.INSTANCE.deserialize(parser, "name"));
    }
}
