package com.company.apitestingapp.service.apitesting;

import com.company.apitestingapp.entity.BodyType;
import com.company.apitestingapp.entity.Request;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URL;

@Service
public class ValidationService {

    public boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        try {
            new URL(url).toURI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isValidJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return true;
        }
        try {
            new ObjectMapper().readTree(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isValidXml(String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            return true;
        }
        try {
            DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xml)));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getValidationError(Request request) {
        if (!isValidUrl(request.getUrl())) {
            return "Неверный формат URL";
        }

        if (request.getBody() != null && !request.getBody().trim().isEmpty()) {
            if (request.getBodyType() == BodyType.JSON && !isValidJson(request.getBody())) {
                return "Неверный формат JSON";
            }
            if (request.getBodyType() == BodyType.XML && !isValidXml(request.getBody())) {
                return "Неверный формат XML";
            }
        }

        return null;
    }

}
