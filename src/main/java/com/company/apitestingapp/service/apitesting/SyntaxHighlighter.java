package com.company.apitestingapp.service.apitesting;

import org.springframework.stereotype.Service;

@Service
public class SyntaxHighlighter {

    public String highlightJson(String json) {
        try {
            // Простая подсветка JSON с цветами
            String highlighted = json
                    .replace("\\\"", "\\\\\"") // Экранируем кавычки
                    .replace("\"", "<span style='color: #d14;'>\"</span>")
                    .replace(":", "<span style='color: #905;'>:</span>")
                    .replace("{", "<span style='color: #905;'>{</span>")
                    .replace("}", "<span style='color: #905;'>}</span>")
                    .replace("[", "<span style='color: #905;'>[</span>")
                    .replace("]", "<span style='color: #905;'>]</span>")
                    .replace(",", "<span style='color: #905;'>,</span>")
                    .replace("\n", "<br>")
                    .replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;")
                    .replace("  ", "&nbsp;&nbsp;");

            // Подсветка ключей
            highlighted = highlighted.replaceAll(
                    "<span style='color: #d14;'>\"</span>(.*?)<span style='color: #d14;'>\"</span><span style='color: #905;'>:</span>",
                    "<span style='color: #d14;'>\"</span><span style='color: #397300;'>$1</span><span style='color: #d14;'>\"</span><span style='color: #905;'>:</span>"
            );

            return "<pre style='background: #f6f8fa; padding: 16px; border-radius: 6px; font-family: monospace; font-size: 14px; line-height: 1.45; overflow: auto;'>" +
                    highlighted + "</pre>";
        } catch (Exception e) {
            return "<pre style='background: #f6f8fa; padding: 16px; border-radius: 6px;'>" +
                    json.replace("\n", "<br>").replace("  ", "&nbsp;&nbsp;") + "</pre>";
        }
    }

    public String highlightXml(String xml) {
        try {
            String highlighted = xml
                    .replace("&", "&amp;")
                    .replace("<", "<span style='color: #905;'>&lt;</span>")
                    .replace(">", "<span style='color: #905;'>&gt;</span>")
                    .replace("\"", "<span style='color: #d14;'>\"</span>")
                    .replace("=", "<span style='color: #905;'>=</span>")
                    .replace("\n", "<br>")
                    .replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;")
                    .replace("  ", "&nbsp;&nbsp;");

            // Подсветка тегов
            highlighted = highlighted.replaceAll(
                    "<span style='color: #905;'>&lt;</span>(/?)([^<span>]+?)<span style='color: #905;'>&gt;</span>",
                    "<span style='color: #905;'>&lt;</span><span style='color: #1f6ed6;'>$1$2</span><span style='color: #905;'>&gt;</span>"
            );

            // Подсветка атрибутов
            highlighted = highlighted.replaceAll(
                    "\\s([^=]+?)=<span style='color: #905;'>=</span>",
                    " <span style='color: #397300;'>$1</span><span style='color: #905;'>=</span>"
            );

            return "<pre style='background: #f6f8fa; padding: 16px; border-radius: 6px; font-family: monospace; font-size: 14px; line-height: 1.45; overflow: auto;'>" +
                    highlighted + "</pre>";
        } catch (Exception e) {
            return "<pre style='background: #f6f8fa; padding: 16px; border-radius: 6px;'>" +
                    xml.replace("\n", "<br>").replace("  ", "&nbsp;&nbsp;") + "</pre>";
        }
    }
}
