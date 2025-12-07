package com.company.apitestingapp.entity;

import io.jmix.core.metamodel.datatype.EnumClass;

import org.springframework.lang.Nullable;


public enum Method implements EnumClass<String> {

    GET("get"),
    POST("post"),
    PUT("put"),
    PATCH("patch"),
    DELETE("delete"),
    HEAD("head"),
    OPTIONS("options");

    private final String id;

    Method(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Nullable
    public static Method fromId(String id) {
        for (Method at : Method.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}