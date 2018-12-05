package com.urbanairship.api.channel.parse.email;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.urbanairship.api.channel.Constants;
import com.urbanairship.api.channel.model.email.OptInLevel;
import com.urbanairship.api.channel.model.email.RegisterEmailChannel;

import java.io.IOException;

public class RegisterEmailChannelSerializer extends JsonSerializer<RegisterEmailChannel> {

    @Override
    public void serialize(RegisterEmailChannel payload, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();
        jgen.writeObjectFieldStart(Constants.CHANNEL);
        jgen.writeStringField(Constants.TYPE, payload.getType().getIdentifier());

        if (payload.getAddress().isPresent()) {
            jgen.writeStringField(Constants.ADDRESS, payload.getAddress().get());
        }

        if (payload.getUaAddress().isPresent()) {
            jgen.writeStringField(Constants.UA_ADDRESS, payload.getUaAddress().get());
        }

        /*Checks keys in the opt-in hashmap to see which matches the opt-in levels in the opt-in level enum.
        * then writes the correct value to the JSON*/
        for (OptInLevel level: OptInLevel.values()
             ) {
            if (payload.getEmailOptInLevel().get().keySet().contains(level)) {
                jgen.writeObjectField(level.getIdentifier(),
                        payload.getEmailOptInLevel().get().get(level));
            }
        }

        if (payload.getTimezone().isPresent()) {
            jgen.writeStringField(Constants.TIMEZONE, payload.getTimezone().get());
        }

        if (payload.getLocaleCountry().isPresent()) {
            jgen.writeStringField(Constants.LOCALE_COUNTRY, payload.getLocaleCountry().get());
        }

        if (payload.getLocaleLanguage().isPresent()) {
            jgen.writeStringField(Constants.LOCALE_LANGUAGE, payload.getLocaleLanguage().get());
        }

        jgen.writeEndObject();
    }
}