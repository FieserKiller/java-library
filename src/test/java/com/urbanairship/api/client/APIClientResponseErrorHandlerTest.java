package com.urbanairship.api.client;

import com.urbanairship.api.push.model.PushResponse;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.ResponseHandler;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class APIClientResponseErrorHandlerTest {

    /* Header keys, values */
    public final static String CONTENT_TYPE_KEY = "Content-type";
    public final static String CONTENT_TYPE_TEXT_HTML = "text/html";
    public final static String CONTENT_TYPE_JSON = "application/json";
    public final static String UA_JSON_RESPONSE =
        "application/vnd.urbanairship+json; version=3; charset=utf8;";

    /*
    Test that a failed message generates the proper exception, with the
    appropriate data. The APIError, APIErrorDetails, and Location objects
    are tested in their respective test classes, this test only verifies
    that they were properly setup during the process of building the
    exception
     */
    @Test
    public void testAPIV3Error() {

        /* Build a BasicHttpResponse */
        String pushJSON = "{\"ok\" : false,\"operation_id\" : \"OpID\"," +
            "\"error\" : \"Could not parse request body\"," +
            "\"error_code\" : 40000," +
            "\"details\" : {\"error\" : \"Unexpected token '#'\"," +
            "\"location\" : {\"line\" : 10,\"column\" : 3}}}";
        HttpResponse httpResponse = new BasicHttpResponse(new BasicStatusLine(
            new ProtocolVersion("HTTP", 1, 1), 400, "Unauthorized"));
        InputStreamEntity inputStreamEntity = new InputStreamEntity(
            new ByteArrayInputStream(pushJSON.getBytes()),
            pushJSON.getBytes().length);
        httpResponse.setEntity(inputStreamEntity);
        httpResponse.setHeader(new BasicHeader(CONTENT_TYPE_KEY, UA_JSON_RESPONSE));

        /* Test handling */
        ResponseHandler<Response<PushResponse>> handler = new ResponseHandler<Response<PushResponse>>() {
            @Override
            public Response<PushResponse> handleResponse(HttpResponse httpResponse) throws IOException {
                int statusCode = httpResponse.getStatusLine().getStatusCode();

                if (statusCode >= 200 && statusCode < 300) {
                    return new Response<PushResponse>(null, null, httpResponse.getStatusLine().getStatusCode());
                } else {
                    throw APIRequestException.exceptionForResponse(httpResponse);
                }
            }
        };

        try {
            handler.handleResponse(httpResponse);
        } catch (APIRequestException ex) {
            System.out.println("Exception " + ex.getMessage());
            APIError error = ex.getError().get();
            System.out.println("Error " + error);
            assertTrue("Operation ID is incorrect",
                error.getOperationId().get().equals("OpID"));
            assertTrue("Error code is incorrect",
                error.getErrorCode().get().equals(40000));
            APIErrorDetails details = error.getDetails().get();
            APIErrorDetails.Location errorLocation = details.getLocation().get();
            assertTrue("Location not setup properly",
                errorLocation.getLine().equals(10));
        } catch (Exception ex) {
            fail("Failed with incorrect exception " + ex.getMessage());
        }

    }

    /*
    Test for error where the default JSON error message is returned instead
    of the v3 error message is returned. Default message json is in the form
    {"message":"description"}
     */
    @Test
    public void testDeprecatedJSONError() {
        /* Build a BasicHttpResponse */
        String pushJSON = "{\"message\":\"Unauthorized\"}";
        HttpResponse httpResponse = new BasicHttpResponse(new BasicStatusLine(
            new ProtocolVersion("HTTP", 1, 1), 400, "Unauthorized"));
        InputStreamEntity inputStreamEntity = new InputStreamEntity(
            new ByteArrayInputStream(pushJSON.getBytes()),
            pushJSON.getBytes().length);
        httpResponse.setEntity(inputStreamEntity);
        httpResponse.setHeader(new BasicHeader(CONTENT_TYPE_KEY, CONTENT_TYPE_JSON));
        ResponseHandler<Response<PushResponse>> handler = new ResponseHandler<Response<PushResponse>>() {
            @Override
            public Response<PushResponse> handleResponse(HttpResponse httpResponse) throws IOException {
                int statusCode = httpResponse.getStatusLine().getStatusCode();

                if (statusCode >= 200 && statusCode < 300) {
                    return new Response<PushResponse>(null, null, httpResponse.getStatusLine().getStatusCode());
                } else {
                    throw APIRequestException.exceptionForResponse(httpResponse);
                }
            }
        };

        try {
            handler.handleResponse(httpResponse);
        } catch (APIRequestException ex) {

            APIError error = ex.getError().get();
            String errorMessage = error.getError();
            assertTrue("Error message incorrect", errorMessage.equals("Unauthorized"));
            return;
        } catch (Exception ex) {
            fail("Failed with incorrect exception " + ex);
        }
        fail("Test should have succeeded by now");
    }

    /*
    Test the deprecated API response where only a string is returned.
     */
    @Test
    public void testDeprecatedStringError() {
        /* Build a BasicHttpResponse */
        String errorString = "Unauthorized";
        HttpResponse httpResponse = new BasicHttpResponse(new BasicStatusLine(
            new ProtocolVersion("HTTP", 1, 1), 400, "Unauthorized"));
        InputStreamEntity inputStreamEntity = new InputStreamEntity(
            new ByteArrayInputStream(errorString.getBytes()),
            errorString.getBytes().length);
        httpResponse.setEntity(inputStreamEntity);
        httpResponse.setHeader(new BasicHeader(CONTENT_TYPE_KEY,
            CONTENT_TYPE_TEXT_HTML));

        ResponseHandler<Response<PushResponse>> handler = new ResponseHandler<Response<PushResponse>>() {
            @Override
            public Response<PushResponse> handleResponse(HttpResponse httpResponse) throws IOException {
                int statusCode = httpResponse.getStatusLine().getStatusCode();

                if (statusCode >= 200 && statusCode < 300) {
                    return new Response<PushResponse>(null, null, httpResponse.getStatusLine().getStatusCode());
                } else {
                    throw APIRequestException.exceptionForResponse(httpResponse);
                }
            }
        };

        try {
            handler.handleResponse(httpResponse);
        } catch (APIRequestException ex) {
            APIError error = ex.getError().get();
            assertTrue("String error message is incorrect",
                error.getError().equals("Unauthorized"));
            return;
        } catch (Exception ex) {
            fail("Failed with incorrect exception " + ex);
        }
        fail("Test should have succeeded by now");
    }
}
