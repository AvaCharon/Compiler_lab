package cn.edu.hitsz.compiler.parser.table;

import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;

public class SemanticSymbol{
    Token token;
    SourceCodeType sourceCodeType;

    private SemanticSymbol(Token token, SourceCodeType sourceCodeType){
        this.token = token;
        this.sourceCodeType = sourceCodeType;
    }

    public SemanticSymbol(Token token){
        this.token = token;
        this.sourceCodeType = null;
    }

    public SemanticSymbol(SourceCodeType sourceCodeType){
        this.token = null;
        this.sourceCodeType = sourceCodeType;
    }

    public Token getToken() {
        if(isToken()){
            return token;
        }
        throw new RuntimeException("It is not a Token");
    }

    public SourceCodeType getSourceCodeType() {
        if(isSourceCodeType()){
            return sourceCodeType;
        }
        throw new RuntimeException("It is not a SourceCodeType");
    }

    public boolean isToken(){
        return this.token != null;
    }

    public boolean isSourceCodeType(){
        return this.sourceCodeType != null;
    }

}

