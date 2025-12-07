package com.company.apitestingapp.service.apitesting;

import com.company.apitestingapp.entity.RequestExecution;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.StreamResource;
import io.jmix.core.DataManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class HtmlReportService {

    @Autowired
    private DataManager dataManager;

    //генерация фортама HTML для отчетов
    public String generateTestReport(List<RequestExecution> executions) {
        String generatedDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        long totalTests = executions.size();
        long successCount = executions.stream().filter(e -> Boolean.TRUE.equals(e.getIsSuccess())).count();
        long failedCount = totalTests - successCount;

        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<title>API Test Report</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; }");
        html.append(".test-result { border: 1px solid #ddd; margin: 10px 0; padding: 15px; border-radius: 5px; }");
        html.append(".success { background-color: #d4edda; border-color: #c3e6cb; }");
        html.append(".error { background-color: #f8d7da; border-color: #f5c6cb; }");
        html.append(".method { display: inline-block; padding: 3px 8px; border-radius: 3px; color: white; font-weight: bold; }");
        html.append(".get { background-color: #28a745; }");
        html.append(".post { background-color: #007bff; }");
        html.append(".put { background-color: #ffc107; color: black; }");
        html.append(".delete { background-color: #dc3545; }");
        html.append(".patch { background-color: #fd7e14; }");
        html.append(".head { background-color: #6f42c1; }");
        html.append(".options { background-color: #e83e8c; }");
        html.append(".unknown { background-color: #6c757d; }");
        html.append(".status { font-weight: bold; }");
        html.append(".time { color: #6c757d; }");
        html.append("table { width: 100%; border-collapse: collapse; }");
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
        html.append("th { background-color: #f2f2f2; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("<h1>API Test Report</h1>");
        html.append("<p>Generated on: ").append(generatedDate).append("</p>");
        html.append("<p>Total tests: ").append(totalTests).append("</p>");
        html.append("<p>Success: ").append(successCount).append(", Failed: ").append(failedCount).append("</p>");

        //сводная таблица
        html.append("<h2>Summary</h2>");
        html.append("<table>");
        html.append("<tr>");
        html.append("<th>Method</th>");
        html.append("<th>URL</th>");
        html.append("<th>Status</th>");
        html.append("<th>Time (ms)</th>");
        html.append("<th>Size (bytes)</th>");
        html.append("</tr>");

        for (RequestExecution execution : executions) {
            String methodClass = execution.getRequest().getMethod() != null ?
                    execution.getRequest().getMethod().name().toLowerCase() : "get";
            String statusClass = Boolean.TRUE.equals(execution.getIsSuccess()) ? "success" : "error";
            String methodName = execution.getRequest().getMethod() != null ?
                    execution.getRequest().getMethod().name() : "N/A";
            String url = execution.getRequest().getUrl() != null ? execution.getRequest().getUrl() : "N/A";
            Integer statusCode = execution.getStatusCode() != null ? execution.getStatusCode() : 500;
            Long responseTime = execution.getResponseTimeMs() != null ? execution.getResponseTimeMs() : 0L;
            Long sizeBytes = execution.getSizeBytes() != null ? execution.getSizeBytes() : 0L;

            html.append("<tr class=\"").append(statusClass).append("\">");
            html.append("<td><span class=\"method ").append(methodClass).append("\">").append(methodName).append("</span></td>");
            html.append("<td>").append(url).append("</td>");
            html.append("<td class=\"status\">").append(statusCode).append("</td>");
            html.append("<td class=\"time\">").append(responseTime).append(" ms</td>");
            html.append("<td>").append(sizeBytes).append("</td>");
            html.append("</tr>");
        }

        html.append("</table>");

        //детали по каждому тесту
        html.append("<h2>Detailed Results</h2>");

        for (RequestExecution execution : executions) {
            String statusClass = Boolean.TRUE.equals(execution.getIsSuccess()) ? "success" : "error";
            String methodName = execution.getRequest().getMethod() != null ?
                    execution.getRequest().getMethod().name() : "N/A";
            String url = execution.getRequest().getUrl() != null ? execution.getRequest().getUrl() : "N/A";
            Integer statusCode = execution.getStatusCode() != null ? execution.getStatusCode() : 0;
            Long responseTime = execution.getResponseTimeMs() != null ? execution.getResponseTimeMs() : 0L;
            Long sizeBytes = execution.getSizeBytes() != null ? execution.getSizeBytes() : 0L;
            String responseBody = execution.getResponseBody() != null ? execution.getResponseBody() :
                    (execution.getErrorMessage() != null ? execution.getErrorMessage() : "No response");

            html.append("<div class=\"test-result ").append(statusClass).append("\">");
            html.append("<h3>").append(methodName).append(" ").append(url).append("</h3>");
            html.append("<p><strong>Status:</strong> ").append(statusCode).append("</p>");
            html.append("<p><strong>Time:</strong> ").append(responseTime).append(" ms</p>");
            html.append("<p><strong>Size:</strong> ").append(sizeBytes).append(" bytes</p>");
            html.append("<p><strong>Response:</strong></p>");
            html.append("<pre>").append(escapeHtml(responseBody)).append("</pre>");
            html.append("</div>");
        }

        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    public void exportReportToFile(List<RequestExecution> executions, String filename) {
        try {
            String htmlContent = generateTestReport(executions);

            //создаем StreamResource для скачивания
            StreamResource resource = new StreamResource(
                    filename,
                    () -> new ByteArrayInputStream(htmlContent.getBytes(StandardCharsets.UTF_8))
            );

            //триггерим скачивание через JavaScript
            UI.getCurrent().getElement().executeJs(
                    "const link = document.createElement('a'); " +
                            "const url = URL.createObjectURL(new Blob([$0], {type: 'text/html'})); " +
                            "link.href = url; " +
                            "link.download = $1; " +
                            "document.body.appendChild(link); " +
                            "link.click(); " +
                            "document.body.removeChild(link); " +
                            "URL.revokeObjectURL(url);",
                    htmlContent, filename
            );

        } catch (Exception e) {
            throw new RuntimeException("Error exporting HTML report", e);
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

}
