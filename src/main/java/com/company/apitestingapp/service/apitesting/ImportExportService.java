package com.company.apitestingapp.service.apitesting;

import com.company.apitestingapp.entity.*;
import com.company.apitestingapp.entity.Collection;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.jmix.core.DataManager;
import io.jmix.core.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.print.Book;
import java.util.*;

@Service
public class ImportExportService {

    private static final Logger log = LoggerFactory.getLogger(ImportExportService.class);

    private final DataManager dataManager;
    private final Metadata metadata;
    private final ObjectMapper objectMapper;

    public ImportExportService(DataManager dataManager, Metadata metadata) {
        this.dataManager = dataManager;
        this.metadata = metadata;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Экспорт коллекции в JSON
     */
    public String exportCollectionToJson(UUID collectionId) {
        try {
            Collection collection = dataManager.load(Collection.class)
                    .id(collectionId)
                    .one();

            if (collection == null) {
                throw new RuntimeException("Коллекция не найдена: " + collectionId);
            }

            List<CollectionFolder> folders = dataManager.load(CollectionFolder.class)
                    .query("select f from CollectionFolder f where f.collection = :collection")
                    .parameter("collection", collection)
                    .list();

            List<Request> requests = dataManager.load(Request.class)
                    .query("select r from Request r where r.collection = :collection")
                    .parameter("collection", collection)
                    .list();

            Map<String, Object> exportData = new LinkedHashMap<>();
            exportData.put("version", "1.0");
            exportData.put("exportDate", new Date().toString());
            exportData.put("collection", buildCollectionData(collection, folders, requests));

            return objectMapper.writeValueAsString(exportData);

        } catch (Exception e) {
            log.error("Ошибка экспорта коллекции", e);
            throw new RuntimeException("Ошибка экспорта коллекции: " + e.getMessage(), e);
        }
    }

    /**
     * Импорт коллекции из JSON
     */
    @Transactional
    public Collection importCollectionFromJson(String json) {
        try {
            Map<String, Object> importData = objectMapper.readValue(json, new TypeReference<>() {
            });

            //проверяем версию
            String version = (String) importData.get("version");
            if (!"1.0".equals(version)) {
                throw new RuntimeException("Неподдерживаемая версия формата: " + version);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> collectionData = (Map<String, Object>) importData.get("collection");

            return importCollectionData(collectionData);

        } catch (Exception e) {
            log.error("Ошибка импорта коллекции", e);
            throw new RuntimeException("Ошибка импорта коллекции: " + e.getMessage(), e);
        }
    }


    /**
     * Импорт всех коллекций из JSON
     */
    public List<Collection> importAllCollectionsFromJson(String jsonContent) {
        System.out.println("=== НАЧАЛО ИМПОРТА В СЕРВИСЕ ===");

        try {
            Map<String, Object> importData = objectMapper.readValue(jsonContent, new TypeReference<>() {});

            List<Collection> importedCollections = new ArrayList<>();

            //обработка разных форматов
            if (importData.containsKey("version")) {
                String version = (String) importData.get("version");
                System.out.println("Обработка версии: " + version);

                if ("1.0".equals(version) && importData.containsKey("collections")) {
                    //обработка структуры с версией 1.0
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> collectionsData = (List<Map<String, Object>>) importData.get("collections");
                    for (Map<String, Object> collectionData : collectionsData) {
                        Collection collection = parseCollection(collectionData);
                        if (collection != null) {
                            importedCollections.add(collection);
                        }
                    }
                } else {
                    throw new RuntimeException("Неподдерживаемая версия формата: " + version);
                }

            } else if (importData.containsKey("collections")) {
                System.out.println("Обработка структуры БЕЗ версии с полем 'collections'");
                //обработка формата без версии, но с коллекциями
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> collectionsData = (List<Map<String, Object>>) importData.get("collections");
                for (Map<String, Object> collectionData : collectionsData) {
                    Collection collection = parseCollection(collectionData);
                    if (collection != null) {
                        importedCollections.add(collection);
                    }
                }

            } else {
                throw new RuntimeException("Неподдерживаемая структура JSON. Ожидается поле 'collections'");
            }

            System.out.println("Успешно импортировано коллекций: " + importedCollections.size());
            return importedCollections;

        } catch (Exception e) {
            System.out.println("Ошибка импорта: " + e.getMessage());
            throw new RuntimeException("Ошибка импорта всех коллекций: " + e.getMessage(), e);
        }
    }

    private Collection parseCollection(Map<String, Object> collectionData) {
        try {
            Collection collection = metadata.create(Collection.class);

            if (collectionData.containsKey("name")) {
                collection.setName((String) collectionData.get("name"));
                System.out.println("Создана коллекция: " + collection.getName());
            } else {
                System.out.println("Предупреждение: у коллекции нет имени");
                collection.setName("Без имени");
            }

            if (collectionData.containsKey("description")) {
                collection.setDescription((String) collectionData.get("description"));
            }

            //сохраняем коллекцию
            dataManager.save(collection);

            return collection;

        } catch (Exception e) {
            System.out.println("Ошибка парсинга коллекции: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Вспомогательные методы

    private Map<String, Object> buildCollectionData(Collection collection, List<CollectionFolder> folders, List<Request> requests) {
        Map<String, Object> collectionData = new LinkedHashMap<>();
        collectionData.put("name", collection.getName());
        collectionData.put("description", collection.getDescription());

        // Папки
        List<Map<String, Object>> foldersData = new ArrayList<>();
        for (CollectionFolder folder : folders) {
            if (folder.getParentFolder() == null) { // Только корневые папки
                foldersData.add(buildFolderData(folder, folders, requests));
            }
        }
        collectionData.put("folders", foldersData);

        // Запросы без папки
        List<Map<String, Object>> requestsData = new ArrayList<>();
        for (Request request : requests) {
            if (request.getFolder() == null) {
                requestsData.add(buildRequestData(request));
            }
        }
        collectionData.put("requests", requestsData);

        return collectionData;
    }

    private Map<String, Object> buildFolderData(CollectionFolder folder, List<CollectionFolder> allFolders, List<Request> allRequests) {
        Map<String, Object> folderData = new LinkedHashMap<>();
        folderData.put("name", folder.getName());
        folderData.put("description", folder.getDescription());

        // Подпапки
        List<Map<String, Object>> subFoldersData = new ArrayList<>();
        for (CollectionFolder subFolder : allFolders) {
            if (subFolder.getParentFolder() != null && subFolder.getParentFolder().getId().equals(folder.getId())) {
                subFoldersData.add(buildFolderData(subFolder, allFolders, allRequests));
            }
        }
        folderData.put("subFolders", subFoldersData);

        // Запросы в папке
        List<Map<String, Object>> requestsData = new ArrayList<>();
        for (Request request : allRequests) {
            if (request.getFolder() != null && request.getFolder().getId().equals(folder.getId())) {
                requestsData.add(buildRequestData(request));
            }
        }
        folderData.put("requests", requestsData);

        return folderData;
    }

    private Map<String, Object> buildRequestData(Request request) {
        Map<String, Object> requestData = new LinkedHashMap<>();
        requestData.put("name", request.getName());
        requestData.put("description", request.getDescription());
        requestData.put("url", request.getUrl());
        requestData.put("method", request.getMethod());
        requestData.put("bodyType", request.getBodyType());
        requestData.put("body", request.getBody());

        // Заголовки
        List<Map<String, String>> headersData = new ArrayList<>();
        if (request.getHeaders() != null) {
            for (Header header : request.getHeaders()) {
                Map<String, String> headerData = new LinkedHashMap<>();
                headerData.put("key", header.getKey());
                headerData.put("value", header.getValue());
                headersData.add(headerData);
            }
        }
        requestData.put("headers", headersData);

        // Параметры запроса
        List<Map<String, String>> queryParamsData = new ArrayList<>();
        if (request.getQueryParams() != null) {
            for (QueryParameter param : request.getQueryParams()) {
                Map<String, String> paramData = new LinkedHashMap<>();
                paramData.put("key", param.getKey());
                paramData.put("value", param.getValue());
                queryParamsData.add(paramData);
            }
        }
        requestData.put("queryParams", queryParamsData);

        return requestData;
    }

    private Collection importCollectionData(Map<String, Object> collectionData) {
        // Создаем коллекцию
        Collection collection = metadata.create(Collection.class);
        collection.setName((String) collectionData.get("name"));
        collection.setDescription((String) collectionData.get("description"));

        Collection savedCollection = dataManager.save(collection);

        // Импортируем папки
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> foldersData = (List<Map<String, Object>>) collectionData.get("folders");
        if (foldersData != null) {
            for (Map<String, Object> folderData : foldersData) {
                importFolderData(savedCollection, null, folderData);
            }
        }

        // Импортируем запросы без папки
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> requestsData = (List<Map<String, Object>>) collectionData.get("requests");
        if (requestsData != null) {
            for (Map<String, Object> requestData : requestsData) {
                importRequestData(savedCollection, null, requestData);
            }
        }

        return savedCollection;
    }

    private void importFolderData(Collection collection, CollectionFolder parentFolder, Map<String, Object> folderData) {
        CollectionFolder folder = metadata.create(CollectionFolder.class);
        folder.setName((String) folderData.get("name"));
        folder.setDescription((String) folderData.get("description"));
        folder.setCollection(collection);
        folder.setParentFolder(parentFolder);

        CollectionFolder savedFolder = dataManager.save(folder);

        // Импортируем подпапки
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> subFoldersData = (List<Map<String, Object>>) folderData.get("subFolders");
        if (subFoldersData != null) {
            for (Map<String, Object> subFolderData : subFoldersData) {
                importFolderData(collection, savedFolder, subFolderData);
            }
        }

        // Импортируем запросы в папке
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> requestsData = (List<Map<String, Object>>) folderData.get("requests");
        if (requestsData != null) {
            for (Map<String, Object> requestData : requestsData) {
                importRequestData(collection, savedFolder, requestData);
            }
        }
    }

    private void importRequestData(Collection collection, CollectionFolder folder, Map<String, Object> requestData) {
        Request request = metadata.create(Request.class);
        request.setName((String) requestData.get("name"));
        request.setDescription((String) requestData.get("description"));
        request.setUrl((String) requestData.get("url"));
        String methodStr = (String) requestData.get("method");
        request.setMethod(methodStr != null ? Method.valueOf(methodStr) : null);
        String bodyTypeStr = (String) requestData.get("bodyType");
        request.setBodyType(bodyTypeStr != null ? BodyType.valueOf(bodyTypeStr) : null);
        request.setBody((String) requestData.get("body"));
        request.setCollection(collection);
        request.setFolder(folder);

        Request savedRequest = dataManager.save(request);

        // Импортируем заголовки
        @SuppressWarnings("unchecked")
        List<Map<String, String>> headersData = (List<Map<String, String>>) requestData.get("headers");
        if (headersData != null) {
            for (Map<String, String> headerData : headersData) {
                Header header = metadata.create(Header.class);
                header.setKey(headerData.get("key"));
                header.setValue(headerData.get("value"));
                header.setRequest(savedRequest);
                dataManager.save(header);
            }
        }

        // Импортируем параметры запроса
        @SuppressWarnings("unchecked")
        List<Map<String, String>> queryParamsData = (List<Map<String, String>>) requestData.get("queryParams");
        if (queryParamsData != null) {
            for (Map<String, String> paramData : queryParamsData) {
                QueryParameter param = metadata.create(QueryParameter.class);
                param.setKey(paramData.get("key"));
                param.setValue(paramData.get("value"));
                param.setRequest(savedRequest);
                dataManager.save(param);
            }
        }
    }

    /**
     * Проверка валидности JSON перед импортом
     */
    public boolean validateImportJson(String json) {
        try {
            Map<String, Object> importData = objectMapper.readValue(json, new TypeReference<>() {});

            boolean hasCollections = importData.containsKey("collections");
            boolean isArray = json.trim().startsWith("[");

            if (importData.containsKey("version")) {
                String version = (String) importData.get("version");
                return "1.0".equals(version);
            } else if (hasCollections || isArray) {
                System.out.println("Валидация: принят JSON без версии, но с коллекциями или как массив");
                return true;
            }

            return false;
        } catch (Exception e) {
            System.out.println("Валидация не пройдена: " + e.getMessage());
            return false;
        }
    }
}