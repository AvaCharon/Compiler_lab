package cn.edu.hitsz.compiler.lexer;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.stream.StreamSupport;

/**
 * TODO: 实验一: 实现词法分析
 * <br>
 * 你可能需要参考的框架代码如下:
 *
 * @see Token 词法单元的实现
 * @see TokenKind 词法单元类型的实现
 */
public class LexicalAnalyzer {
    private final SymbolTable symbolTable;

    private static StringBuffer inStrings = new StringBuffer();

    public LinkedList<Token> tokens = new LinkedList<>();

    public static String[] retainWord = {"int","return"};
    public static char[] operator = {'+','-','*','/','(',')','=',',',';'};

    public LexicalAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }


    /**
     * 从给予的路径中读取并加载文件内容
     *
     * @param path 路径
     */
    public void loadFile(String path) {
        // TODO: 词法分析前的缓冲区实现
        // 可自由实现各类缓冲区
        // 或直接采用完整读入方法
        BufferedReader bufferedReader = null;
        try{
            bufferedReader = new BufferedReader(new FileReader(path));
            String line;
            while((line=bufferedReader.readLine())!=null)
            {
                inStrings.append(line);
            }
        }catch (IOException e){
            e.printStackTrace();
        }finally {
            try {
                if(bufferedReader!=null){
                    bufferedReader.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    /**
     * 执行词法分析, 准备好用于返回的 token 列表 <br>
     * 需要维护实验一所需的符号表条目, 而得在语法分析中才能确定的符号表条目的成员可以先设置为 null
     */
    public void run() {
        // TODO: 自动机实现的词法分析过程
        StringBuffer strToken = new StringBuffer();
        char ch ;
        for(int i=0;i<inStrings.length();i++){
            ch = inStrings.charAt(i);
            if(isLetter(ch)){
                strToken.append(ch);
                i++;
                for(;i<inStrings.length();i++){
                    ch = inStrings.charAt(i);
                    if(isLetter(ch)||isDigit(ch)){
                        strToken.append(ch);
                    }else{
                        Token token = switch (isRetain(strToken)) {
                            case 0 -> {
                                symbolTable.add(strToken.toString());
                                yield Token.normal("id", strToken.toString());
                            }
                            case 1 -> Token.simple("int");
                            case 2 -> Token.simple("return");
                            default -> {
                                symbolTable.add(strToken.toString());
                                yield Token.normal("id", strToken.toString());
                            }
                        };
                        tokens.add(token);
                        strToken.delete(0,strToken.length());
                        break;
                    }
                }
            }
            if(isDigit(ch)){
                strToken.append(ch);
                i++;
                for(;i<inStrings.length();i++){
                    ch = inStrings.charAt(i);
                    if(isDigit(ch)){
                        strToken.append(ch);
                    }else{
                        Token token = Token.normal("IntConst",strToken.toString());
                        tokens.add(token);
                        strToken.delete(0,strToken.length());
                        break;
                    }
                }
            }
            if(isOperator(ch)){
                Token token = switch (ch) {
                    case '+' -> Token.simple("+");
                    case '-' -> Token.simple("-");
                    case '*' -> Token.simple("*");
                    case '/' -> Token.simple("/");
                    case '=' -> Token.simple("=");
                    case '(' -> Token.simple("(");
                    case ')' -> Token.simple(")");
                    case ';' -> Token.simple("Semicolon");
                    default -> throw new RuntimeException("非法字符");
                };
                tokens.add(token);
            }
        }
        tokens.add(Token.eof());

    }

    //判断是否是字母
    public boolean isLetter(char ch){
        if((ch>='a' && ch<='z') || (ch>='A' && ch<='Z')){
            return true;
        }
        return false;
    }

    //判断是否是数字
    public boolean isDigit(char ch){
        if(ch>='0' && ch <= '9'){
            return true;
        }
        return false;
    }

    //判断是否是符号
    public boolean isOperator(char ch){
        for(char str : operator){
            if(str == ch){
                return true;
            }
        }
        return false;
    }

    //判断单词是否为保留字
    public int isRetain(StringBuffer str){
        if("int".equals(str.toString())){
            return 1;
        }
        else if("return".equals(str.toString())){
            return 2;
        }
        return 0;
    }

    /**
     * 获得词法分析的结果, 保证在调用了 run 方法之后调用
     *
     * @return Token 列表
     */
    public Iterable<Token> getTokens() {
        // TODO: 从词法分析过程中获取 Token 列表
        // 词法分析过程可以使用 Stream 或 Iterator 实现按需分析
        // 亦可以直接分析完整个文件
        // 总之实现过程能转化为一列表即可
        return tokens;
    }

    public void dumpTokens(String path) {
        FileUtils.writeLines(
            path,
            StreamSupport.stream(getTokens().spliterator(), false).map(Token::toString).toList()
        );
    }


}
