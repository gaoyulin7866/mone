package run.mone.mcp.cursor.miapi.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import run.mone.mcp.cursor.miapi.model.ApiInfo;
import run.mone.mcp.cursor.miapi.model.ParameterInfo;
import run.mone.mcp.cursor.miapi.model.ParserResult;
import run.mone.mcp.cursor.miapi.util.FileScanner;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于源码解析的API解析器
 * 通过解析Java源码文件来提取接口信息
 */
public class SourceCodeApiParser {
    
    private static final Logger logger = LoggerFactory.getLogger(SourceCodeApiParser.class);
    private final JavaParser javaParser;

    private final List<CompilationUnit> cus = new ArrayList<>();
    
    public SourceCodeApiParser() {
        this(null);
    }
    
    public SourceCodeApiParser(String sourcePath) {
        // 配置SymbolResolver以支持类型解析
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());
        
        // 添加JavaParserTypeSolver来解析项目中的自定义类型
        if (sourcePath != null && !sourcePath.isEmpty()) {
            try {
                File sourceDir = new File(sourcePath);
                if (sourceDir.exists() && sourceDir.isDirectory()) {
                    typeSolver.add(new JavaParserTypeSolver(sourceDir));
                    logger.debug("已添加JavaParserTypeSolver，源码路径: {}", sourcePath);
                } else {
                    logger.warn("指定的源码路径不存在或不是目录: {}", sourcePath);
                }
            } catch (Exception e) {
                logger.warn("无法配置JavaParserTypeSolver: {}", e.getMessage());
            }
        } else {
            logger.debug("未提供源码路径，跳过JavaParserTypeSolver配置");
        }
        
        // 创建ParserConfiguration并设置SymbolResolver
        ParserConfiguration config = new ParserConfiguration();
        config.setSymbolResolver(new JavaSymbolSolver(typeSolver));
        
        this.javaParser = new JavaParser(config);
        logger.debug("SourceCodeApiParser初始化完成，已配置SymbolResolver");
    }
    
    /**
     * 解析单个Java文件
     * @param filePath 文件路径
     * @return 解析结果
     */
    public void parseFile(String filePath) {
        logger.debug("开始解析文件: {}", filePath);
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                logger.warn("文件不存在: {}", filePath);
            }
            
            CompilationUnit cu = javaParser.parse(file).getResult().orElse(null);
            if (cu == null) {
                logger.warn("无法解析Java文件CompilationUnit: {}", filePath);
            }
            // 解析类信息
            cus.add(cu);

        } catch (FileNotFoundException e) {
            logger.error("文件未找到: {}", filePath, e);
        } catch (Exception e) {
            logger.error("解析文件失败: {}", filePath, e);
        }
    }
    
    /**
     * 解析目录下的所有Java文件
     * @param directoryPath 目录路径
     * @return 解析结果
     */
    public ParserResult parseDirectory(String directoryPath) {
        logger.info("开始解析目录: {}", directoryPath);
        ParserResult result = new ParserResult();
        result.setSuccess(true);
        
        try {
            List<String> javaFiles = FileScanner.scanJavaFiles(directoryPath);
            logger.info("找到 {} 个Java文件", javaFiles.size());
            
            if (javaFiles.isEmpty()) {
                logger.warn("目录下没有找到Java文件: {}", directoryPath);
                result.setSuccess(false);
                result.setErrorMessage("目录下没有找到Java文件: " + directoryPath);
                return result;
            }
            
            for (String filePath : javaFiles) {
                parseFile(filePath);
            }

            for (int i = 0; i < cus.size(); i++) {
                ParserResult fileResult = new ParserResult();
                result.setSuccess(true);
                parseCompilationUnit(cus.get(i), fileResult);
                mergeResults(result, fileResult);
            }


            
            // 处理相同地址的区分
            processDuplicatePaths(result);
            logger.info("目录解析完成: {}, 总接口数: {}", directoryPath, result.getParsedApiCount());
            
        } catch (Exception e) {
            logger.error("解析目录失败: {}", directoryPath, e);
            result.setSuccess(false);
            result.setErrorMessage("解析目录失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 解析编译单元
     */
    private void parseCompilationUnit(CompilationUnit cu, ParserResult result) {
        cu.accept(new VoidVisitorAdapter<CompilationUnit>() {
            @Override
            public void visit(ClassOrInterfaceDeclaration n, CompilationUnit arg) {
                if (isControllerClass(n)) {
                    String className = n.getNameAsString();
                    String qualifiedName = n.getFullyQualifiedName().orElse("");
                    
                    // 解析Controller级别的路径
                    String controllerPath = getControllerPath(n);

                    // 解析方法
                    for (MethodDeclaration method : n.getMethods()) {
                        if (isApiMethod(method)) {
                            ApiInfo apiInfo = parseApiMethod(method, className, qualifiedName, controllerPath, arg);
                            if (apiInfo != null) {
//                                result.addApi(apiInfo);
                                result.addApiToGroup(qualifiedName, apiInfo);
//                                result.addApiToPath(apiInfo.getPath(), apiInfo);
                                result.setParsedApiCount(result.getParsedApiCount() + 1);
                            }
                        }
                    }
                    
                    result.setParsedFileCount(result.getParsedFileCount() + 1);
                }
                super.visit(n, arg);
            }

        }, cu);
    }
    
    /**
     * 检查是否是Controller类
     */
    private boolean isControllerClass(ClassOrInterfaceDeclaration n) {
        return n.getAnnotationByName("Controller").isPresent() ||
               n.getAnnotationByName("RestController").isPresent() ||
               n.getAnnotationByName("RequestMapping").isPresent();
    }
    
    /**
     * 获取Controller级别的路径
     */
    private String getControllerPath(ClassOrInterfaceDeclaration n) {
        return n.getAnnotationByName("RequestMapping")
                .map(annotation -> getAnnotationValue(annotation, "value"))
                .orElse("");
    }
    
    /**
     * 检查是否是API方法
     */
    private boolean isApiMethod(MethodDeclaration method) {
        return method.getAnnotationByName("RequestMapping").isPresent() ||
               method.getAnnotationByName("GetMapping").isPresent() ||
               method.getAnnotationByName("PostMapping").isPresent() ||
               method.getAnnotationByName("PutMapping").isPresent() ||
               method.getAnnotationByName("DeleteMapping").isPresent() ||
               method.getAnnotationByName("PatchMapping").isPresent();
    }
    
    /**
     * 解析API方法
     */
    private ApiInfo parseApiMethod(MethodDeclaration method, String className, String qualifiedName, String controllerPath, CompilationUnit cu) {
        ApiInfo apiInfo = new ApiInfo();

        // 设置基本信息
        apiInfo.setName(method.getNameAsString());
        apiInfo.setControllerClass(className);
        apiInfo.setControllerQualifiedName(qualifiedName);
        apiInfo.setMethodSignature(method.getSignature().asString());
        apiInfo.setDeprecated(method.getAnnotationByName("Deprecated").isPresent());
        
        // 解析请求映射
        parseRequestMapping(method, apiInfo, controllerPath);
        
        // 解析参数
        parseParameters(method, apiInfo);
        
        // 解析返回值
        parseReturnType(method, apiInfo, cu);
        
        return apiInfo;
    }
    
    /**
     * 解析请求映射
     */
    private void parseRequestMapping(MethodDeclaration method, ApiInfo apiInfo, String controllerPath) {
        String methodType = "GET";
        String path = "";
        
        if (method.getAnnotationByName("GetMapping").isPresent()) {
            methodType = "GET";
            path = getAnnotationValue(method.getAnnotationByName("GetMapping").get(), "value");
        } else if (method.getAnnotationByName("PostMapping").isPresent()) {
            methodType = "POST";
            path = getAnnotationValue(method.getAnnotationByName("PostMapping").get(), "value");
        } else if (method.getAnnotationByName("PutMapping").isPresent()) {
            methodType = "PUT";
            path = getAnnotationValue(method.getAnnotationByName("PutMapping").get(), "value");
        } else if (method.getAnnotationByName("DeleteMapping").isPresent()) {
            methodType = "DELETE";
            path = getAnnotationValue(method.getAnnotationByName("DeleteMapping").get(), "value");
        } else if (method.getAnnotationByName("PatchMapping").isPresent()) {
            methodType = "PATCH";
            path = getAnnotationValue(method.getAnnotationByName("PatchMapping").get(), "value");
        } else if (method.getAnnotationByName("RequestMapping").isPresent()) {
            AnnotationExpr annotation = method.getAnnotationByName("RequestMapping").get();
            path = getAnnotationValue(annotation, "value");
            if (path.isEmpty()) {
                path = getAnnotationValue(annotation, "path");
            }
            String methodValue = getAnnotationValue(annotation, "method");
            if (!methodValue.isEmpty()) {
                methodType = methodValue;
            }
        }
        
        // 组合完整路径
        String fullPath = controllerPath + path;
        if (!fullPath.startsWith("/")) {
            fullPath = "/" + fullPath;
        }
        
        apiInfo.setMethod(methodType);
        apiInfo.setPath(fullPath);
    }
    
    /**
     * 解析参数
     */
    private void parseParameters(MethodDeclaration method, ApiInfo apiInfo) {
        List<ParameterInfo> parameters = new ArrayList<>();
        
        method.getParameters().forEach(param -> {
            ParameterInfo paramInfo = new ParameterInfo();
            paramInfo.setName(param.getNameAsString());
            paramInfo.setType(param.getType().asString());
            paramInfo.setPosition("body"); // 默认位置
            
            // 解析参数注解
            if (param.getAnnotationByName("RequestParam").isPresent()) {
                paramInfo.setPosition("query");
                String value = getAnnotationValue(param.getAnnotationByName("RequestParam").get(), "value");
                if (!value.isEmpty()) {
                    paramInfo.setName(value);
                }
                paramInfo.setRequired(getAnnotationBooleanValue(param.getAnnotationByName("RequestParam").get(), "required", true));
            } else if (param.getAnnotationByName("PathVariable").isPresent()) {
                paramInfo.setPosition("path");
                String value = getAnnotationValue(param.getAnnotationByName("PathVariable").get(), "value");
                if (!value.isEmpty()) {
                    paramInfo.setName(value);
                }
                paramInfo.setRequired(true);
            } else if (param.getAnnotationByName("RequestBody").isPresent()) {
                paramInfo.setPosition("body");
                paramInfo.setRequired(true);
            } else if (param.getAnnotationByName("RequestHeader").isPresent()) {
                paramInfo.setPosition("header");
                String value = getAnnotationValue(param.getAnnotationByName("RequestHeader").get(), "value");
                if (!value.isEmpty()) {
                    paramInfo.setName(value);
                }
                paramInfo.setRequired(getAnnotationBooleanValue(param.getAnnotationByName("RequestHeader").get(), "required", true));
            }
            
            parameters.add(paramInfo);
        });
        
        apiInfo.setInputParameters(parameters);
    }
    
    /**
     * 解析返回值类型
     */
    private void parseReturnType(MethodDeclaration method, ApiInfo apiInfo, CompilationUnit cu) {
        String returnType = method.getType().asString();
        apiInfo.setReturnType(returnType);
        apiInfo.setReturnDescription("返回值类型: " + returnType);

        NodeList<ImportDeclaration> imports = cu.getImports();

        // 尝试解析返回类型的字段信息
        try {
//            List<String> list = TypeExtractorUtil.extractTypesByLevel(returnType);


            for (int i = 0; i < cus.size(); i++) {
                CompilationUnit compilationUnit = cus.get(i);
                PackageDeclaration packageDeclaration = compilationUnit.getPackageDeclaration().orElse(null);
                NodeList<TypeDeclaration<?>> types = compilationUnit.getTypes();

//                cus.get(i).getClassByName(list.get(0)).ifPresent(v-> {
//                    String className = v.getFullyQualifiedName().orElse("");
//                    Class<?> clazz = tryLoadClass(className);
//                });
            }
//            resolveClassNameFromImports(returnType, imports);
            // 从返回类型字符串中提取类名
//            String className = extractClassNameFromType(returnType);
//            if (className != null && !isBasicType(className)) {
//                // 尝试不同的包名组合
//                Class<?> clazz = tryLoadClass(className);
//                if (clazz != null) {
//                    List<ParameterInfo> returnFields = ReturnTypeFieldParser.parseReturnFields(clazz);
//                    apiInfo.setReturnFields(returnFields);
//                }
//            }
        } catch (com.github.javaparser.resolution.UnsolvedSymbolException e) {
            // 处理无法解析的符号，尝试通过imports查找自定义类
            logger.debug("无法解析返回类型符号: {}, 尝试通过imports查找: {}", e.getMessage(), returnType);
            tryResolveCustomClass(returnType, imports, apiInfo);
        } catch (Exception e) {
            // 忽略其他解析错误，尝试通过imports查找自定义类
            logger.debug("无法解析返回类型字段: {}, 尝试通过imports查找: {}", returnType, e.getMessage());
            tryResolveCustomClass(returnType, imports, apiInfo);
        }
    }
    
    /**
     * 尝试通过imports解析自定义类
     */
    private void tryResolveCustomClass(String returnType, NodeList<ImportDeclaration> imports, ApiInfo apiInfo) {
        // 从返回类型中提取类名
        String className = extractClassNameFromType(returnType);
        if (className == null || isBasicType(className)) {
            return;
        }
        
        logger.debug("尝试解析自定义类: {}", className);
        
        // 尝试通过imports查找完整的类名
        String fullClassName = resolveClassNameFromImports(className, imports);
        if (fullClassName != null) {
            logger.debug("通过imports找到完整类名: {}", fullClassName);
            try {
                Class<?> clazz = Class.forName(fullClassName);
                List<ParameterInfo> returnFields = ReturnTypeFieldParser.parseReturnFields(clazz);
                apiInfo.setReturnFields(returnFields);
                apiInfo.setReturnDescription("返回值类型: " + returnType + " (已解析字段信息)");
                logger.debug("成功解析自定义类字段: {}, 字段数: {}", fullClassName, returnFields.size());
            } catch (ClassNotFoundException e) {
                logger.debug("无法加载类: {}, 错误: {}", fullClassName, e.getMessage());
                apiInfo.setReturnDescription("返回值类型: " + returnType + " (无法加载类: " + fullClassName + ")");
            } catch (Exception e) {
                logger.debug("解析类字段失败: {}, 错误: {}", fullClassName, e.getMessage());
                apiInfo.setReturnDescription("返回值类型: " + returnType + " (解析字段失败: " + e.getMessage() + ")");
            }
        } else {
            logger.debug("无法通过imports找到类: {}", className);
            apiInfo.setReturnDescription("返回值类型: " + returnType + " (无法解析详细信息)");
        }
    }
    
    /**
     * 通过imports解析类名
     */
    private String resolveClassNameFromImports(String className, NodeList<ImportDeclaration> imports) {
        // 首先检查是否有直接导入的类
        for (ImportDeclaration importDecl : imports) {
            String importName = importDecl.getNameAsString();
            String importedClassName = getClassNameFromImport(importName);
            
            if (className.equals(importedClassName)) {
                logger.debug("找到直接导入的类: {} -> {}", className, importName);
                return importName;
            }
        }
        
        // 检查是否有通配符导入（*号导入）
        for (ImportDeclaration importDecl : imports) {
            String packageName = importDecl.getNameAsString();
            if (packageName.endsWith(".*")) {
                packageName = packageName.substring(0, packageName.length() - 2);
            }
            String fullClassName = packageName + "." + className;

            // 尝试加载这个类来验证是否存在
            try {
                Class.forName(fullClassName);
                logger.debug("找到通配符导入的类: {} -> {}", className, fullClassName);
                return fullClassName;
            } catch (ClassNotFoundException e) {
                // 继续尝试下一个包
                logger.debug("通配符导入包 {} 中未找到类: {}", packageName, className);
            }
        }
        
        return null;
    }
    
    /**
     * 从导入语句中提取类名
     */
    private String getClassNameFromImport(String importName) {
        if (importName.contains(".")) {
            return importName.substring(importName.lastIndexOf(".") + 1);
        }
        return importName;
    }
    
    /**
     * 从类型字符串中提取类名
     */
    private String extractClassNameFromType(String typeString) {
        // 处理泛型类型，如 ResponseEntity<Map<String,Object>>
        if (typeString.contains("<")) {
            // 提取泛型参数
            int start = typeString.indexOf('<');
            int end = typeString.lastIndexOf('>');
            if (start > 0 && end > start) {
                String genericParam = typeString.substring(start + 1, end);
                // 取第一个泛型参数
                if (genericParam.contains(",")) {
                    genericParam = genericParam.substring(0, genericParam.indexOf(',')).trim();
                }
                return genericParam;
            }
        }
        
        // 处理简单类型
        return typeString;
    }
    
    /**
     * 尝试加载类
     */
    private Class<?> tryLoadClass(String className) {
        // 尝试直接加载
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            // 尝试从当前类路径中查找
            try {
                // 使用当前线程的类加载器
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                if (classLoader != null) {
                    return classLoader.loadClass(className);
                }
            } catch (ClassNotFoundException ignored) {
                // 继续尝试其他方法
            }
        }
        return null;
    }
    
    /**
     * 判断是否是基本类型
     */
    private boolean isBasicType(String typeName) {
        return typeName.equals("String") ||
               typeName.equals("int") ||
               typeName.equals("Integer") ||
               typeName.equals("long") ||
               typeName.equals("Long") ||
               typeName.equals("double") ||
               typeName.equals("Double") ||
               typeName.equals("float") ||
               typeName.equals("Float") ||
               typeName.equals("boolean") ||
               typeName.equals("Boolean") ||
               typeName.equals("char") ||
               typeName.equals("Character") ||
               typeName.equals("byte") ||
               typeName.equals("Byte") ||
               typeName.equals("short") ||
               typeName.equals("Short") ||
               typeName.equals("void") ||
               typeName.equals("Void");
    }
    
    /**
     * 获取注解值
     */
    private String getAnnotationValue(AnnotationExpr annotation, String key) {
        if (annotation instanceof NormalAnnotationExpr) {
            NormalAnnotationExpr normalAnnotation = (NormalAnnotationExpr) annotation;
            return normalAnnotation.getPairs().stream()
                    .filter(pair -> pair.getNameAsString().equals(key))
                    .findFirst()
                    .map(pair -> {
                        if (pair.getValue() instanceof StringLiteralExpr) {
                            return ((StringLiteralExpr) pair.getValue()).getValue();
                        }
                        return pair.getValue().toString().replaceAll("\"", "");
                    })
                    .orElse("");
        } else if (annotation instanceof SingleMemberAnnotationExpr) {
            SingleMemberAnnotationExpr singleAnnotation = (SingleMemberAnnotationExpr) annotation;
            if (singleAnnotation.getMemberValue() instanceof StringLiteralExpr) {
                return ((StringLiteralExpr) singleAnnotation.getMemberValue()).getValue();
            }
            return singleAnnotation.getMemberValue().toString().replaceAll("\"", "");
        }
        return "";
    }
    
    /**
     * 获取注解布尔值
     */
    private boolean getAnnotationBooleanValue(AnnotationExpr annotation, String key, boolean defaultValue) {
        if (annotation instanceof NormalAnnotationExpr) {
            NormalAnnotationExpr normalAnnotation = (NormalAnnotationExpr) annotation;
            return normalAnnotation.getPairs().stream()
                    .filter(pair -> pair.getNameAsString().equals(key))
                    .findFirst()
                    .map(pair -> Boolean.parseBoolean(pair.getValue().toString()))
                    .orElse(defaultValue);
        }
        return defaultValue;
    }
    
    /**
     * 合并解析结果
     */
    private void mergeResults(ParserResult target, ParserResult source) {
        // 合并按Controller分组的结果
        for (String key : source.getApiGroups().keySet()) {
            target.getApiGroups().computeIfAbsent(key, k -> new ArrayList<>())
                  .addAll(source.getApiGroups().get(key));
        }
        
        // 更新统计信息
        target.setParsedFileCount(target.getParsedFileCount() + source.getParsedFileCount());
        target.setParsedApiCount(target.getParsedApiCount() + source.getParsedApiCount());
    }
    
    /**
     * 处理相同地址的区分
     */
    private void processDuplicatePaths(ParserResult result) {

    }
}
