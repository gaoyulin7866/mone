package run.mone.mcp.cursor.miapi.util;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class TypeExtractorUtil {

    /**
     * 提取嵌套类型名称，按照从外到里的层级顺序
     * @param typeStr 类型字符串，如 "GoodInfo<List<Map<String,Object>>>"
     * @return 按层级排序的类型名称列表
     */
    public static List<String> extractTypesByLevel(String typeStr) {
        List<String> result = new ArrayList<>();
        if (typeStr == null || typeStr.trim().isEmpty()) {
            return result;
        }

        // 移除所有空格以便处理
        String cleanedStr = typeStr.replaceAll("\\s+", "");

        // 使用栈来匹配尖括号并提取类型
        Deque<Integer> stack = new ArrayDeque<>();
        List<TypeSegment> segments = new ArrayList<>();

        for (int i = 0; i < cleanedStr.length(); i++) {
            char c = cleanedStr.charAt(i);
            if (c == '<') {
                stack.push(i);
            } else if (c == '>') {
                if (!stack.isEmpty()) {
                    int start = stack.pop();
                    if (stack.isEmpty()) {
                        // 当栈为空时，说明这是一个完整的顶层类型段
                        String segment = cleanedStr.substring(start + 1, i);
                        segments.add(new TypeSegment(segment, getLevel(cleanedStr, start)));
                    }
                }
            }
        }

        // 添加最外层类型
        String outerType = cleanedStr.split("<")[0];
        result.add(outerType);

        // 按层级排序并添加内部类型
        segments.stream()
                .sorted(Comparator.comparingInt(TypeSegment::getLevel))
                .forEach(segment -> {
                    // 提取该段中的类型名称
                    extractTypesFromSegment(segment.getContent(), result);
                });

        return result;
    }

    /**
     * 从类型段中提取类型名称
     */
    private static void extractTypesFromSegment(String segment, List<String> result) {
        // 使用逗号分割，但要注意嵌套的泛型
        List<String> parts = splitByComma(segment);

        for (String part : parts) {
            // 提取类型名称（去掉可能的泛型参数）
            String typeName = part.split("<")[0].trim();
            if (!typeName.isEmpty() && !result.contains(typeName)) {
                result.add(typeName);
            }

            // 递归处理嵌套的泛型
            if (part.contains("<")) {
                int start = part.indexOf('<');
                int end = part.lastIndexOf('>');
                if (start < end) {
                    String nested = part.substring(start + 1, end);
                    extractTypesFromSegment(nested, result);
                }
            }
        }
    }

    /**
     * 按逗号分割，但跳过嵌套在尖括号内的逗号
     */
    private static List<String> splitByComma(String str) {
        List<String> result = new ArrayList<>();
        if (str == null || str.isEmpty()) {
            return result;
        }

        Deque<Character> bracketStack = new ArrayDeque<>();
        StringBuilder current = new StringBuilder();

        for (char c : str.toCharArray()) {
            if (c == '<' || c == '[' || c == '(') {
                bracketStack.push(c);
            } else if (c == '>' || c == ']' || c == ')') {
                if (!bracketStack.isEmpty()) {
                    bracketStack.pop();
                }
            }

            if (c == ',' && bracketStack.isEmpty()) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            result.add(current.toString());
        }

        return result;
    }

    /**
     * 计算类型的嵌套层级
     */
    private static int getLevel(String str, int position) {
        int level = 0;
        for (int i = 0; i < position; i++) {
            char c = str.charAt(i);
            if (c == '<') {
                level++;
            } else if (c == '>') {
                level--;
            }
        }
        return level;
    }

    // 辅助类，用于存储类型段和其层级
    private static class TypeSegment {
        private final String content;
        private final int level;

        public TypeSegment(String content, int level) {
            this.content = content;
            this.level = level;
        }

        public String getContent() {
            return content;
        }

        public int getLevel() {
            return level;
        }
    }


    public static List<String> filterNonJavaInternalTypesStrict(String[] typeNames) {
        if (typeNames == null) {
            return new ArrayList<>();
        }

        List<String> result = new ArrayList<>();

        for (String typeName : typeNames) {
            if (typeName == null || typeName.trim().isEmpty()) {
                continue;
            }

            String cleanTypeName = typeName.trim();

            try {
                // 尝试加载类，如果成功且在java包下，则认为是Java内部类型
                Class<?> clazz = Class.forName(cleanTypeName);
                if (!clazz.getName().startsWith("java.")) {
                    result.add(cleanTypeName);
                }
            } catch (ClassNotFoundException e) {
                // 如果类不存在，认为不是Java内部类型
                result.add(cleanTypeName);
            } catch (Exception e) {
                // 其他异常，保守起见认为不是Java内部类型
                result.add(cleanTypeName);
            }
        }

        return result;
    }


    /**
     * 根据给定的字符串数组筛选出不是Java内部类型的字符串
     * @param typeNames 类型名称数组
     * @return 非Java内部类型的字符串列表
     */
    public static List<String> filterNonJavaInternalTypes(String[] typeNames) {
        if (typeNames == null) {
            return new ArrayList<>();
        }

        // Java内部类型集合（包括基本类型、包装类、常用JDK类等）
        Set<String> javaInternalTypes = new HashSet<>(Arrays.asList(
                // 基本类型
                "byte", "short", "int", "long", "float", "double", "boolean", "char",
                "void",

                // 包装类
                "Byte", "Short", "Integer", "Long", "Float", "Double", "Boolean", "Character", "Void",

                // 常用java.lang包中的类
                "Object", "String", "Class", "Package", "Enum", "Throwable", "Exception", "Error",
                "RuntimeException", "NullPointerException", "IllegalArgumentException",
                "Number", "Math", "System", "Thread", "Runnable", "Comparable",

                // 常用java.util包中的类
                "List", "ArrayList", "LinkedList", "Set", "HashSet", "TreeSet", "Map", "HashMap",
                "TreeMap", "Collection", "Arrays", "Collections", "Iterator", "Date", "Calendar",

                // 常用java.io包中的类
                "File", "InputStream", "OutputStream", "Reader", "Writer",

                // 其他常用JDK类
                "BigInteger", "BigDecimal", "Pattern", "Matcher", "LocalDate", "LocalDateTime"
        ));

        List<String> result = new ArrayList<>();

        for (String typeName : typeNames) {
            if (typeName == null || typeName.trim().isEmpty()) {
                continue;
            }

            String cleanTypeName = typeName.trim();

            // 检查是否是Java内部类型
            if (!javaInternalTypes.contains(cleanTypeName)) {
                result.add(cleanTypeName);
            }
        }

        return result;
    }
}
