package com.company.apitestingapp.entity;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@JmixEntity
@Table(name = "REQUEST_EXECUTION")
@Entity
public class RequestExecution {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    private UUID id;

    @Column(name = "RESPONSE_BODY")
    @Lob
    private String responseBody;

    @Column(name = "RESPONSE_HEADERS")
    @Lob
    private String responseHeaders;

    @Column(name = "RESPONSE_TIME_MS")
    private Long responseTimeMs;

    @Column(name = "EXECUTED_AT")
    private OffsetDateTime executedAt;

    @Column(name = "SIZE_BYTES")
    private Long sizeBytes;

    @Column(name = "ERROR_MESSAGE")
    @Lob
    private String errorMessage;

    @Column(name = "IS_SUCCESS")
    private Boolean isSuccess;

    @Column(name = "STATUS_CODE")
    private Integer statusCode;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "REQUEST_ID")
    private Request request;


    public Request getRequest(){
        return request;
    }

    public void setRequest(Request request){
        this.request = request;
    }

    public Integer getStatusCode() { return statusCode; }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public Boolean getIsSuccess() {
        return isSuccess;
    }

    public void setIsSuccess(Boolean isSuccess) {
        this.isSuccess = isSuccess;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long size_bytes) {
        this.sizeBytes = size_bytes;
    }

    public OffsetDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(OffsetDateTime executed_at) {
        this.executedAt = executed_at;
    }

    public Long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(Long response_time_ms) {
        this.responseTimeMs = response_time_ms;
    }

    public String getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(String response_headers) {
        this.responseHeaders = response_headers;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String response_body) {
        this.responseBody = response_body;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    // Метод для определения успешности на основе статус кода
    public Boolean determineSuccess() {
        if (statusCode == null) return false;
        return statusCode >= 200 && statusCode < 300;
    }

}