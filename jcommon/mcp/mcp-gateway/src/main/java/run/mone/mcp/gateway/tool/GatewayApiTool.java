package run.mone.mcp.gateway.tool;


import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import run.mone.hive.roles.ReactorRole;
import run.mone.hive.roles.tool.ITool;
import run.mone.mcp.gateway.service.GatewayService;
import run.mone.mcp.gateway.service.bo.ListApiInfoParam;

@Component
@Slf4j
public class GatewayApiTool implements ITool {

    @Autowired
    private GatewayService gatewayService;

    @Override
    public String getName() {
        return "gateway_api_tool";
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
            1.功能仅支持根据用户输入的网关接口地址查询网关接口详情和根据用户输入的网关接口关键字查询网关接口列表。
            2.除以上两种情况外，一律不要调用此工具。
            """;
    }

    @Override
    public String parameters() {
        return """
                - operation: (Required) The api operation to perform(Optional value: listApiInfo or detailByUrl).
                - env: (Required) The api environment(Optional value: staging or online, default: staging).
                - keyword: (Optional) fuzzy search keyword.
                - url: (Optional) api url.
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
        String operation = getParams(inputJson, "operation");
        String env = getParams(inputJson, "env");
        if (operation.isEmpty() || env.isEmpty()) {
            result.addProperty("message", "无法推断出执行操作类型或执行环境");
            return result;
        }
        try {
            switch (operation){
                case "listApiInfo"-> {
                    ListApiInfoParam param = new ListApiInfoParam();
                    String keyword = getParams(inputJson, "keyword");
                    if (keyword.isEmpty()) {
                        result.addProperty("message", "请输入要查询网关接口的关键字");
                    } else {
                        String r = gatewayService.listApiInfo(env, param);
                        result.addProperty("data", r);
                        result.addProperty("message", "查询成功");
                    }
                }
                case "detailByUrl" -> {
                    String url = getParams(inputJson, "url");
                    if (url.isEmpty()) {
                        result.addProperty("message", "请输入要查询网关接口的url");
                    } else {
                        String r = gatewayService.detailByUrl(env, url);
                        result.addProperty("data", r);
                        result.addProperty("message", "查询成功");
                    }
                }
                default -> result.addProperty("message", "此工具无法执行该操作");
            }
        }catch (Exception e) {
            result.addProperty("message", "工具执行失败：" + e.getMessage());
        }
        return result;
    }

    private String getParams(JsonObject inputJson, String name) {
        return inputJson.has(name) ? inputJson.get(name).getAsString() : "";
    }
}
