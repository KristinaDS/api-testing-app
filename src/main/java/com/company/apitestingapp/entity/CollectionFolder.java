package com.company.apitestingapp.entity;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.Composition;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@JmixEntity
@Table(name = "COLLECTION_FOLDER")
@Entity
public class CollectionFolder {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    private UUID id;

    @InstanceName
    @Column(name = "NAME")
    private String name;

    @Column(name = "DESCRIPTION")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COLLECTION_ID")
    private Collection collection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_FOLDER_ID")
    private CollectionFolder parentFolder;

    @OneToMany(mappedBy = "parentFolder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CollectionFolder> subFolders;

    @OneToMany(mappedBy = "folder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Request> requests;

    @Column(name = "SORT_ORDER")
    private Integer sortOrder;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Collection getCollection() {
        return collection;
    }

    public void setCollection(Collection collection) {
        this.collection = collection;
    }

    public CollectionFolder getParentFolder() {
        return parentFolder;
    }

    public void setParentFolder(CollectionFolder parentFolder) {
        this.parentFolder = parentFolder;
    }

    public List<CollectionFolder> getSubFolders() {
        return subFolders;
    }

    public void setSubFolders(List<CollectionFolder> subFolders) {
        this.subFolders = subFolders;
    }

    public List<Request> getRequests() {
        return requests;
    }

    public void setRequests(List<Request> requests) {
        this.requests = requests;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    // Вспомогательные методы
    public String getFullPath() {
        if (parentFolder != null) {
            return parentFolder.getFullPath() + " / " + name;
        }
        return name;
    }

    public int getTotalRequestsCount() {
        int count = requests != null ? requests.size() : 0;
        if (subFolders != null) {
            for (CollectionFolder subFolder : subFolders) {
                count += subFolder.getTotalRequestsCount();
            }
        }
        return count;
    }

    public int getTotalSubfoldersCount() {
        int count = subFolders != null ? subFolders.size() : 0;
        if (subFolders != null) {
            for (CollectionFolder subFolder : subFolders) {
                count += subFolder.getTotalSubfoldersCount();
            }
        }
        return count;
    }

    public boolean isRootFolder() {
        return parentFolder == null;
    }

    public boolean hasSubfolders() {
        return subFolders != null && !subFolders.isEmpty();
    }

    public boolean hasRequests() {
        return requests != null && !requests.isEmpty();
    }
}