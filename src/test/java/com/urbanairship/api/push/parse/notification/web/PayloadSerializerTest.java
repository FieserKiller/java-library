package com.urbanairship.api.push.parse.notification.web;

import com.urbanairship.api.push.model.notification.web.WebDevicePayload;
import com.urbanairship.api.push.model.notification.web.WebIcon;
import com.urbanairship.api.push.parse.PushObjectMapper;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PayloadSerializerTest {

    private static final ObjectMapper MAPPER = PushObjectMapper.getInstance();

    @Test
    public void testFullWebPayload() throws Exception {
        WebIcon webIcon = WebIcon.newBuilder()
                .setUrl("https://i.ytimg.com/vi/PNgykntrIzE/maxresdefault.jpg")
                .build();

        WebDevicePayload webPayload = WebDevicePayload.newBuilder()
                .setAlert("Web specific alert")
                .setTitle("Web title")
                .addExtraEntry("extrakey", "extravalue")
                .setWebIcon(webIcon)
                .build();

        String expected = "{" +
                    "\"alert\":\"Web specific alert\"," +
                    "\"extra\":{\"extrakey\":\"extravalue\"}," +
                    "\"icon\":{\"url\":\"https://i.ytimg.com/vi/PNgykntrIzE/maxresdefault.jpg\"}," +
                    "\"title\":\"Web title\"" +
                "}";

        String parsedJson = MAPPER.writeValueAsString(webPayload);
        WebDevicePayload roundTripWebPayload = MAPPER.readValue(parsedJson, WebDevicePayload.class);

        JsonNode jsonFromObject = MAPPER.readTree(parsedJson);
        JsonNode jsonFromString = MAPPER.readTree(expected);

        assertEquals(jsonFromObject, jsonFromString);
        assertEquals(roundTripWebPayload, webPayload);
    }
}
