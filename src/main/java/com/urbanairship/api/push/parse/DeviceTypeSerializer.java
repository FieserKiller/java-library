/*
 * Copyright (c) 2013-2016.  Urban Airship and Contributors
 */

package com.urbanairship.api.push.parse;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.urbanairship.api.push.model.DeviceType;

import java.io.IOException;

public class DeviceTypeSerializer extends JsonSerializer<DeviceType> {

    @Override
    public void serialize(DeviceType deviceType, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeString(deviceType.getIdentifier());
    }
}
