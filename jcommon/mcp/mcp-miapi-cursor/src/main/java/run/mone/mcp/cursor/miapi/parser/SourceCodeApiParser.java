package run.mone.mcp.cursor.miapi.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
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
import run.mone.mcp.cursor.miapi.util.TypeExtractorUtil;

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

    private String codeRoot = "";

    private Integer batchSize = 20;
    
    public SourceCodeApiParser() {
        this(null);
    }
    
    public SourceCodeApiParser(String sourcePath) {
        codeRoot = sourcePath;
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
            for (int i = 0; i < javaFiles.size(); i+=batchSize) {
                logger.info("javaFiles: {}", i + batchSize);
                List<String> list = javaFiles.subList(i, Math.min(i + batchSize, javaFiles.size()));
                list.forEach(this::parseFile);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    logger.error("解析失败: ", e);
                    break;
                }
            }

            for (int i = 0; i < cus.size(); i+=batchSize) {
                logger.info("cus: {}", i + batchSize);
                List<CompilationUnit> compilationUnits = cus.subList(i, Math.min(i + batchSize, cus.size()));
                for (CompilationUnit compilationUnit : compilationUnits) {
                    ParserResult fileResult = new ParserResult();
                    result.setSuccess(true);
                    parseCompilationUnit(compilationUnit, fileResult);
                    mergeResults(result, fileResult);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    logger.error("解析失败: ", e);
                    break;
                }
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

    private void deepField (Type type, ParameterInfo parameterInfo) {
        List<ParameterInfo> children = new ArrayList<>();
        parameterInfo.setChildList(children);
        try {
            if (!TypeExtractorUtil.isInternalType(type)) {
                List<Node> childNodes = ((Node) type).getChildNodes();
                List<Node> nodes = childNodes.subList(1, childNodes.size());
                String className = ((ClassOrInterfaceType)type).getName().asString();
//                List<String> list = TypeExtractorUtil.extractTypesByLevel(type.asString());
                if (className != null) {
                    List<ClassFieldExtractor.FieldInfo> fields = ClassFieldExtractor.findClassAndExtractFields(className, codeRoot, nodes);
                    if (fields.size() > 0) {
                        List<ParameterInfo> parameterInfoList = new ArrayList<>();
                        for (ClassFieldExtractor.FieldInfo field : fields) {
                            ParameterInfo info = new ParameterInfo();
                            info.setName(field.getFieldName());
                            info.setDescription(field.getComment());
                            info.setGenericType(String.join(",",field.getGenericTypes()));
                            info.setType(TypeExtractorUtil.typeStr2TypeNo(field.getFieldType()));
                            deepField(field.getClassType(), info);
                            parameterInfoList.add(info);
                        }
                        parameterInfo.setChildList(parameterInfoList);
                    } else {
                        parameterInfo.setType(TypeExtractorUtil.typeStr2TypeNo(TypeExtractorUtil.getPrimitiveSimpleName(type)));
                    }
                }
            } else {
                parameterInfo.setType(TypeExtractorUtil.typeStr2TypeNo(TypeExtractorUtil.getPrimitiveSimpleName(type)));
            }
        }catch (Exception e) {
            logger.error("deepField error: ", e);
        }
    }

    /**
     * 解析参数
     */
    private void parseParameters(MethodDeclaration method, ApiInfo apiInfo) {
        List<ParameterInfo> parameters = new ArrayList<>();
        
        method.getParameters().forEach(param -> {
            ParameterInfo paramInfo = new ParameterInfo();
            paramInfo.setName(param.getNameAsString());
            paramInfo.setClassType(param.getType());
            paramInfo.setPosition("body"); // 默认位置
            deepField(param.getType(), paramInfo);
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
        ParameterInfo paramInfo = new ParameterInfo();
        paramInfo.setName("root");
        paramInfo.setClassType(method.getType());
        deepField(method.getType(), paramInfo);
        apiInfo.setReturnFields(List.of(paramInfo));
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
