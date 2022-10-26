package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

// TODO: 实验三: 实现 IR 生成

/**
 *
 */
public class IRGenerator implements ActionObserver {
    private SymbolTable symbolTable;

    private List<Instruction> irList = new ArrayList<>();
    private Stack<IRValue> irValueStack = new Stack<>();

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        // TODO
        //变量
        if (currentToken.getKindId().equals("id")) {
            if (symbolTable.has(currentToken.getText())) {
                irValueStack.push(IRVariable.named(currentToken.getText()));
            }
        }
        //常量
        else if (currentToken.getKindId().equals("IntConst")) {
            irValueStack.push(IRImmediate.of(Integer.valueOf(currentToken.getText())));
        }
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        // TODO
        switch (production.index()) {
            //S -> D id
            case 4 -> {
                irValueStack.pop();
            }
            //S -> id = E
            case 6 -> {
                var value1 = irValueStack.pop();
                IRVariable value2 = (IRVariable) irValueStack.pop();
                irList.add(Instruction.createMov(value2, value1));
            }
            //S -> return E
            case 7 -> {
                irList.add(Instruction.createRet(irValueStack.pop()));
            }
            //E -> E + A
            case 8 -> {
                var tempValue = IRVariable.temp();
                var value1 = irValueStack.pop();
                var value2 = irValueStack.pop();
                irList.add(Instruction.createAdd(tempValue, value2, value1));
                irValueStack.push(tempValue);
            }
            //E -> E - A
            case 9 -> {
                var tempValue = IRVariable.temp();
                var value1 = irValueStack.pop();
                var value2 = irValueStack.pop();
                irList.add(Instruction.createSub(tempValue, value2, value1));
                irValueStack.push(tempValue);
            }
            //A -> A * B
            case 11 -> {
                var tempValue = IRVariable.temp();
                var value1 = irValueStack.pop();
                var value2 = irValueStack.pop();
                irList.add(Instruction.createMul(tempValue, value2, value1));
                irValueStack.push(tempValue);
            }
            default -> {}
        }
    }


    @Override
    public void whenAccept(Status currentStatus) {
        // TODO
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        // TODO
        this.symbolTable = table;
    }

    public List<Instruction> getIR() {
        // TODO
        return irList;
    }

    public void dumpIR(String path) {
        FileUtils.writeLines(path, getIR().stream().map(Instruction::toString).toList());
    }
}

