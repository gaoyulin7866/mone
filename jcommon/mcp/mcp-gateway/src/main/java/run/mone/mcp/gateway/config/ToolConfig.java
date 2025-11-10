package run.mone.mcp.gateway.config;


import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import run.mone.hive.mcp.function.ChatFunction;
import run.mone.hive.mcp.service.RoleMeta;
import run.mone.hive.roles.tool.*;
import run.mone.mcp.gateway.tool.GatewayApiTool;

@Configuration
public class ToolConfig {

    @Autowired
    private GatewayApiTool gatewayApiTool;
    @Value("${mcp.agent.name}")
    private String agentName;

    @Bean
    public RoleMeta roleMeta() {
        return RoleMeta.builder()
                .outputFormat("json")
                .profile("你是一名优秀的私人助理")
                .goal("你的目标是更好的帮助用户根据需求选择合适工具完成任务")
                .constraints("不要探讨一些负面的东西,如果用户问你,你可以直接拒绝掉")
                //内部工具
                .tools(Lists.newArrayList(
                        gatewayApiTool,
                        new ExecuteCommandTool(),
                        new ChatTool(),
                        new AttemptCompletionTool(),
                        new AskTool(),
                        new SpeechToTextTool(),
                        new TextToSpeechTool()))
                //mcp工具
                .mcpTools(Lists.newArrayList(new ChatFunction(agentName, 10 * 60 * 1000)))
                .build();
    }
}
