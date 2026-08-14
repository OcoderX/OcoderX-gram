/*
 * This is the source code of AyuGram / OcoderX for Android.
 *
 * Copyright @Radolyn, 2023-2026.
 */

package com.radolyn.ayugram.utils;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeSyntaxHighlighter {

    // --- Common Regex Patterns ---
    private static final Pattern NUMBERS = Pattern.compile("\\b0[xX][0-9a-fA-F]+[uUlL]*\\b|\\b0[bB][01]+[uUlL]*\\b|\\b\\d+(\\.\\d+)?([eE][+-]?\\d+)?[fFdDlL]?\\b");

    // Python
    private static final Pattern PY_COMMENTS = Pattern.compile("#.*$", Pattern.MULTILINE);
    private static final Pattern PY_STRINGS = Pattern.compile("(\"\"\"[\\s\\S]*?\"\"\"|'''[\\s\\S]*?'''|[rRfFbBuU]?\"(?:\\\\.|[^\"\\\\])*\"|[rRfFbBuU]?'(?:\\\\.|[^'\\\\])*')");
    private static final Pattern PY_KEYWORDS = Pattern.compile("\\b(and|as|assert|async|await|break|case|class|continue|def|del|elif|else|except|exec|finally|for|from|global|if|import|in|is|lambda|match|nonlocal|not|or|pass|raise|return|try|while|with|yield)\\b");
    private static final Pattern PY_BUILTINS = Pattern.compile("\\b(abs|all|any|bin|bool|bytearray|bytes|callable|chr|classmethod|compile|complex|delattr|dict|dir|divmod|enumerate|eval|filter|float|format|frozenset|getattr|globals|hasattr|hash|help|hex|id|input|int|isinstance|issubclass|iter|len|list|locals|map|max|memoryview|min|next|object|oct|open|ord|pow|print|property|range|repr|reversed|round|set|setattr|slice|sorted|staticmethod|str|sum|super|tuple|type|vars|zip)\\b");
    private static final Pattern PY_CONSTANTS = Pattern.compile("\\b(True|False|None|self|cls)\\b");
    private static final Pattern PY_DECORATORS = Pattern.compile("@[a-zA-Z_]\\w*(\\.[a-zA-Z_]\\w*)*");
    private static final Pattern PY_FUNCTIONS = Pattern.compile("\\bdef\\s+([a-zA-Z_]\\w*)|([a-zA-Z_]\\w*)\\s*(?=\\()");

    // JavaScript / TypeScript
    private static final Pattern JS_COMMENTS = Pattern.compile("(//.*$|/\\*[\\s\\S]*?\\*/)", Pattern.MULTILINE);
    private static final Pattern JS_STRINGS = Pattern.compile("(`(?:\\\\.|[^`\\\\])*`|\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*')");
    private static final Pattern JS_KEYWORDS = Pattern.compile("\\b(abstract|any|as|async|await|boolean|break|case|catch|class|const|constructor|continue|debugger|declare|default|delete|do|else|enum|export|extends|false|finally|for|from|function|get|if|implements|import|in|infer|instanceof|interface|is|keyof|let|module|namespace|never|new|null|number|of|package|private|protected|public|readonly|require|return|set|static|string|super|switch|symbol|this|throw|true|try|type|typeof|undefined|unknown|var|void|while|with|yield)\\b");
    private static final Pattern JS_BUILTINS = Pattern.compile("\\b(console|document|window|Math|JSON|Promise|Array|Object|String|Number|Boolean|Date|RegExp|Map|Set|Symbol|BigInt|setTimeout|setInterval|clearTimeout|clearInterval|fetch|process|global|globalThis|Error|Response|Request|URL)\\b");
    private static final Pattern JS_FUNCTIONS = Pattern.compile("([a-zA-Z_$]\\w*)\\s*(?=\\()");

    // Java / Kotlin
    private static final Pattern JAVA_COMMENTS = Pattern.compile("(//.*$|/\\*[\\s\\S]*?\\*/)", Pattern.MULTILINE);
    private static final Pattern JAVA_STRINGS = Pattern.compile("(\"\"\"[\\s\\S]*?\"\"\"|\"(?:\\\\.|[^\"\\\\])*\"|'(\\\\.|[^'\\\\])')");
    private static final Pattern JAVA_KEYWORDS = Pattern.compile("\\b(abstract|assert|boolean|break|byte|case|catch|char|class|const|continue|default|do|double|else|enum|extends|final|finally|float|for|goto|if|implements|import|instanceof|int|interface|long|native|new|package|private|protected|public|return|short|static|strictfp|super|switch|synchronized|this|throw|throws|transient|try|void|volatile|while|val|var|fun|when|is|in|object|companion|data|sealed|override|open|suspend|inline|crossinline|noinline|lateinit|by|lazy|constructor|init|inner|tailrec|operator|infix)\\b");
    private static final Pattern JAVA_TYPES = Pattern.compile("\\b(String|Integer|Long|Float|Double|Boolean|Character|Byte|Short|List|Map|Set|ArrayList|HashMap|HashSet|LinkedList|TreeMap|TreeSet|Optional|Object|Thread|Runnable|Exception|Throwable|Unit|Any|Nothing|CharSequence|StringBuilder|StringBuffer|Activity|Context|View|Intent)\\b");
    private static final Pattern JAVA_CONSTANTS = Pattern.compile("\\b(true|false|null)\\b");
    private static final Pattern JAVA_ANNOTATIONS = Pattern.compile("@[a-zA-Z_]\\w*");
    private static final Pattern JAVA_FUNCTIONS = Pattern.compile("([a-zA-Z_]\\w*)\\s*(?=\\()");

    // C / C++ / C#
    private static final Pattern C_PREPROCESSOR = Pattern.compile("^\\s*#[a-zA-Z_]\\w*.*$", Pattern.MULTILINE);
    private static final Pattern C_COMMENTS = Pattern.compile("(//.*$|/\\*[\\s\\S]*?\\*/)", Pattern.MULTILINE);
    private static final Pattern C_STRINGS = Pattern.compile("(\"(?:\\\\.|[^\"\\\\])*\"|'(\\\\.|[^'\\\\])')");
    private static final Pattern C_KEYWORDS = Pattern.compile("\\b(alignas|alignof|and|and_eq|asm|auto|bitand|bitor|bool|break|case|catch|char|char8_t|char16_t|char32_t|class|compl|concept|const|consteval|constexpr|constinit|const_cast|continue|co_await|co_return|co_yield|decltype|default|delete|do|double|dynamic_cast|else|enum|explicit|export|extern|false|float|for|friend|goto|if|inline|int|long|mutable|namespace|new|noexcept|not|not_eq|nullptr|operator|or|or_eq|private|protected|public|register|reinterpret_cast|requires|return|short|signed|sizeof|static|static_assert|static_cast|struct|switch|template|this|thread_local|throw|true|try|typedef|typeid|typename|union|unsigned|using|virtual|void|volatile|wchar_t|while|xor|xor_eq|async|await|fixed|unsafe|checked|unchecked|lock|delegate|event|get|set|init|record|var)\\b");
    private static final Pattern C_TYPES = Pattern.compile("\\b(std|cout|cin|cerr|endl|vector|string|map|set|pair|tuple|unique_ptr|shared_ptr|make_shared|make_unique|size_t|int8_t|int16_t|int32_t|int64_t|uint8_t|uint16_t|uint32_t|uint64_t)\\b");
    private static final Pattern C_FUNCTIONS = Pattern.compile("([a-zA-Z_]\\w*)\\s*(?=\\()");

    // Go
    private static final Pattern GO_KEYWORDS = Pattern.compile("\\b(break|case|chan|const|continue|default|defer|else|fallthrough|for|func|go|goto|if|import|interface|map|package|range|return|select|struct|switch|type|var)\\b");
    private static final Pattern GO_TYPES = Pattern.compile("\\b(append|cap|close|complex|copy|delete|imag|len|make|new|panic|print|println|real|recover|bool|byte|complex64|complex128|error|float32|float64|int|int8|int16|int32|int64|rune|string|uint|uint8|uint16|uint32|uint64|uintptr|true|false|iota|nil)\\b");

    // Rust
    private static final Pattern RUST_KEYWORDS = Pattern.compile("\\b(as|async|await|break|const|continue|crate|dyn|else|enum|extern|false|fn|for|if|impl|in|let|loop|match|mod|move|mut|pub|ref|return|self|Self|static|struct|super|trait|true|type|unsafe|use|where|while)\\b");
    private static final Pattern RUST_TYPES = Pattern.compile("\\b(i8|i16|i32|i64|i128|isize|u8|u16|u32|u64|u128|usize|f32|f64|bool|char|str|String|Vec|Option|Some|None|Result|Ok|Err|Box|Rc|Arc|println!|print!|eprintln!|format!|vec!|panic!)\\b");
    private static final Pattern RUST_ATTR = Pattern.compile("#\\[[\\s\\S]*?\\]");

    // JSON
    private static final Pattern JSON_KEYS = Pattern.compile("\"(?:\\\\.|[^\"\\\\])*\"(?=\\s*:)");
    private static final Pattern JSON_STRINGS = Pattern.compile("\"(?:\\\\.|[^\"\\\\])*\"");
    private static final Pattern JSON_CONSTANTS = Pattern.compile("\\b(true|false|null)\\b");
    private static final Pattern JSON_NUMBERS = Pattern.compile("-?\\b\\d+(\\.\\d+)?([eE][+-]?\\d+)?\\b");

    // HTML / XML
    private static final Pattern HTML_COMMENTS = Pattern.compile("<!--[\\s\\S]*?-->");
    private static final Pattern HTML_TAGS = Pattern.compile("</?[a-zA-Z0-9:-]+|/?>");
    private static final Pattern HTML_ATTR = Pattern.compile("\\b[a-zA-Z0-9:-]+(?=\\s*=)");
    private static final Pattern HTML_STRINGS = Pattern.compile("(\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*')");

    // SQL
    private static final Pattern SQL_KEYWORDS = Pattern.compile("(?i)\\b(SELECT|INSERT|UPDATE|DELETE|FROM|WHERE|AND|OR|NOT|IN|IS|NULL|LIKE|JOIN|INNER|LEFT|RIGHT|FULL|OUTER|ON|GROUP\\s+BY|ORDER\\s+BY|HAVING|LIMIT|OFFSET|CREATE|TABLE|DROP|ALTER|INDEX|PRIMARY\\s+KEY|FOREIGN\\s+KEY|REFERENCES|DEFAULT|CHECK|UNIQUE|VALUES|SET|AS|DISTINCT|UNION|ALL|EXISTS|BETWEEN|CASE|WHEN|THEN|ELSE|END|COUNT|SUM|AVG|MIN|MAX|DATABASE|VIEW|INTO)\\b");
    private static final Pattern SQL_COMMENTS = Pattern.compile("(--.*$|/\\*[\\s\\S]*?\\*/)", Pattern.MULTILINE);
    private static final Pattern SQL_STRINGS = Pattern.compile("'(?:''|[^'])*'");

    // Universal Fallback
    private static final Pattern UNIVERSAL_COMMENTS = Pattern.compile("(//.*$|#.*$|/\\*[\\s\\S]*?\\*/|<!--[\\s\\S]*?-->)", Pattern.MULTILINE);
    private static final Pattern UNIVERSAL_STRINGS = Pattern.compile("(`(?:\\\\.|[^`\\\\])*`|\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*')");
    private static final Pattern UNIVERSAL_KEYWORDS = Pattern.compile("\\b(def|function|fn|func|var|val|let|const|class|struct|interface|enum|import|export|from|return|if|else|elif|while|for|in|of|switch|case|break|continue|try|catch|except|finally|throw|raise|new|delete|public|private|protected|static|void|async|await|yield|package|namespace|use|as|type|null|nil|true|false|True|False|None)\\b");
    private static final Pattern UNIVERSAL_FUNCTIONS = Pattern.compile("([a-zA-Z_]\\w*)\\s*(?=\\()");

    public static class SyntaxTheme {
        public final int keywordColor;
        public final int stringColor;
        public final int numberColor;
        public final int commentColor;
        public final int typeColor;
        public final int functionColor;
        public final int constantColor;
        public final int decoratorColor;
        public final int jsonKeyColor;
        public final int tagColor;
        public final int attrColor;

        public SyntaxTheme(boolean isDark) {
            if (isDark) {
                // Dracula / One Dark Theme Colors
                keywordColor = 0xFFFF7B72;    // Coral Pink / Red
                stringColor = 0xFF7EE787;     // Vibrant Mint Green
                numberColor = 0xFF79C0FF;     // Light Sky Blue
                commentColor = 0xFF8B949E;    // Slate Grey (Muted)
                typeColor = 0xFFFFA657;       // Amber Orange
                functionColor = 0xFFD2A8FF;   // Lavender Purple
                constantColor = 0xFFFF7B72;   // Coral
                decoratorColor = 0xFFFFA657;  // Amber
                jsonKeyColor = 0xFF79C0FF;    // Sky Blue
                tagColor = 0xFF7EE787;        // Mint Green
                attrColor = 0xFFFFA657;       // Amber Orange
            } else {
                // GitHub Light Theme Colors
                keywordColor = 0xFFD73A49;    // Vivid Crimson
                stringColor = 0xFF22863A;     // Forest Green
                numberColor = 0xFF005CC5;     // Royal Blue
                commentColor = 0xFF6A737D;    // Slate Grey
                typeColor = 0xFFE36209;       // Burnt Orange
                functionColor = 0xFF6F42C1;   // Purple
                constantColor = 0xFF005CC5;   // Deep Blue
                decoratorColor = 0xFFB31D28;  // Dark Red
                jsonKeyColor = 0xFF005CC5;    // Royal Blue
                tagColor = 0xFF22863A;        // Forest Green
                attrColor = 0xFFE36209;       // Burnt Orange
            }
        }
    }

    public static void highlight(Spannable text, int start, int end, String language, boolean isDarkTheme) {
        if (text == null || start < 0 || end <= start || end > text.length()) {
            return;
        }

        try {
            CharSequence sub = text.subSequence(start, end);
            String code = sub.toString();
            String lang = detectLanguage(code, language);
            SyntaxTheme theme = new SyntaxTheme(isDarkTheme);

            switch (lang) {
                case "python":
                case "py":
                    applyPython(text, start, code, theme);
                    break;
                case "javascript":
                case "js":
                case "typescript":
                case "ts":
                case "jsx":
                case "tsx":
                case "vue":
                    applyJavaScript(text, start, code, theme);
                    break;
                case "java":
                case "kotlin":
                case "kt":
                    applyJava(text, start, code, theme);
                    break;
                case "c":
                case "cpp":
                case "c++":
                case "csharp":
                case "cs":
                case "h":
                case "hpp":
                    applyCpp(text, start, code, theme);
                    break;
                case "go":
                case "golang":
                    applyGo(text, start, code, theme);
                    break;
                case "rust":
                case "rs":
                    applyRust(text, start, code, theme);
                    break;
                case "json":
                    applyJson(text, start, code, theme);
                    break;
                case "html":
                case "xml":
                case "svg":
                    applyHtml(text, start, code, theme);
                    break;
                case "sql":
                    applySql(text, start, code, theme);
                    break;
                default:
                    applyUniversal(text, start, code, theme);
                    break;
            }
        } catch (Throwable ignored) {
        }
    }

    public static SpannableStringBuilder highlightJson(CharSequence jsonText, boolean isDarkTheme) {
        if (jsonText == null) {
            return new SpannableStringBuilder();
        }
        SpannableStringBuilder ssb = new SpannableStringBuilder(jsonText);
        try {
            SyntaxTheme theme = new SyntaxTheme(isDarkTheme);
            applyJson(ssb, 0, ssb.toString(), theme);
        } catch (Throwable ignored) {
        }
        return ssb;
    }

    private static String detectLanguage(String code, String explicitLang) {
        if (explicitLang != null && !explicitLang.trim().isEmpty()) {
            return explicitLang.trim().toLowerCase();
        }

        String trim = code.trim();
        if (trim.startsWith("<?php")) {
            return "php";
        } else if (trim.startsWith("<!DOCTYPE") || trim.startsWith("<html") || (trim.startsWith("<") && trim.endsWith(">"))) {
            return "html";
        } else if ((trim.startsWith("{") && trim.endsWith("}")) || (trim.startsWith("[") && trim.endsWith("]"))) {
            return "json";
        } else if (trim.startsWith("SELECT ") || trim.startsWith("INSERT ") || trim.startsWith("CREATE TABLE")) {
            return "sql";
        } else if (trim.contains("def ") || trim.contains("import ") && trim.contains("print(")) {
            return "python";
        } else if (trim.contains("const ") || trim.contains("let ") || trim.contains("function ") || trim.contains("console.log")) {
            return "javascript";
        } else if (trim.contains("public class ") || trim.contains("System.out.println")) {
            return "java";
        } else if (trim.contains("fun ") || trim.contains("val ")) {
            return "kotlin";
        } else if (trim.contains("#include <") || trim.contains("std::")) {
            return "cpp";
        } else if (trim.contains("fn ") && trim.contains("let mut ")) {
            return "rust";
        } else if (trim.contains("package ") && trim.contains("func ")) {
            return "go";
        }
        return "universal";
    }

    private static void applyPattern(Spannable text, int baseOffset, Pattern pattern, String code, int color, boolean italic, boolean bold) {
        Matcher m = pattern.matcher(code);
        while (m.find()) {
            int s = baseOffset + m.start();
            int e = baseOffset + m.end();
            text.setSpan(new ForegroundColorSpan(color), s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (italic) {
                text.setSpan(new StyleSpan(Typeface.ITALIC), s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (bold) {
                text.setSpan(new StyleSpan(Typeface.BOLD), s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    private static void applyPython(Spannable text, int offset, String code, SyntaxTheme theme) {
        applyPattern(text, offset, PY_FUNCTIONS, code, theme.functionColor, false, false);
        applyPattern(text, offset, PY_KEYWORDS, code, theme.keywordColor, false, true);
        applyPattern(text, offset, PY_BUILTINS, code, theme.typeColor, false, false);
        applyPattern(text, offset, PY_CONSTANTS, code, theme.constantColor, false, true);
        applyPattern(text, offset, PY_DECORATORS, code, theme.decoratorColor, false, false);
        applyPattern(text, offset, NUMBERS, code, theme.numberColor, false, false);
        applyPattern(text, offset, PY_STRINGS, code, theme.stringColor, false, false);
        applyPattern(text, offset, PY_COMMENTS, code, theme.commentColor, true, false);
    }

    private static void applyJavaScript(Spannable text, int offset, String code, SyntaxTheme theme) {
        applyPattern(text, offset, JS_FUNCTIONS, code, theme.functionColor, false, false);
        applyPattern(text, offset, JS_KEYWORDS, code, theme.keywordColor, false, true);
        applyPattern(text, offset, JS_BUILTINS, code, theme.typeColor, false, false);
        applyPattern(text, offset, NUMBERS, code, theme.numberColor, false, false);
        applyPattern(text, offset, JS_STRINGS, code, theme.stringColor, false, false);
        applyPattern(text, offset, JS_COMMENTS, code, theme.commentColor, true, false);
    }

    private static void applyJava(Spannable text, int offset, String code, SyntaxTheme theme) {
        applyPattern(text, offset, JAVA_FUNCTIONS, code, theme.functionColor, false, false);
        applyPattern(text, offset, JAVA_KEYWORDS, code, theme.keywordColor, false, true);
        applyPattern(text, offset, JAVA_TYPES, code, theme.typeColor, false, false);
        applyPattern(text, offset, JAVA_CONSTANTS, code, theme.constantColor, false, true);
        applyPattern(text, offset, JAVA_ANNOTATIONS, code, theme.decoratorColor, false, false);
        applyPattern(text, offset, NUMBERS, code, theme.numberColor, false, false);
        applyPattern(text, offset, JAVA_STRINGS, code, theme.stringColor, false, false);
        applyPattern(text, offset, JAVA_COMMENTS, code, theme.commentColor, true, false);
    }

    private static void applyCpp(Spannable text, int offset, String code, SyntaxTheme theme) {
        applyPattern(text, offset, C_PREPROCESSOR, code, theme.decoratorColor, false, false);
        applyPattern(text, offset, C_FUNCTIONS, code, theme.functionColor, false, false);
        applyPattern(text, offset, C_KEYWORDS, code, theme.keywordColor, false, true);
        applyPattern(text, offset, C_TYPES, code, theme.typeColor, false, false);
        applyPattern(text, offset, NUMBERS, code, theme.numberColor, false, false);
        applyPattern(text, offset, C_STRINGS, code, theme.stringColor, false, false);
        applyPattern(text, offset, C_COMMENTS, code, theme.commentColor, true, false);
    }

    private static void applyGo(Spannable text, int offset, String code, SyntaxTheme theme) {
        applyPattern(text, offset, GO_KEYWORDS, code, theme.keywordColor, false, true);
        applyPattern(text, offset, GO_TYPES, code, theme.typeColor, false, false);
        applyPattern(text, offset, NUMBERS, code, theme.numberColor, false, false);
        applyPattern(text, offset, JS_STRINGS, code, theme.stringColor, false, false);
        applyPattern(text, offset, JS_COMMENTS, code, theme.commentColor, true, false);
    }

    private static void applyRust(Spannable text, int offset, String code, SyntaxTheme theme) {
        applyPattern(text, offset, RUST_KEYWORDS, code, theme.keywordColor, false, true);
        applyPattern(text, offset, RUST_TYPES, code, theme.typeColor, false, false);
        applyPattern(text, offset, RUST_ATTR, code, theme.decoratorColor, false, false);
        applyPattern(text, offset, NUMBERS, code, theme.numberColor, false, false);
        applyPattern(text, offset, JS_STRINGS, code, theme.stringColor, false, false);
        applyPattern(text, offset, JS_COMMENTS, code, theme.commentColor, true, false);
    }

    private static void applyJson(Spannable text, int offset, String code, SyntaxTheme theme) {
        applyPattern(text, offset, JSON_STRINGS, code, theme.stringColor, false, false);
        applyPattern(text, offset, JSON_KEYS, code, theme.jsonKeyColor, false, true);
        applyPattern(text, offset, JSON_CONSTANTS, code, theme.constantColor, false, true);
        applyPattern(text, offset, JSON_NUMBERS, code, theme.numberColor, false, false);
    }

    private static void applyHtml(Spannable text, int offset, String code, SyntaxTheme theme) {
        applyPattern(text, offset, HTML_TAGS, code, theme.tagColor, false, true);
        applyPattern(text, offset, HTML_ATTR, code, theme.attrColor, false, false);
        applyPattern(text, offset, HTML_STRINGS, code, theme.stringColor, false, false);
        applyPattern(text, offset, HTML_COMMENTS, code, theme.commentColor, true, false);
    }

    private static void applySql(Spannable text, int offset, String code, SyntaxTheme theme) {
        applyPattern(text, offset, SQL_KEYWORDS, code, theme.keywordColor, false, true);
        applyPattern(text, offset, NUMBERS, code, theme.numberColor, false, false);
        applyPattern(text, offset, SQL_STRINGS, code, theme.stringColor, false, false);
        applyPattern(text, offset, SQL_COMMENTS, code, theme.commentColor, true, false);
    }

    private static void applyUniversal(Spannable text, int offset, String code, SyntaxTheme theme) {
        applyPattern(text, offset, UNIVERSAL_FUNCTIONS, code, theme.functionColor, false, false);
        applyPattern(text, offset, UNIVERSAL_KEYWORDS, code, theme.keywordColor, false, true);
        applyPattern(text, offset, NUMBERS, code, theme.numberColor, false, false);
        applyPattern(text, offset, UNIVERSAL_STRINGS, code, theme.stringColor, false, false);
        applyPattern(text, offset, UNIVERSAL_COMMENTS, code, theme.commentColor, true, false);
    }
}
