package run.mone.mcp.miapi.doc.tool;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import run.mone.hive.http.HttpClient;
import run.mone.hive.roles.ReactorRole;
import run.mone.hive.roles.tool.ITool;
import run.mone.mcp.miapi.doc.model.ParserResult;
import run.mone.mcp.miapi.doc.parser.SourceCodeApiParser;
import run.mone.mcp.miapi.doc.util.FileScanner;

import java.util.List;

@Component
public class ApiDocTool implements ITool {

    @Value("${miapi.host}")
    private String host;

    private static HttpClient httpClient = new HttpClient();

    private static final Gson gson = new Gson();

    @Override
    public String getName() {
        return "create_api_doc";
    }

    @Override
    public boolean needExecute() {
        return true;
    }

    @Override
    public boolean show() {
        return true;
    }

    @Override
    public String description() {
        return """
            1.功能只是根据用户提供的java源代码生成接口文档.
            2.用户需要输入源代码地址或路径.
            """;
    }

    @Override
    public String parameters() {
        return """
                - path: (Required) java source code address or path.
                """;
    }

    @Override
    public String usage() {
        return """
            (Attention: If you are using this tool, you must return whether the generation was successful):
            Example:
            ```json
            
            api docs result
            
            ```
            """;
    }

    @Override
    public JsonObject execute(ReactorRole role, JsonObject inputJson) {
        JsonObject result = new JsonObject();
        try {
            String directoryPath = inputJson.has("path") ? inputJson.get("path").getAsString() : null;
            if (!FileScanner.isDirectoryExists(directoryPath)) {
                result.addProperty("message", "代码路径或地址不正确");
            }
            String doc = parseDirectory(directoryPath);
            result.addProperty("apiDocs", doc);
            return result;
        } catch (Exception e) {
            result.addProperty("error", "生成接口信息失败: " + e.getMessage());
            return result;
        }
    }

    private String parseDirectory(String directoryPath) {
        List<String> javaFiles = FileScanner.scanJavaFiles(directoryPath);
        if (javaFiles.isEmpty()) {
            return "";
        }
        // 解析接口文档
        SourceCodeApiParser parser = new SourceCodeApiParser(directoryPath);
        ParserResult result = parser.parseDirectory(directoryPath);

        if (result.isSuccess()) {
            return result.toString();
        } else {
            return "";
        }
    }
}
