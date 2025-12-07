package com.company.apitestingapp.entity;

import io.jmix.core.metamodel.datatype.EnumClass;

import org.springframework.lang.Nullable;


public enum BodyType implements EnumClass<String> {

    JSON("json"),
    XML("xml"),
    TEXT("text"),
    FORM_DATA("from_data"),
    NONE("none");

    private final String id;

    BodyType(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Nullable
    public static BodyType fromId(String id) {
        for (BodyType at : BodyType.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}