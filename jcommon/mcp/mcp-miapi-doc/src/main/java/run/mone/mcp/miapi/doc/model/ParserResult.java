package run.mone.mcp.miapi.doc.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 解析结果
 */
public class ParserResult {
    
    /**
     * 解析是否成功
     */
    @JsonProperty("success")
    private boolean success;
    
    /**
     * 错误信息
     */
    @JsonProperty("errorMessage")
    private String errorMessage;
    
    /**
     * 按Controller权限定名分组的接口信息
     * Key: Controller权限定名
     * Value: 该Controller下的接口信息列表
     */
    @JsonProperty("apiGroups")
    private Map<String, List<ApiInfo>> apiGroups = new HashMap<>();
    
    /**
     * 按接口地址分组的接口信息
     * Key: 接口地址
     * Value: 该地址下的接口信息列表（用于处理相同地址的情况）
     */
//    @JsonProperty("apiByPath")
//    private Map<String, List<ApiInfo>> apiByPath = new HashMap<>();
//
//    /**
//     * 所有接口信息
//     */
//    @JsonProperty("allApis")
//    private List<ApiInfo> allApis = new ArrayList<>();
    
    /**
     * 解析的文件数量
     */
    @JsonProperty("parsedFileCount")
    private int parsedFileCount;
    
    /**
     * 解析的接口数量
     */
    @JsonProperty("parsedApiCount")
    private int parsedApiCount;

    public ParserResult() {
    }

    public ParserResult(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Map<String, List<ApiInfo>> getApiGroups() {
        return apiGroups;
    }

    public void setApiGroups(Map<String, List<ApiInfo>> apiGroups) {
        this.apiGroups = apiGroups;
    }

//    public Map<String, List<ApiInfo>> getApiByPath() {
//        return apiByPath;
//    }
//
//    public void setApiByPath(Map<String, List<ApiInfo>> apiByPath) {
//        this.apiByPath = apiByPath;
//    }
//
//    public List<ApiInfo> getAllApis() {
//        return allApis;
//    }
//
//    public void setAllApis(List<ApiInfo> allApis) {
//        this.allApis = allApis;
//    }

    public int getParsedFileCount() {
        return parsedFileCount;
    }

    public void setParsedFileCount(int parsedFileCount) {
        this.parsedFileCount = parsedFileCount;
    }

    public int getParsedApiCount() {
        return parsedApiCount;
    }

    public void setParsedApiCount(int parsedApiCount) {
        this.parsedApiCount = parsedApiCount;
    }

    /**
     * 添加接口信息到指定Controller组
     */
    public void addApiToGroup(String controllerQualifiedName, ApiInfo apiInfo) {
        apiGroups.computeIfAbsent(controllerQualifiedName, k -> new ArrayList<>()).add(apiInfo);
    }

    /**
     * 添加接口信息到指定路径组
     */
//    public void addApiToPath(String path, ApiInfo apiInfo) {
//        apiByPath.computeIfAbsent(path, k -> new ArrayList<>()).add(apiInfo);
//    }
//
//    /**
//     * 添加接口信息到所有接口列表
//     */
//    public void addApi(ApiInfo apiInfo) {
//        allApis.add(apiInfo);
//    }

    @Override
    public String toString() {
        return "ParserResult{" +
                "success=" + success +
                ", errorMessage='" + errorMessage + '\'' +
                ", apiGroups=" + apiGroups +
                ", parsedFileCount=" + parsedFileCount +
                ", parsedApiCount=" + parsedApiCount +
                '}';
    }
}
