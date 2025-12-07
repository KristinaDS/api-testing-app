package com.company.apitestingapp.entity;

import io.jmix.core.metamodel.datatype.EnumClass;

public enum StatusCode implements EnumClass<Integer> {
    // 1xx Informational
    CONTINUE(100, "Continue"),
    SWITCHING_PROTOCOLS(101, "Switching Protocols"),
    PROCESSING(102, "Processing"),

    // 2xx Success
    OK(200, "OK"),
    CREATED(201, "Created"),
    ACCEPTED(202, "Accepted"),
    NON_AUTHORITATIVE_INFORMATION(203, "Non-Authoritative Information"),
    NO_CONTENT(204, "No Content"),
    RESET_CONTENT(205, "Reset Content"),
    PARTIAL_CONTENT(206, "Partial Content"),

    // 3xx Redirection
    MULTIPLE_CHOICES(300, "Multiple Choices"),
    MOVED_PERMANENTLY(301, "Moved Permanently"),
    FOUND(302, "Found"),
    SEE_OTHER(303, "See Other"),
    NOT_MODIFIED(304, "Not Modified"),
    TEMPORARY_REDIRECT(307, "Temporary Redirect"),
    PERMANENT_REDIRECT(308, "Permanent Redirect"),

    // 4xx Client Errors
    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    PAYMENT_REQUIRED(402, "Payment Required"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
    NOT_ACCEPTABLE(406, "Not Acceptable"),
    REQUEST_TIMEOUT(408, "Request Timeout"),
    CONFLICT(409, "Conflict"),
    GONE(410, "Gone"),
    LENGTH_REQUIRED(411, "Length Required"),
    PRECONDITION_FAILED(412, "Precondition Failed"),
    PAYLOAD_TOO_LARGE(413, "Payload Too Large"),
    URI_TOO_LONG(414, "URI Too Long"),
    UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),
    TOO_MANY_REQUESTS(429, "Too Many Requests"),

    // 5xx Server Errors
    INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
    NOT_IMPLEMENTED(501, "Not Implemented"),
    BAD_GATEWAY(502, "Bad Gateway"),
    SERVICE_UNAVAILABLE(503, "Service Unavailable"),
    GATEWAY_TIMEOUT(504, "Gateway Timeout"),
    HTTP_VERSION_NOT_SUPPORTED(505, "HTTP Version Not Supported");


    private final int code;
    private final String reason;

    StatusCode(int code, String reason) {
        this.code = code;
        this.reason = reason;
    }

    public int getCode(){
        return code;
    }
    public String getReason(){
        return reason;
    }

    public static StatusCode valueOf(int statusCode) {
        for (StatusCode status : values()) {
            if (status.code == statusCode) {
                return status;
            }
        }
        throw new IllegalArgumentException("No matching constant for [" + statusCode + "]");
    }

    public static StatusCode fromCode(int statusCode) {
        for (StatusCode status : values()) {
            if (status.code == statusCode) {
                return status;
            }
        }
        return null; // или выбросить исключение
    }

    public boolean is1xxInformational() {
        return code >= 100 && code < 200;
    }

    public boolean is2xxSuccessful() {
        return code >= 200 && code < 300;
    }

    public boolean is3xxRedirection() {
        return code >= 300 && code < 400;
    }

    public boolean is4xxClientError() {
        return code >= 400 && code < 500;
    }

    public boolean is5xxServerError() {
        return code >= 500 && code < 600;
    }

    public boolean isError() {
        return is4xxClientError() || is5xxServerError();
    }

    @Override
    public String toString() {
        return code + " " + reason;
    }

    @Override
    public Integer getId() {
        return 0;
    }
}