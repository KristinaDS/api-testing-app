package com.company.apitestingapp.entity;

import io.jmix.core.DeletePolicy;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.entity.annotation.OnDelete;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.*;

@JmixEntity
@Table(name = "REQUEST")
@Entity
public class Request {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    private UUID id;

    @InstanceName
    @Column(name = "NAME")
    private String name;

    @Column(name = "DESCRIPTION")
    @Lob
    private String description;

    @Column(name = "SORT_ORDER")
    private Integer sortOrder;

    @Column(name = "URL")
    private String url;

    @OneToMany(mappedBy = "request")
    @OnDelete(DeletePolicy.CASCADE)
    private List<Header> headers;

    @OneToMany(mappedBy = "request")
    @OnDelete(DeletePolicy.CASCADE)
    private List<QueryParameter> queryParams;

    @Column(name = "BODY")
    @Lob
    private String body;

    @Column(name = "METHOD", nullable = false)
    @NotNull
    private Method method;

    @Column(name = "BODY_TYPE", nullable = false)
    @NotNull
    private BodyType bodyType;

    @JoinColumn(name = "COLLECTION_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private Collection collection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FOLDER_ID")
    private CollectionFolder folder;


    public List<QueryParameter> getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(List<QueryParameter> queryParams){
        this.queryParams = queryParams;
    }

    public void setHeaders(List<Header> headers) {
        this.headers = headers;
    }

    public List<Header> getHeaders() {
        return headers;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method){
        this.method = method;
    }

    public BodyType getBodyType() {
        return bodyType;
    }

    public void setBodyType(BodyType bodyType){
        this.bodyType = bodyType;
    }

    public Collection getCollection() {
        return collection;
    }

    public void setCollection(Collection collection) {
        this.collection = collection;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer orderIndex) {
        this.sortOrder = orderIndex;
    }

    public CollectionFolder getFolder() {
        return folder;
    }

    public void setFolder(CollectionFolder folder) {
        this.folder = folder;
    }

}