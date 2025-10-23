package run.mone.mcp.cursor.miapi.parser;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassFieldExtractor {
    private static final Logger logger = LoggerFactory.getLogger(ClassFieldExtractor.class);
    public static class FieldInfo {
        private String fieldName;
        private String fieldType;
        private List<String> genericTypes = new ArrayList<>();
        private Type classType;
        private String comment;

        // getters and setters
        public String getFieldName() { return fieldName; }
        public void setFieldName(String fieldName) { this.fieldName = fieldName; }

        public String getFieldType() { return fieldType; }
        public void setFieldType(String fieldType) { this.fieldType = fieldType; }

        public List<String> getGenericTypes() { return genericTypes; }
        public void setGenericTypes(List<String> genericTypes) { this.genericTypes = genericTypes; }

        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }

        public Type getClassType() {
            return classType;
        }

        public void setClassType(Type classType) {
            this.classType = classType;
        }

        @Override
        public String toString() {
            return String.format("Field{name='%s', type='%s', generics=%s}",
                    fieldName, fieldType, genericTypes);
        }
    }

    /**
     * 根据类名扫描项目目录查找类文件
     */
    public static List<FieldInfo> findClassAndExtractFields(String className, String projectRoot, List<Node> nodes) throws IOException {
        List<File> classFiles = findClassFiles(className, projectRoot);
        List<FieldInfo> allFields = new ArrayList<>();

        for (File file : classFiles) {
            allFields.addAll(parseJavaFile(file, nodes));
        }

        return allFields;
    }

    /**
     * 在项目目录中递归查找类文件
     */
    private static List<File> findClassFiles(String className, String projectRoot) throws IOException {
        List<File> result = new ArrayList<>();
        Path startPath = Paths.get(projectRoot);

        Files.walk(startPath)
                .filter(path -> path.toString().endsWith(".java"))
                .forEach(path -> {
                    File file = path.toFile();
                    if (isTargetClass(file, className)) {
                        result.add(file);
                    }
                });

        return result;
    }

    /**
     * 检查文件是否包含目标类
     */
    private static boolean isTargetClass(File javaFile, String className) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(javaFile);
            return cu.findAll(ClassOrInterfaceDeclaration.class).stream()
                    .anyMatch(cls -> cls.getNameAsString().equals(className));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 解析Java文件提取字段信息
     */
    private static List<FieldInfo> parseJavaFile(File javaFile, List<Node> nodes) throws IOException {
        List<FieldInfo> fieldInfos = new ArrayList<>();
        CompilationUnit cu = StaticJavaParser.parse(javaFile);

        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
            Map<String, Type> typeMap = new HashMap<>();
            NodeList<TypeParameter> typeParameters = classDecl.getTypeParameters();
            if (typeParameters != null && typeParameters.size()>0) {
                for (int i = 0; i < typeParameters.size(); i++) {
                    if(nodes.size()>0 && nodes.get(i) != null) {
                        try {
                            typeMap.put(typeParameters.get(i).getNameAsString(), (Type) nodes.get(i));
                        }catch (Exception e) {
                            logger.error("parseJavaFile error: {}", classDecl.toString());
                        }
                    }
                }
            }
            classDecl.getFields().forEach(field -> {
                FieldInfo fieldInfo = extractFieldInfo(field, typeMap);
                fieldInfos.add(fieldInfo);
            });
        });

        return fieldInfos;
    }

    /**
     * 提取单个字段的详细信息
     */
    private static FieldInfo extractFieldInfo(FieldDeclaration field, Map<String, Type> typeMap) {
        FieldInfo fieldInfo = new FieldInfo();
        // 获取字段类型
        String fieldType = field.getElementType().toString();
        if (typeMap.get(fieldType) != null) {
            Type elementType = typeMap.get(fieldType).getElementType();
            fieldInfo.setClassType(elementType);
            if (elementType.isClassOrInterfaceType()) {
                String name = ((ClassOrInterfaceType) elementType).getName().asString();
                fieldInfo.setFieldType(name);
            } else {
                fieldInfo.setFieldType(typeMap.get(fieldType).getElementType().asString());
            }
        } else {
            fieldInfo.setClassType(field.getElementType());
            fieldInfo.setFieldType(fieldType);
        }

        // 获取字段名
        field.getVariables().forEach(variable -> {
            fieldInfo.setFieldName(variable.getNameAsString());
        });

        // 获取注释
        field.getComment().ifPresent(comment -> {
            fieldInfo.setComment(comment.getContent());
        });

        // 处理泛型
        if (field.getElementType().isClassOrInterfaceType()) {
            var classType = field.getElementType().asClassOrInterfaceType();
            if (classType.getTypeArguments().isPresent()) {
                classType.getTypeArguments().get().forEach(typeArg -> {
                    fieldInfo.getGenericTypes().add(typeArg.toString());
                });
            }
        }

        return fieldInfo;
    }
}
