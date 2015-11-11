package com.urbanairship.api.client;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.urbanairship.api.client.model.APIClientResponse;
import com.urbanairship.api.client.model.APIListAllSegmentsResponse;
import com.urbanairship.api.client.model.APIListTagsResponse;
import com.urbanairship.api.client.model.APILocationResponse;
import com.urbanairship.api.location.model.BoundedBox;
import com.urbanairship.api.location.model.Point;
import com.urbanairship.api.segments.model.AudienceSegment;
import com.urbanairship.api.segments.model.TagPredicateBuilder;
import com.urbanairship.api.tag.model.AddRemoveDeviceFromTagPayload;
import com.urbanairship.api.tag.model.AddRemoveSet;
import com.urbanairship.api.tag.model.BatchModificationPayload;
import com.urbanairship.api.tag.model.BatchTagSet;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.log4j.BasicConfigurator;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class APIClientTest {

    public final static String CONTENT_TYPE_KEY = "Content-type";
    public final static String APP_JSON = "application/json";

    static {
        BasicConfigurator.configure();
    }

    @ClassRule
    @Rule
    public static WireMockClassRule wireMockClassRule = new WireMockClassRule();


    @Test(expected = NullPointerException.class)
    public void testAPIClientThrowsForNoAppKey() {
        @SuppressWarnings("UnusedAssignment") APIClient apiClient = APIClient.newBuilder().setKey("foo")
                .build();
    }

    @Test(expected = NullPointerException.class)
    public void testAPIClientThrowsForNoAppSecret() {
        @SuppressWarnings("UnusedAssignment") APIClient apiClient = APIClient.newBuilder().setSecret("foo")
                .build();
    }

    @Test
    public void testAPIClientBuilder() {
        APIClient client = APIClient.newBuilder()
                .setKey("key")
                .setSecret("secret")
                .build();
        assertEquals("App key incorrect", "key", client.getAppKey());
        assertEquals("App secret incorrect", "secret", client.getAppSecret());
        assertFalse(client.getProxyInfo().isPresent());
    }

    @Test
    public void testAPIClientBuilderWithOptionalProxyInfoOptionalCredential() {
        APIClient proxyClient = APIClient.newBuilder()
                .setKey("key")
                .setSecret("secret")
                .setProxyInfo(ProxyInfo.newBuilder()
                        .setProxyHost(new HttpHost("host"))
                        .setProxyCredentials(new UsernamePasswordCredentials("user", "password"))
                        .build())
                .build();

        assertTrue(proxyClient.getProxyInfo().isPresent());
        assertTrue(proxyClient.getProxyInfo().get().getProxyCredentials().isPresent());

        assertEquals(new HttpHost("host"), proxyClient.getProxyInfo().get().getProxyHost());
        assertEquals(new UsernamePasswordCredentials("user", "password"), proxyClient.getProxyInfo().get().getProxyCredentials().get());
    }

    @Test
    public void testAPIClientBuilderWithOptionalProxyInfoNoCredential() {
        APIClient proxyClient = APIClient.newBuilder()
                .setKey("key")
                .setSecret("secret")
                .setProxyInfo(ProxyInfo.newBuilder()
                        .setProxyHost(new HttpHost("host"))
                        .build())
                .build();

        assertTrue(proxyClient.getProxyInfo().isPresent());
        assertFalse(proxyClient.getProxyInfo().get().getProxyCredentials().isPresent());

        assertEquals(new HttpHost("host"), proxyClient.getProxyInfo().get().getProxyHost());
    }

    @Test
    public void testGetUserAgent() {
        APIClient client = APIClient.newBuilder()
                .setKey("key")
                .setSecret("secret")
                .build();
        String userAgent = client.getUserAgent();
        assertNotNull(userAgent);
        assertFalse(userAgent.equals("UNKNOWN"));
        assertFalse(userAgent.equals("UAJavaLib/UNKNOWN"));
        assertFalse(userAgent.equals("UAJavaLib/"));
        assertFalse(userAgent.endsWith("/"));
        assertTrue(userAgent.startsWith("UAJavaLib/"));
    }

    @Test
    public void testAPIClientBuilderWithBasicHttpParams() {
        BasicHttpParams httpParams = new BasicHttpParams();
        httpParams.setParameter(CoreConnectionPNames.SO_TIMEOUT, 10);
        httpParams.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 20);

        APIClient client = APIClient.newBuilder()
                .setKey("key")
                .setSecret("secret")
                .setHttpParams(httpParams)
                .build();

        String socketTimeoutName = "http.socket.timeout";
        String connectionTimeoutName = "http.connection.timeout";
        BasicHttpParams retrievedParams = client.getHttpParams().get();

        assertTrue(retrievedParams.getNames().contains(socketTimeoutName));
        assertTrue(retrievedParams.getNames().contains(connectionTimeoutName));
        assertTrue(retrievedParams.getParameter(socketTimeoutName).equals(10));
        assertTrue(retrievedParams.getParameter(connectionTimeoutName).equals(20));
    }

    @Test
    public void testBaseUriResolutionWithPath() throws URISyntaxException {
        String base = "https://test.com/big/fun/path/";
        String relative = "/api/push/";
        String expected = "https://test.com/big/fun/path/api/push/";

        URI uriBase = new URI(base);
        URI uriNuResolved = APIClient.baseURIResolution(uriBase, relative);
        assertEquals(expected, uriNuResolved.toString());
    }

    @Test
    public void testBaseUriResolutionWithPathWithoutSlash() throws URISyntaxException {
        String base = "https://test.com/big/fun/path";
        String relative = "/api/push/";
        String expected = "https://test.com/big/fun/path/api/push/";

        URI uriBase = new URI(base);
        URI uriNuResolved = APIClient.baseURIResolution(uriBase, relative);
        assertEquals(expected, uriNuResolved.toString());
    }

    @Test
    public void testBaseUriResolutionWithoutPath() throws URISyntaxException {
        String base = "https://test.com/";
        String relative = "/api/push/";
        String expected = "https://test.com/api/push/";

        URI uriBase = new URI(base);
        URI uriNuResolved = APIClient.baseURIResolution(uriBase, relative);
        assertEquals(expected, uriNuResolved.toString());
    }

    @Test
    public void testBaseUriResolutionWithoutSlash() throws URISyntaxException {
        String base = "https://test.com";
        String relative = "/api/push/";
        String expected = "https://test.com/api/push/";

        URI uriBase = new URI(base);
        URI uriNuResolved = APIClient.baseURIResolution(uriBase, relative);
        assertEquals(expected, uriNuResolved.toString());
    }

    @Test
    public void testListTags() {
        // Setup a client
        APIClient client = APIClient.newBuilder()
                .setBaseURI("http://localhost:8080")
                .setKey("key")
                .setSecret("secret")
                .build();

        // Setup a stubbed response for the server
        String listtagresponse = "{\"tags\":[\"Puppies\",\"Kitties\",\"GrumpyCat\"]}";

        stubFor(get(urlEqualTo("/api/tags/"))
                .willReturn(aResponse()
                        .withHeader(CONTENT_TYPE_KEY, "application/json")
                        .withBody(listtagresponse)
                        .withStatus(200)));

        try {
            APIClientResponse<APIListTagsResponse> response = client.listTags();

            // Verify components of the underlying HttpRequest
            verify(getRequestedFor(urlEqualTo("/api/tags/"))
                    .withHeader(CONTENT_TYPE_KEY, equalTo(APP_JSON)));
            List<LoggedRequest> requests = findAll(getRequestedFor(
                    urlEqualTo("/api/tags/")));
            // There should only be one request
            assertEquals(requests.size(), 1);

            // The response is tested elsewhere, just check that it exists
            assertNotNull(response);
            assertNotNull(response.getApiResponse());
            assertNotNull(response.getHttpResponse());
        } catch (Exception ex) {
            fail("Exception thrown " + ex);
        }
    }

    @Test
    public void testCreateTag() {
        // Setup a client
        APIClient client = APIClient.newBuilder()
                .setBaseURI("http://localhost:8080")
                .setKey("key")
                .setSecret("secret")
                .build();

        stubFor(put(urlEqualTo("/api/tags/puppies"))
                .willReturn(aResponse()
                        .withStatus(201)));

        try {
            HttpResponse response = client.createTag("puppies");

            // Verify components of the underlying HttpRequest
            verify(putRequestedFor(urlEqualTo("/api/tags/puppies")));
            List<LoggedRequest> requests = findAll(putRequestedFor(
                    urlEqualTo("/api/tags/puppies")));
            // There should only be one request
            assertEquals(requests.size(), 1);

            // The response is tested elsewhere, just check that it exists
            assertNotNull(response);
            assertEquals(201, response.getStatusLine().getStatusCode());
        } catch (Exception ex) {
            fail("Exception thrown " + ex);
        }
    }

    @Test
    public void testDeleteTag() {
        // Setup a client
        APIClient client = APIClient.newBuilder()
                .setBaseURI("http://localhost:8080")
                .setKey("key")
                .setSecret("secret")
                .build();

        stubFor(delete(urlEqualTo("/api/tags/puppies"))
                .willReturn(aResponse()
                        .withStatus(204)));

        try {
            HttpResponse response = client.deleteTag("puppies");

            // Verify components of the underlying HttpRequest
            verify(deleteRequestedFor(urlEqualTo("/api/tags/puppies"))
                    .withHeader(CONTENT_TYPE_KEY, equalTo(APP_JSON)));
            List<LoggedRequest> requests = findAll(deleteRequestedFor(
                    urlEqualTo("/api/tags/puppies")));
            // There should only be one request
            assertEquals(requests.size(), 1);

            // The response is tested elsewhere, just check that it exists
            assertNotNull(response);
            assertEquals(204, response.getStatusLine().getStatusCode());
        } catch (Exception ex) {
            fail("Exception thrown " + ex);
        }
    }

    @Test
    public void testAddRemoveDevicesFromTag() {
        // Setup a client
        APIClient client = APIClient.newBuilder()
                .setBaseURI("http://localhost:8080")
                .setKey("key")
                .setSecret("secret")
                .build();

        stubFor(post(urlEqualTo("/api/tags/puppies"))
                .willReturn(aResponse()
                        .withStatus(200)));

        try {
            HttpResponse response = client.addRemoveDevicesFromTag("puppies", AddRemoveDeviceFromTagPayload.newBuilder()
                    .setApids(AddRemoveSet.newBuilder().add("device1").build())
                    .setIOSChannels(AddRemoveSet.newBuilder().remove("device2").build())
                    .build());

            // Verify components of the underlying HttpRequest
            List<LoggedRequest> requests = findAll(postRequestedFor(
                    urlEqualTo("/api/tags/puppies")));
            // There should only be one request
            assertEquals(requests.size(), 1);

            // The response is tested elsewhere, just check that it exists
            assertNotNull(response);
            assertEquals(200, response.getStatusLine().getStatusCode());
        } catch (Exception ex) {
            fail("Exception thrown " + ex);
        }
    }

    @Test
    public void testBatchModificationofTags() {
        // Setup a client
        APIClient client = APIClient.newBuilder()
                .setBaseURI("http://localhost:8080")
                .setKey("key")
                .setSecret("secret")
                .build();

        stubFor(post(urlEqualTo("/api/tags/batch/"))
                .willReturn(aResponse()
                        .withStatus(200)));

        try {
            HttpResponse response = client.batchModificationOfTags(BatchModificationPayload.newBuilder()
                    .addBatchObject(BatchTagSet.newBuilder()
                            .setDevice(BatchTagSet.DeviceIdTypes.APID, "device1")
                            .addTag("tag1")
                            .addTag("tag2")
                            .build())
                    .addBatchObject(BatchTagSet.newBuilder()
                            .setDevice(BatchTagSet.DeviceIdTypes.ANDROID_CHANNEL, "device2")
                            .addTag("tag3")
                            .addTag("tag4")
                            .build())
                    .build());

            // Verify components of the underlying HttpRequest
            List<LoggedRequest> requests = findAll(postRequestedFor(
                    urlEqualTo("/api/tags/batch/")));
            // There should only be one request
            assertEquals(requests.size(), 1);

            // The response is tested elsewhere, just check that it exists
            assertNotNull(response);
            assertEquals(200, response.getStatusLine().getStatusCode());
        } catch (Exception ex) {
            fail("Exception thrown " + ex);
        }
    }

    @Test
    public void testGetLocationBoundaryInformationQueryType() {
        // Setup a client
        APIClient client = APIClient.newBuilder()
                .setBaseURI("http://localhost:8080")
                .setKey("key")
                .setSecret("secret")
                .build();

        String jsonResponse = "{\n" +
                "  \"features\":[\n" +
                "    {\n" +
                "      \"type\":\"Feature\",\n" +
                "      \"id\":\"4oFkxX7RcUdirjtaenEQIV\",\n" +
                "      \"properties\":{\n" +
                "        \"source\":\"tiger.census.gov\",\n" +
                "        \"boundary_type_string\":\"City/Place\",\n" +
                "        \"name\":\"San Francisco\",\n" +
                "        \"context\":{\n" +
                "          \"us_state_name\":\"California\",\n" +
                "          \"us_state\":\"CA\"\n" +
                "        },\n" +
                "        \"boundary_type\":\"city\"\n" +
                "      },\n" +
                "      \"bounds\":[\n" +
                "        37.63983,\n" +
                "        -123.173825,\n" +
                "        37.929824,\n" +
                "        -122.28178\n" +
                "      ],\n" +
                "      \"centroid\":[\n" +
                "        37.759715,\n" +
                "        -122.693976\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"type\":\"Feature\",\n" +
                "      \"id\":\"44jJFKMJg1oeYvv9SImLEx\",\n" +
                "      \"properties\":{\n" +
                "        \"source\":\"tiger.census.gov\",\n" +
                "        \"boundary_type_string\":\"City/Place\",\n" +
                "        \"name\":\"South San Francisco\",\n" +
                "        \"context\":{\n" +
                "          \"us_state_name\":\"California\",\n" +
                "          \"us_state\":\"CA\"\n" +
                "        },\n" +
                "        \"boundary_type\":\"city\"\n" +
                "      },\n" +
                "      \"bounds\":[\n" +
                "        37.633916,\n" +
                "        -122.471883,\n" +
                "        37.673132,\n" +
                "        -122.220531\n" +
                "      ],\n" +
                "      \"centroid\":[\n" +
                "        37.652731,\n" +
                "        -122.343222\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        stubFor(get(urlEqualTo("/api/location/?q=San+Francisco&type=city"))
                .willReturn(aResponse()
                        .withHeader(CONTENT_TYPE_KEY, "application/json")
                        .withBody(jsonResponse)
                        .withStatus(200)));

        try {
            APIClientResponse<APILocationResponse> response = client.queryLocationInformation("San Francisco", "city");

            // Verify components of the underlying request
            verify(getRequestedFor(urlEqualTo("/api/location/?q=San+Francisco&type=city"))
                    .withHeader(CONTENT_TYPE_KEY, equalTo(APP_JSON)));
            List<LoggedRequest> requests = findAll(getRequestedFor(urlEqualTo("/api/location/?q=San+Francisco&type=city")));
            assertEquals(requests.size(), 1);

            assertNotNull(response);
            assertNotNull(response.getApiResponse());
            assertNotNull(response.getHttpResponse());

        } catch (Exception ex) {
            fail("Exception " + ex);
        }
    }

    @Test
    public void testGetLocationBoundaryInformationQuery() {
        // Setup a client
        APIClient client = APIClient.newBuilder()
                .setBaseURI("http://localhost:8080")
                .setKey("key")
                .setSecret("secret")
                .build();

        String jsonResponse = "{\n" +
                "  \"features\":[\n" +
                "    {\n" +
                "      \"type\":\"Feature\",\n" +
                "      \"id\":\"4oFkxX7RcUdirjtaenEQIV\",\n" +
                "      \"properties\":{\n" +
                "        \"source\":\"tiger.census.gov\",\n" +
                "        \"boundary_type_string\":\"City/Place\",\n" +
                "        \"name\":\"San Francisco\",\n" +
                "        \"context\":{\n" +
                "          \"us_state_name\":\"California\",\n" +
                "          \"us_state\":\"CA\"\n" +
                "        },\n" +
                "        \"boundary_type\":\"city\"\n" +
                "      },\n" +
                "      \"bounds\":[\n" +
                "        37.63983,\n" +
                "        -123.173825,\n" +
                "        37.929824,\n" +
                "        -122.28178\n" +
                "      ],\n" +
                "      \"centroid\":[\n" +
                "        37.759715,\n" +
                "        -122.693976\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"type\":\"Feature\",\n" +
                "      \"id\":\"44jJFKMJg1oeYvv9SImLEx\",\n" +
                "      \"properties\":{\n" +
                "        \"source\":\"tiger.census.gov\",\n" +
                "        \"boundary_type_string\":\"City/Place\",\n" +
                "        \"name\":\"South San Francisco\",\n" +
                "        \"context\":{\n" +
                "          \"us_state_name\":\"California\",\n" +
                "          \"us_state\":\"CA\"\n" +
                "        },\n" +
                "        \"boundary_type\":\"city\"\n" +
                "      },\n" +
                "      \"bounds\":[\n" +
                "        37.633916,\n" +
                "        -122.471883,\n" +
                "        37.673132,\n" +
                "        -122.220531\n" +
                "      ],\n" +
                "      \"centroid\":[\n" +
                "        37.652731,\n" +
                "        -122.343222\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        stubFor(get(urlEqualTo("/api/location/?q=San+Francisco"))
                .willReturn(aResponse()
                        .withHeader(CONTENT_TYPE_KEY, "application/json")
                        .withBody(jsonResponse)
                        .withStatus(200)));

        try {
            APIClientResponse<APILocationResponse> response = client.queryLocationInformation("San Francisco");

            // Verify components of the underlying request
            verify(getRequestedFor(urlEqualTo("/api/location/?q=San+Francisco"))
                    .withHeader(CONTENT_TYPE_KEY, equalTo(APP_JSON)));
            List<LoggedRequest> requests = findAll(getRequestedFor(urlEqualTo("/api/location/?q=San+Francisco")));
            assertEquals(requests.size(), 1);

            assertNotNull(response);
            assertNotNull(response.getApiResponse());
            assertNotNull(response.getHttpResponse());

        } catch (Exception ex) {
            fail("Exception " + ex);
        }
    }

    @Test
    public void testGetLocationBoundaryInformationPointType() {
        // Setup a client
        APIClient client = APIClient.newBuilder()
                .setBaseURI("http://localhost:8080")
                .setKey("key")
                .setSecret("secret")
                .build();

        String jsonResponse = "{\n" +
                "  \"features\":[\n" +
                "    {\n" +
                "      \"type\":\"Feature\",\n" +
                "      \"id\":\"4oFkxX7RcUdirjtaenEQIV\",\n" +
                "      \"properties\":{\n" +
                "        \"source\":\"tiger.census.gov\",\n" +
                "        \"boundary_type_string\":\"City/Place\",\n" +
                "        \"name\":\"San Francisco\",\n" +
                "        \"context\":{\n" +
                "          \"us_state_name\":\"California\",\n" +
                "          \"us_state\":\"CA\"\n" +
                "        },\n" +
                "        \"boundary_type\":\"city\"\n" +
                "      },\n" +
                "      \"bounds\":[\n" +
                "        37.63983,\n" +
                "        -123.173825,\n" +
                "        37.929824,\n" +
                "        -122.28178\n" +
                "      ],\n" +
                "      \"centroid\":[\n" +
                "        37.759715,\n" +
                "        -122.693976\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"type\":\"Feature\",\n" +
                "      \"id\":\"44jJFKMJg1oeYvv9SImLEx\",\n" +
                "      \"properties\":{\n" +
                "        \"source\":\"tiger.census.gov\",\n" +
                "        \"boundary_type_string\":\"City/Place\",\n" +
                "        \"name\":\"South San Francisco\",\n" +
                "        \"context\":{\n" +
                "          \"us_state_name\":\"California\",\n" +
                "          \"us_state\":\"CA\"\n" +
                "        },\n" +
                "        \"boundary_type\":\"city\"\n" +
                "      },\n" +
                "      \"bounds\":[\n" +
                "        37.633916,\n" +
                "        -122.471883,\n" +
                "        37.673132,\n" +
                "        -122.220531\n" +
                "      ],\n" +
                "      \"centroid\":[\n" +
                "        37.652731,\n" +
                "        -122.343222\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        Point portland = Point.newBuilder()
                .setLatitude(45.52)
                .setLongitude(-122.681944)
                .build();

        stubFor(get(urlEqualTo("/api/location/45.52,-122.681944?type=city"))
                .willReturn(aResponse()
                        .withHeader(CONTENT_TYPE_KEY, "application/json")
                        .withBody(jsonResponse)
                        .withStatus(200)));

        try {
            APIClientResponse<APILocationResponse> response = client.queryLocationInformation(portland, "city");

            // Verify components of the underlying request
            verify(getRequestedFor(urlEqualTo("/api/location/45.52,-122.681944?type=city"))
                    .withHeader(CONTENT_TYPE_KEY, equalTo(APP_JSON)));
            List<LoggedRequest> requests = findAll(getRequestedFor(urlEqualTo("/api/location/45.52,-122.681944?type=city")));
            assertEquals(requests.size(), 1);

            assertNotNull(response);
            assertNotNull(response.getApiResponse());
            assertNotNull(response.getHttpResponse());

        } catch (Exception ex) {
            fail("Exception " + ex);
        }
    }

    @Test
    public void testGetLocationBoundaryInformationPoint() {
        // Setup a client
        APIClient client = APIClient.newBuilder()
                .setBaseURI("http://localhost:8080")
                .setKey("key")
                .setSecret("secret")
                .build();

        String jsonResponse = "{\n" +
                "  \"features\":[\n" +
                "    {\n" +
                "      \"type\":\"Feature\",\n" +
                "      \"id\":\"4oFkxX7RcUdirjtaenEQIV\",\n" +
                "      \"properties\":{\n" +
                "        \"source\":\"tiger.census.gov\",\n" +
                "        \"boundary_type_string\":\"City/Place\",\n" +
                "        \"name\":\"San Francisco\",\n" +
                "        \"context\":{\n" +
                "          \"us_state_name\":\"California\",\n" +
                "          \"us_state\":\"CA\"\n" +
                "        },\n" +
                "        \"boundary_type\":\"city\"\n" +
                "      },\n" +
                "      \"bounds\":[\n" +
                "        37.63983,\n" +
                "        -123.173825,\n" +
                "        37.929824,\n" +
                "        -122.28178\n" +
                "      ],\n" +
                "      \"centroid\":[\n" +
                "        37.759715,\n" +
                "        -122.693976\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"type\":\"Feature\",\n" +
                "      \"id\":\"44jJFKMJg1oeYvv9SImLEx\",\n" +
                "      \"properties\":{\n" +
                "        \"source\":\"tiger.census.gov\",\n" +
                "        \"boundary_type_string\":\"City/Place\",\n" +
                "        \"name\":\"South San Francisco\",\n" +
                "        \"context\":{\n" +
                "          \"us_state_name\":\"California\",\n" +
                "          \"us_state\":\"CA\"\n" +
                "        },\n" +
                "        \"boundary_type\":\"city\"\n" +
                "      },\n" +
                "      \"bounds\":[\n" +
                "        37.633916,\n" +
                "        -122.471883,\n" +
                "        37.673132,\n" +
                "        -122.220531\n" +
                "      ],\n" +
                "      \"centroid\":[\n" +
                "        37.652731,\n" +
                "        -122.343222\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        Point portland = Point.newBuilder()
                .setLatitude(45.52)
                .setLongitude(-122.681944)
                .build();

        stubFor(get(urlEqualTo("/api/location/45.52,-122.681944"))
                .willReturn(aResponse()
                        .withHeader(CONTENT_TYPE_KEY, "application/json")
                        .withBody(jsonResponse)
                        .withStatus(200)));

        try {
            APIClientResponse<APILocationResponse> response = client.queryLocationInformation(portland);

            // Verify components of the underlying request
            verify(getRequestedFor(urlEqualTo("/api/location/45.52,-122.681944"))
                    .withHeader(CONTENT_TYPE_KEY, equalTo(APP_JSON)));
            List<LoggedRequest> requests = findAll(getRequestedFor(urlEqualTo("/api/location/45.52,-122.681944")));
            assertEquals(requests.size(), 1);

            assertNotNull(response);
            assertNotNull(response.getApiResponse());
            assertNotNull(response.getHttpResponse());

        } catch (Exception ex) {
            fail("Exception " + ex);
        }
    }

    @Test
    public void testGetLocationBoundaryInformationBoxType() {
        // Setup a client
        APIClient client = APIClient.newBuilder()
                .setBaseURI("http://localhost:8080")
                .setKey("key")
                .setSecret("secret")
                .build();

        String jsonResponse = "{\n" +
                "  \"features\":[\n" +
                "    {\n" +
                "      \"type\":\"Feature\",\n" +
                "      \"id\":\"4oFkxX7RcUdirjtaenEQIV\",\n" +
                "      \"properties\":{\n" +
                "        \"source\":\"tiger.census.gov\",\n" +
                "        \"boundary_type_string\":\"City/Place\",\n" +
                "        \"name\":\"San Francisco\",\n" +
                "        \"context\":{\n" +
                "          \"us_state_name\":\"California\",\n" +
                "          \"us_state\":\"CA\"\n" +
                "        },\n" +
                "        \"boundary_type\":\"city\"\n" +
                "      },\n" +
                "      \"bounds\":[\n" +
                "        37.63983,\n" +
                "        -123.173825,\n" +
                "        37.929824,\n" +
                "        -122.28178\n" +
                "      ],\n" +
                "      \"centroid\":[\n" +
                "        37.759715,\n" +
                "        -122.693976\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"type\":\"Feature\",\n" +
                "      \"id\":\"44jJFKMJg1oeYvv9SImLEx\",\n" +
                "      \"properties\":{\n" +
                "        \"source\":\"tiger.census.gov\",\n" +
                "        \"boundary_type_string\":\"City/Place\",\n" +
                "        \"name\":\"South San Francisco\",\n" +
                "        \"context\":{\n" +
                "          \"us_state_name\":\"California\",\n" +
                "          \"us_state\":\"CA\"\n" +
                "        },\n" +
                "        \"boundary_type\":\"city\"\n" +
                "      },\n" +
                "      \"bounds\":[\n" +
                "        37.633916,\n" +
                "        -122.471883,\n" +
                "        37.673132,\n" +
                "        -122.220531\n" +
                "      ],\n" +
                "      \"centroid\":[\n" +
                "        37.652731,\n" +
                "        -122.343222\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        BoundedBox california = new BoundedBox(Point.newBuilder()
                .setLatitude(32.5343)
                .setLongitude(-124.4096)
                .build(), Point.newBuilder()
                .setLatitude(42.0095)
                .setLongitude(-114.1308)
                .build());

        stubFor(get(urlEqualTo("/api/location/32.5343,-124.4096,42.0095,-114.1308?type=city"))
                .willReturn(aResponse()
                        .withHeader(CONTENT_TYPE_KEY, "application/json")
                        .withBody(jsonResponse)
                        .withStatus(200)));

        try {
            APIClientResponse<APILocationResponse> response = client.queryLocationInformation(california, "city");

            // Verify components of the underlying request
            verify(getRequestedFor(urlEqualTo("/api/location/32.5343,-124.4096,42.0095,-114.1308?type=city"))
                    .withHeader(CONTENT_TYPE_KEY, equalTo(APP_JSON)));
            List<LoggedRequest> requests = findAll(getRequestedFor(urlEqualTo("/api/location/32.5343,-124.4096,42.0095,-114.1308?type=city")));
            assertEquals(requests.size(), 1);

            assertNotNull(response);
            assertNotNull(response.getApiResponse());
            assertNotNull(response.getHttpResponse());

        } catch (Exception ex) {
            fail("Exception " + ex);
        }
    }

    @Test
    public void testGetLocationBoundaryInformationBox() {
        // Setup a client
        APIClient client = APIClient.newBuilder()
                .setBaseURI("http://localhost:8080")
                .setKey("key")
                .setSecret("secret")
                .build();

        String jsonResponse = "{\n" +
                "  \"features\":[\n" +
                "    {\n" +
                "      \"type\":\"Feature\",\n" +
                "      \"id\":\"4oFkxX7RcUdirjtaenEQIV\",\n" +
                "      \"properties\":{\n" +
                "        \"source\":\"tiger.census.gov\",\n" +
                "        \"boundary_type_string\":\"City/Place\",\n" +
                "        \"name\":\"San Francisco\",\n" +
                "        \"context\":{\n" +
                "          \"us_state_name\":\"California\",\n" +
                "          \"us_state\":\"CA\"\n" +
                "        },\n" +
                "        \"boundary_type\":\"city\"\n" +
                "      },\n" +
                "      \"bounds\":[\n" +
                "        37.63983,\n" +
                "        -123.173825,\n" +
                "        37.929824,\n" +
                "        -122.28178\n" +
                "      ],\n" +
                "      \"centroid\":[\n" +
                "        37.759715,\n" +
                "        -122.693976\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"type\":\"Feature\",\n" +
                "      \"id\":\"44jJFKMJg1oeYvv9SImLEx\",\n" +
                "      \"properties\":{\n" +
                "        \"source\":\"tiger.census.gov\",\n" +
                "        \"boundary_type_string\":\"City/Place\",\n" +
                "        \"name\":\"South San Francisco\",\n" +
                "        \"context\":{\n" +
                "          \"us_state_name\":\"California\",\n" +
                "          \"us_state\":\"CA\"\n" +
                "        },\n" +
                "        \"boundary_type\":\"city\"\n" +
                "      },\n" +
                "      \"bounds\":[\n" +
                "        37.633916,\n" +
                "        -122.471883,\n" +
                "        37.673132,\n" +
                "        -122.220531\n" +
                "      ],\n" +
                "      \"centroid\":[\n" +
                "        37.652731,\n" +
                "        -122.343222\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        BoundedBox california = new BoundedBox(Point.newBuilder()
                .setLatitude(32.5343)
                .setLongitude(-124.4096)
                .build(), Point.newBuilder()
                .setLatitude(42.0095)
                .setLongitude(-114.1308)
                .build());

        stubFor(get(urlEqualTo("/api/location/32.5343,-124.4096,42.0095,-114.1308"))
                .willReturn(aResponse()
                        .withHeader(CONTENT_TYPE_KEY, "application/json")
                        .withBody(jsonResponse)
                        .withStatus(200)));

        try {
            APIClientResponse<APILocationResponse> response = client.queryLocationInformation(california);

            // Verify components of the underlying request
            verify(getRequestedFor(urlEqualTo("/api/location/32.5343,-124.4096,42.0095,-114.1308"))
                    .withHeader(CONTENT_TYPE_KEY, equalTo(APP_JSON)));
            List<LoggedRequest> requests = findAll(getRequestedFor(urlEqualTo("/api/location/32.5343,-124.4096,42.0095,-114.1308")));
            assertEquals(requests.size(), 1);

            assertNotNull(response);
            assertNotNull(response.getApiResponse());
            assertNotNull(response.getHttpResponse());

        } catch (Exception ex) {
            fail("Exception " + ex);
        }
    }

    @Test
    public void testListAllSegments() {
        // Setup a client
        APIClient client = APIClient.newBuilder()
                .setBaseURI("http://localhost:8080")
                .setKey("key")
                .setSecret("secret")
                .build();

        String testresponse = "{\n" +
                "   \"next_page\": \"https://go.urbanairship.com/api/segments?limit=1&sort=id&order=asc&start=3832cf72-cb44-4132-a11f-eafb41b82f64\",\n" +
                "   \"segments\": [\n" +
                "      {\n" +
                "         \"creation_date\": 1346248822220,\n" +
                "         \"display_name\": \"A segment\",\n" +
                "         \"id\": \"00c0d899-a595-4c66-9071-bc59374bbe6b\",\n" +
                "         \"modification_date\": 1346248822221\n" +
                "      }\n" +
                "   ]\n" +
                "}";

        stubFor(get(urlEqualTo("/api/segments/"))
                .willReturn(aResponse()
                        .withHeader(CONTENT_TYPE_KEY, "application/json")
                        .withHeader("Link", "NextPage")
                        .withBody(testresponse)
                        .withStatus(200)));

        try {
            APIClientResponse<APIListAllSegmentsResponse> response = client.listAllSegments();

            // Verify components of the underlying HttpRequest
            verify(getRequestedFor(urlEqualTo("/api/segments/"))
                    .withHeader(CONTENT_TYPE_KEY, equalTo(APP_JSON)));

            List<LoggedRequest> requests = findAll(getRequestedFor(
                    urlEqualTo("/api/segments/")));

            // There should only be one request
            assertEquals(requests.size(), 1);

            // The response is tested elsewhere, just check that it exists
            assertNotNull(response);
            assertNotNull(response.getApiResponse());
            assertNotNull(response.getHttpResponse());
            assertNotNull(response.getApiResponse().getNextPage());
            assertNotNull(response.getApiResponse().getSegments());

        } catch (Exception ex) {
            fail("Exception thrown " + ex);
        }
    }

    @Test
    public void testListAllSegmentsNextPage() {
        // Setup a client
        APIClient client = APIClient.newBuilder()
                .setBaseURI("http://localhost:8080")
                .setKey("key")
                .setSecret("secret")
                .build();

        String testresponse = "{\n" +
                "   \"next_page\": \"https://go.urbanairship.com/api/segments?limit=1&sort=id&order=asc&start=3832cf72-cb44-4132-a11f-eafb41b82f64\",\n" +
                "   \"segments\": [\n" +
                "      {\n" +
                "         \"creation_date\": 1346248822220,\n" +
                "         \"display_name\": \"A segment\",\n" +
                "         \"id\": \"00c0d899-a595-4c66-9071-bc59374bbe6b\",\n" +
                "         \"modification_date\": 1346248822221\n" +
                "      }\n" +
                "   ]\n" +
                "}";

        stubFor(get(urlEqualTo("/api/segments?limit=1&sort=id&order=asc&start=3832cf72-cb44-4132-a11f-eafb41b82f64"))
                .willReturn(aResponse()
                        .withHeader(CONTENT_TYPE_KEY, "application/json")
                        .withHeader("Link", "NextPage")
                        .withBody(testresponse)
                        .withStatus(200)));

        String nextPage = "https://go.urbanairship.com/api/segments?limit=1&sort=id&order=asc&start=3832cf72-cb44-4132-a11f-eafb41b82f64";

        try {
            APIClientResponse<APIListAllSegmentsResponse> response = client.listAllSegments(nextPage);

            // Verify components of the underlying HttpRequest
            verify(getRequestedFor(urlEqualTo("/api/segments?limit=1&sort=id&order=asc&start=3832cf72-cb44-4132-a11f-eafb41b82f64"))
                    .withHeader(CONTENT_TYPE_KEY, equalTo(APP_JSON)));

            List<LoggedRequest> requests = findAll(getRequestedFor(
                    urlEqualTo("/api/segments?limit=1&sort=id&order=asc&start=3832cf72-cb44-4132-a11f-eafb41b82f64")));

            // There should only be one request
            assertEquals(requests.size(), 1);

            // The response is tested elsewhere, just check that it exists
            assertNotNull(response);
            assertNotNull(response.getApiResponse());
            assertNotNull(response.getHttpResponse());
            assertNotNull(response.getApiResponse().getNextPage());
            assertNotNull(response.getApiResponse().getSegments());

        } catch (Exception ex) {
            fail("Exception thrown " + ex);
        }
    }

    @Test
    public void testListAllSegmentsWithParameters() {
        // Setup a client
        APIClient client = APIClient.newBuilder()
                .setBaseURI("http://localhost:8080")
                .setKey("key")
                .setSecret("secret")
                .build();

        String testresponse = "{\n" +
                "   \"next_page\": \"https://go.urbanairship.com/api/segments?limit=1&sort=id&order=asc&start=3832cf72-cb44-4132-a11f-eafb41b82f64\",\n" +
                "   \"segments\": [\n" +
                "      {\n" +
                "         \"creation_date\": 1346248822220,\n" +
                "         \"display_name\": \"A segment\",\n" +
                "         \"id\": \"00c0d899-a595-4c66-9071-bc59374bbe6b\",\n" +
                "         \"modification_date\": 1346248822221\n" +
                "      }\n" +
                "   ]\n" +
                "}";

        stubFor(get(urlEqualTo("/api/segments?start=3832cf72-cb44-4132-a11f-eafb41b82f64&limit=1&order=asc"))
                .willReturn(aResponse()
                        .withHeader(CONTENT_TYPE_KEY, "application/json")
                        .withHeader("Link", "NextPage")
                        .withBody(testresponse)
                        .withStatus(200)));

        try {
            APIClientResponse<APIListAllSegmentsResponse> response = client.listAllSegments("3832cf72-cb44-4132-a11f-eafb41b82f64", 1, "asc");

            // Verify components of the underlying HttpRequest
            verify(getRequestedFor(urlEqualTo("/api/segments?start=3832cf72-cb44-4132-a11f-eafb41b82f64&limit=1&order=asc"))
                    .withHeader(CONTENT_TYPE_KEY, equalTo(APP_JSON)));

            List<LoggedRequest> requests = findAll(getRequestedFor(
                    urlEqualTo("/api/segments?start=3832cf72-cb44-4132-a11f-eafb41b82f64&limit=1&order=asc")));

            // There should only be one request
            assertEquals(requests.size(), 1);

            // The response is tested elsewhere, just check that it exists
            assertNotNull(response);
            assertNotNull(response.getApiResponse());
            assertNotNull(response.getHttpResponse());
            assertNotNull(response.getApiResponse().getNextPage());
            assertNotNull(response.getApiResponse().getSegments());

        } catch (Exception ex) {
            fail("Exception thrown " + ex);
        }
    }

    @Test
    public void testListSegment() {
        // Setup a client
        APIClient client = APIClient.newBuilder()
                .setBaseURI("http://localhost:8080")
                .setKey("key")
                .setSecret("secret")
                .build();

        String testresponse = "{  \n" +
                "  \"display_name\":\"2014-11-07T14:26:56.749-08:00\",\n" +
                "  \"criteria\":{  \n" +
                "    \"and\":[  \n" +
                "      {  \n" +
                "        \"location\":{  \n" +
                "          \"us_state\":\"OR\",\n" +
                "          \"date\":{  \n" +
                "            \"days\":{  \n" +
                "              \"start\":\"2014-11-02\",\n" +
                "              \"end\":\"2014-11-07\"\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      },\n" +
                "      {  \n" +
                "        \"location\":{  \n" +
                "          \"us_state\":\"CA\",\n" +
                "          \"date\":{  \n" +
                "            \"recent\":{  \n" +
                "              \"months\":3\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      },\n" +
                "      {  \n" +
                "        \"or\":[  \n" +
                "          {  \n" +
                "            \"tag\":\"tag1\"\n" +
                "          },\n" +
                "          {  \n" +
                "            \"tag\":\"tag2\"\n" +
                "          }\n" +
                "        ]\n" +
                "      },\n" +
                "      {  \n" +
                "        \"not\":{  \n" +
                "          \"tag\":\"not-tag\"\n" +
                "        }\n" +
                "      },\n" +
                "      {  \n" +
                "        \"not\":{  \n" +
                "          \"and\":[  \n" +
                "            {  \n" +
                "              \"location\":{  \n" +
                "                \"us_state\":\"WA\",\n" +
                "                \"date\":{  \n" +
                "                  \"months\":{  \n" +
                "                    \"start\":\"2011-05\",\n" +
                "                    \"end\":\"2012-02\"\n" +
                "                  }\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            {  \n" +
                "              \"tag\":\"woot\"\n" +
                "            }\n" +
                "          ]\n" +
                "        }\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";

        stubFor(get(urlEqualTo("/api/segments/abc"))
                .willReturn(aResponse()
                        .withHeader(CONTENT_TYPE_KEY, "application/json")
                        .withBody(testresponse)
                        .withStatus(200)));

        try {
            APIClientResponse<AudienceSegment> response = client.listSegment("abc");

            // Verify components of the underlying HttpRequest
            verify(getRequestedFor(urlEqualTo("/api/segments/abc"))
                    .withHeader(CONTENT_TYPE_KEY, equalTo(APP_JSON)));

            List<LoggedRequest> requests = findAll(getRequestedFor(
                    urlEqualTo("/api/segments/abc")));

            // There should only be one request
            assertEquals(requests.size(), 1);

            // The response is tested elsewhere, just check that it exists
            assertNotNull(response);
            assertNotNull(response.getApiResponse());
            assertNotNull(response.getHttpResponse());
            assertNotNull(response.getApiResponse().getDisplayName());
        } catch (Exception ex) {
            fail("Exception thrown " + ex);
        }
    }

    @Test
    public void testCreateSegment() {
        // Setup a client
        APIClient client = APIClient.newBuilder()
                .setBaseURI("http://localhost:8080")
                .setKey("key")
                .setSecret("secret")
                .build();

        stubFor(post(urlEqualTo("/api/segments/"))
                .willReturn(aResponse()
                        .withHeader(CONTENT_TYPE_KEY, "application/json")
                        .withStatus(200)));

        try {
            HttpResponse response = client.createSegment(
                    AudienceSegment.newBuilder()
                            .setDisplayName("hi")
                            .setRootPredicate(TagPredicateBuilder.newInstance()
                                    .setTag("tag")
                                    .build())
                            .build()
            );

            // Verify components of the underlying HttpRequest
            verify(postRequestedFor(urlEqualTo("/api/segments/"))
                    .withHeader(CONTENT_TYPE_KEY, equalTo(APP_JSON)));

            List<LoggedRequest> requests = findAll(postRequestedFor(
                    urlEqualTo("/api/segments/")));

            // There should only be one request
            assertEquals(requests.size(), 1);

            // The response is tested elsewhere, just check that it exists
            assertNotNull(response);
        } catch (Exception ex) {
            fail("Exception thrown " + ex);
        }
    }

    @Test
    public void testChangeSegment() {
        // Setup a client
        APIClient client = APIClient.newBuilder()
                .setBaseURI("http://localhost:8080")
                .setKey("key")
                .setSecret("secret")
                .build();

        stubFor(put(urlEqualTo("/api/segments/abc"))
                .willReturn(aResponse()
                        .withHeader(CONTENT_TYPE_KEY, "application/json")
                        .withStatus(200)));

        try {
            HttpResponse response = client.changeSegment("abc",
                    AudienceSegment.newBuilder()
                            .setDisplayName("hi")
                            .setRootPredicate(TagPredicateBuilder.newInstance()
                                    .setTag("tag")
                                    .build())
                            .build()
            );

            // Verify components of the underlying HttpRequest
            verify(putRequestedFor(urlEqualTo("/api/segments/abc"))
                    .withHeader(CONTENT_TYPE_KEY, equalTo(APP_JSON)));

            List<LoggedRequest> requests = findAll(putRequestedFor(
                    urlEqualTo("/api/segments/abc")));

            // There should only be one request
            assertEquals(requests.size(), 1);

            // The response is tested elsewhere, just check that it exists
            assertNotNull(response);
        } catch (Exception ex) {
            fail("Exception thrown " + ex);
        }
    }

    @Test
    public void testDeleteSegment() {
        // Setup a client
        APIClient client = APIClient.newBuilder()
                .setBaseURI("http://localhost:8080")
                .setKey("key")
                .setSecret("secret")
                .build();

        stubFor(delete(urlEqualTo("/api/segments/abc"))
                .willReturn(aResponse()
                        .withHeader(CONTENT_TYPE_KEY, "application/json")
                        .withStatus(200)));

        try {
            HttpResponse response = client.deleteSegment("abc");

            // Verify components of the underlying HttpRequest
            verify(deleteRequestedFor(urlEqualTo("/api/segments/abc"))
                    .withHeader(CONTENT_TYPE_KEY, equalTo(APP_JSON)));

            List<LoggedRequest> requests = findAll(deleteRequestedFor(
                    urlEqualTo("/api/segments/abc")));

            // There should only be one request
            assertEquals(requests.size(), 1);

            // The response is tested elsewhere, just check that it exists
            assertNotNull(response);
        } catch (Exception ex) {
            fail("Exception thrown " + ex);
        }
    }
}