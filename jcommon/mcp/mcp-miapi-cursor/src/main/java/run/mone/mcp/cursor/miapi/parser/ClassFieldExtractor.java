package run.mone.mcp.cursor.miapi.parser;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ClassFieldExtractor {
    public static class FieldInfo {
        private String fieldName;
        private String fieldType;
        private List<String> genericTypes = new ArrayList<>();
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

        @Override
        public String toString() {
            return String.format("Field{name='%s', type='%s', generics=%s}",
                    fieldName, fieldType, genericTypes);
        }
    }

    /**
     * 根据类名扫描项目目录查找类文件
     */
    public static List<FieldInfo> findClassAndExtractFields(String className, String projectRoot) throws IOException {
        List<File> classFiles = findClassFiles(className, projectRoot);
        List<FieldInfo> allFields = new ArrayList<>();

        for (File file : classFiles) {
            allFields.addAll(parseJavaFile(file));
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
    private static List<FieldInfo> parseJavaFile(File javaFile) throws IOException {
        List<FieldInfo> fieldInfos = new ArrayList<>();
        CompilationUnit cu = StaticJavaParser.parse(javaFile);

        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
            classDecl.getFields().forEach(field -> {
                FieldInfo fieldInfo = extractFieldInfo(field);
                fieldInfos.add(fieldInfo);
            });
        });

        return fieldInfos;
    }

    /**
     * 提取单个字段的详细信息
     */
    private static FieldInfo extractFieldInfo(FieldDeclaration field) {
        FieldInfo fieldInfo = new FieldInfo();

        // 获取字段类型
        String fieldType = field.getElementType().toString();
        fieldInfo.setFieldType(fieldType);

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
