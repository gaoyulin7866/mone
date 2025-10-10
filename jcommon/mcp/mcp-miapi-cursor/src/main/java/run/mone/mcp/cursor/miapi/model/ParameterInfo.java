package run.mone.mcp.cursor.miapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 参数信息
 */
public class ParameterInfo {
    
    /**
     * 参数名称
     */
    @JsonProperty("name")
    private String name;
    
    /**
     * 参数类型
     */
    @JsonProperty("type")
    private String type;
    
    /**
     * 参数描述
     */
    @JsonProperty("description")
    private String description;
    
    /**
     * 是否必填
     */
    @JsonProperty("required")
    private boolean required;
    
    /**
     * 参数位置（query, path, body, header等）
     */
    @JsonProperty("position")
    private String position;
    
    /**
     * 默认值
     */
    @JsonProperty("defaultValue")
    private String defaultValue;
    
    /**
     * 泛型信息
     */
    @JsonProperty("genericType")
    private String genericType;

    public ParameterInfo() {
    }

    public ParameterInfo(String name, String type, String description, boolean required, String position) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.required = required;
        this.position = position;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getGenericType() {
        return genericType;
    }

    public void setGenericType(String genericType) {
        this.genericType = genericType;
    }

    @Override
    public String toString() {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"name\": \"").append(escapeJson(name)).append("\",\n");
        json.append("  \"type\": \"").append(escapeJson(type)).append("\",\n");
        json.append("  \"description\": \"").append(escapeJson(description)).append("\",\n");
        json.append("  \"required\": ").append(required).append(",\n");
        json.append("  \"position\": \"").append(escapeJson(position)).append("\",\n");
        json.append("  \"defaultValue\": \"").append(escapeJson(defaultValue)).append("\",\n");
        json.append("  \"genericType\": \"").append(escapeJson(genericType)).append("\"\n");
        json.append("}");
        return json.toString();
    }

    // 辅助方法：转义 JSON 字符串
    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
