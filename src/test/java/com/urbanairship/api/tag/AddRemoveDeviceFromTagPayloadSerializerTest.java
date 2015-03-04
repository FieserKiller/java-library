package com.urbanairship.api.tag;

import com.urbanairship.api.push.parse.PushObjectMapper;
import com.urbanairship.api.tag.model.AddRemoveDeviceFromTagPayload;
import com.urbanairship.api.tag.model.AddRemoveSet;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AddRemoveDeviceFromTagPayloadSerializerTest {

    private static final ObjectMapper MAPPER = PushObjectMapper.getInstance();

    @Test
    public void testSerialization() throws Exception {

        AddRemoveSet ar = AddRemoveSet.newBuilder()
                .add("device1")
                .add("device2")
                .remove("device3")
                .build();

        AddRemoveDeviceFromTagPayload ardftp = AddRemoveDeviceFromTagPayload.newBuilder()
                .setApids(ar)
                .setDevicePins(ar)
                .setDeviceTokens(ar)
                .build();

        String json = MAPPER.writeValueAsString(ardftp);
        String expectedJson = "{\"device_tokens\":{\"add\":[\"device1\",\"device2\"],\"remove\":[\"device3\"]},\"device_pins\":{\"add\":[\"device1\",\"device2\"],\"remove\":[\"device3\"]},\"apids\":{\"add\":[\"device1\",\"device2\"],\"remove\":[\"device3\"]}}";

        assertEquals(expectedJson, json);
    }

    @Test
    public void testChannelsSerialization() throws Exception {

        AddRemoveSet ar = AddRemoveSet.newBuilder()
            .add("device1")
            .add("device2")
            .remove("device3")
            .build();

        AddRemoveDeviceFromTagPayload payload = AddRemoveDeviceFromTagPayload.newBuilder()
            .setIOSChannels(ar)
            .setAndroidChannels(ar)
            .setAmazonChannels(ar)
            .build();

        String json = MAPPER.writeValueAsString(payload);
        String expectedJson = "{\"ios_channels\":{\"add\":[\"device1\",\"device2\"],\"remove\":[\"device3\"]},\"android_channels\":{\"add\":[\"device1\",\"device2\"],\"remove\":[\"device3\"]},\"amazon_channels\":{\"add\":[\"device1\",\"device2\"],\"remove\":[\"device3\"]}}";

        assertEquals(expectedJson, json);
    }
}
