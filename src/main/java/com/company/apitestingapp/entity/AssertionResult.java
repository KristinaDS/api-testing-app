package com.company.apitestingapp.entity;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;

import java.util.UUID;

@JmixEntity
@Table(name = "ASSERTION_RESULT")
@Entity
public class AssertionResult {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TEST_EXECUTION_RESULT_ID")
    private TestExecutionResult testExecutionResult;

    @Column(name = "ASSERTION_NAME")
    private String assertionName;

    @Column(name = "ASSERTION_TYPE")
    private String assertionType;

    @Column(name = "EXPECTED_VALUE")
    @Lob
    private String expectedValue;

    @Column(name = "ACTUAL_VALUE")
    @Lob
    private String actualValue;

    @Column(name = "PASSED")
    private Boolean passed;

    @Column(name = "ERROR_MESSAGE")
    @Lob
    private String errorMessage;


    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public TestExecutionResult getTestExecutionResult() {
        return testExecutionResult;
    }

    public void setTestExecutionResult(TestExecutionResult testExecutionResult) {
        this.testExecutionResult = testExecutionResult;
    }

    public String getAssertionName() {
        return assertionName;
    }

    public void setAssertionName(String assertionName) {
        this.assertionName = assertionName;
    }

    public String getAssertionType() {
        return assertionType;
    }

    public void setAssertionType(String assertionType) {
        this.assertionType = assertionType;
    }

    public String getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(String expectedValue) {
        this.expectedValue = expectedValue;
    }

    public String getActualValue() {
        return actualValue;
    }

    public void setActualValue(String actualValue) {
        this.actualValue = actualValue;
    }

    public Boolean getPassed() {
        return passed;
    }

    public void setPassed(Boolean passed) {
        this.passed = passed;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}