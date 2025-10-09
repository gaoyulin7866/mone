package run.mone.mcp.miapi.doc.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * 接口信息
 */
public class ApiInfo {
    
    /**
     * 接口名称（方法名）
     */
    @JsonProperty("name")
    private String name;
    
    /**
     * 接口地址
     */
    @JsonProperty("path")
    private String path;
    
    /**
     * 请求方式（GET, POST, PUT, DELETE等）
     */
    @JsonProperty("method")
    private String method;
    
    /**
     * 接口描述
     */
    @JsonProperty("description")
    private String description;
    
    /**
     * 入参列表
     */
    @JsonProperty("inputParameters")
    private List<ParameterInfo> inputParameters = new ArrayList<>();
    
    /**
     * 出参类型
     */
    @JsonProperty("returnType")
    private String returnType;
    
    /**
     * 出参描述
     */
    @JsonProperty("returnDescription")
    private String returnDescription;
    
    /**
     * 返回类型字段列表（当返回类型是自定义类时）
     */
    @JsonProperty("returnFields")
    private List<ParameterInfo> returnFields = new ArrayList<>();
    
    /**
     * 方法签名
     */
    @JsonProperty("methodSignature")
    private String methodSignature;
    
    /**
     * 所属Controller类名
     */
    @JsonProperty("controllerClass")
    private String controllerClass;
    
    /**
     * Controller权限定名
     */
    @JsonProperty("controllerQualifiedName")
    private String controllerQualifiedName;
    
    /**
     * 接口分组
     */
    @JsonProperty("group")
    private String group;
    
    /**
     * 接口标签
     */
    @JsonProperty("tags")
    private List<String> tags = new ArrayList<>();
    
    /**
     * 是否过时
     */
    @JsonProperty("deprecated")
    private boolean deprecated;

    public ApiInfo() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ParameterInfo> getInputParameters() {
        return inputParameters;
    }

    public void setInputParameters(List<ParameterInfo> inputParameters) {
        this.inputParameters = inputParameters;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public String getReturnDescription() {
        return returnDescription;
    }

    public void setReturnDescription(String returnDescription) {
        this.returnDescription = returnDescription;
    }

    public List<ParameterInfo> getReturnFields() {
        return returnFields;
    }

    public void setReturnFields(List<ParameterInfo> returnFields) {
        this.returnFields = returnFields;
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    public void setMethodSignature(String methodSignature) {
        this.methodSignature = methodSignature;
    }

    public String getControllerClass() {
        return controllerClass;
    }

    public void setControllerClass(String controllerClass) {
        this.controllerClass = controllerClass;
    }

    public String getControllerQualifiedName() {
        return controllerQualifiedName;
    }

    public void setControllerQualifiedName(String controllerQualifiedName) {
        this.controllerQualifiedName = controllerQualifiedName;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    @Override
    public String toString() {
        return "ApiInfo{" +
                "name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", method='" + method + '\'' +
                ", description='" + description + '\'' +
                ", inputParameters=" + inputParameters +
                ", returnType='" + returnType + '\'' +
                ", returnDescription='" + returnDescription + '\'' +
                ", methodSignature='" + methodSignature + '\'' +
                ", controllerClass='" + controllerClass + '\'' +
                ", controllerQualifiedName='" + controllerQualifiedName + '\'' +
                ", group='" + group + '\'' +
                ", tags=" + tags +
                ", deprecated=" + deprecated +
                '}';
    }
}
