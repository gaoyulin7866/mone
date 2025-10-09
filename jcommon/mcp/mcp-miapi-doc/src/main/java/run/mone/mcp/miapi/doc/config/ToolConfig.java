package run.mone.mcp.miapi.doc.config;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import run.mone.hive.mcp.function.ChatFunction;
import run.mone.hive.mcp.service.RoleMeta;
import run.mone.hive.roles.tool.*;
import run.mone.mcp.miapi.doc.tool.ApiDocTool;

@Configuration
public class ToolConfig {

    @Value("${mcp.agent.name}")
    private String agentName;

    @Autowired
    private ApiDocTool apiDocTool;

    @Bean
    public RoleMeta roleMeta() {
        return RoleMeta.builder()
                .outputFormat("json")
                .profile("你是一名优秀的私人助理")
                .goal("你的目标是帮助用户根据java源代码生成API接口文档")
                .constraints("不要探讨一些负面的东西,如果用户问你,你可以直接拒绝掉")
                //内部工具
                .tools(Lists.newArrayList(
                        apiDocTool,
                        new ChatTool(),
                        new AttemptCompletionTool(),
                        new AskTool(),
                        new SpeechToTextTool(),
                        new TextToSpeechTool()))
                //mcp工具
                .mcpTools(Lists.newArrayList(new ChatFunction(agentName, 30000)))
                .build();
    }
}
