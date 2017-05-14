/*
 * Copyright (c) 2013-2016. Urban Airship and Contributors
 */

package com.urbanairship.api.reports.parse;

import com.urbanairship.api.push.parse.PushObjectMapper;
import com.urbanairship.api.reports.model.PlatformStats;
import com.urbanairship.api.reports.model.PlatformStatsResponse;
import com.urbanairship.api.reports.model.PushInfoResponse;
import com.urbanairship.api.reports.model.PushListingResponse;
import com.urbanairship.api.reports.model.StatisticsResponse;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.module.SimpleModule;


public class ReportsObjectMapper {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final SimpleModule MODULE = new SimpleModule("Reports API Module", new Version(1, 0, 0, null));

    static {
        MODULE.addDeserializer(PushInfoResponse.class, new PushInfoResponseDeserializer());
        MODULE.addDeserializer(PushListingResponse.class, new PushListingResponseDeserializer());
        MODULE.addDeserializer(StatisticsResponse.class, new StatisticsResponseDeserializer());
        MODULE.addDeserializer(PlatformStats.class, new PlatformStatsDeserializer());
        MODULE.addDeserializer(PlatformStatsResponse.class, new PlatformStatsResponseDeserializer());

        MAPPER.registerModule(MODULE);
        MAPPER.registerModule(PushObjectMapper.getModule());
    }

    public static SimpleModule getModule() {
        return MODULE;
    }

    public static ObjectMapper getInstance() {
        return MAPPER;
    }

    private ReportsObjectMapper() {
    }
}
