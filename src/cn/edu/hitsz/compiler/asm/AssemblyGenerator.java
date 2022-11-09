package cn.edu.hitsz.compiler.asm;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.*;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


/**
 * TODO: 实验四: 实现汇编生成
 * <br>
 * 在编译器的整体框架中, 代码生成可以称作后端, 而前面的所有工作都可称为前端.
 * <br>
 * 在前端完成的所有工作中, 都是与目标平台无关的, 而后端的工作为将前端生成的目标平台无关信息
 * 根据目标平台生成汇编代码. 前后端的分离有利于实现编译器面向不同平台生成汇编代码. 由于前后
 * 端分离的原因, 有可能前端生成的中间代码并不符合目标平台的汇编代码特点. 具体到本项目你可以
 * 尝试加入一个方法将中间代码调整为更接近 risc-v 汇编的形式, 这样会有利于汇编代码的生成.
 * <br>
 * 为保证实现上的自由, 框架中并未对后端提供基建, 在具体实现时可自行设计相关数据结构.
 *
 * @see AssemblyGenerator#run() 代码生成与寄存器分配
 */
public class AssemblyGenerator {
    List<Instruction> irList;
    List<Instruction> newIRList;
    List<String> asmList = new ArrayList<>();
    BMap<IRVariable,Integer> regMap = new BMap<IRVariable, Integer>();

    /**
     * 加载前端提供的中间代码
     * <br>
     * 视具体实现而定, 在加载中或加载后会生成一些在代码生成中会用到的信息. 如变量的引用
     * 信息. 这些信息可以通过简单的映射维护, 或者自行增加记录信息的数据结构.
     *
     * @param originInstructions 前端提供的中间代码
     */
    public void loadIR(List<Instruction> originInstructions) {
        // TODO: 读入前端提供的中间代码并生成所需要的信息
        this.irList = originInstructions;
    }

    /**
     * 预处理
     * 使中间代码中的二元操作数指令中不存在左立即数
     */
    public void preTreat(){
        int index = 0;
        Instruction instruction;
        newIRList = new LinkedList<>();
        while (true){
            instruction = irList.get(index);
            if(instruction.getKind().isBinary()){
                var LHS = instruction.getLHS();
                var RHS = instruction.getRHS();
                if(LHS.isImmediate()&&RHS.isImmediate()){
                    int ret = 0;
                    IRImmediate ILHS = (IRImmediate)LHS;
                    IRImmediate IRHS = (IRImmediate)RHS;
                    switch (instruction.getKind()){
                        case MUL -> {
                            ret = ILHS.getValue() * IRHS.getValue();
                        }
                        case ADD -> {
                            ret = ILHS.getValue() + IRHS.getValue();
                        }
                        case SUB -> {
                            ret = ILHS.getValue() - IRHS.getValue();
                        }
                    }
                    newIRList.add(Instruction.createMov(instruction.getResult(), IRImmediate.of(ret)));
                }else if(LHS.isImmediate()){
                    switch (instruction.getKind()){
                        case ADD -> {
                            newIRList.add(Instruction.createAdd(instruction.getResult(),RHS,LHS));
                        }
                        case MUL -> {
                            IRVariable t = IRVariable.temp();
                            newIRList.add(Instruction.createMov(t,(IRImmediate)LHS));
                            newIRList.add(Instruction.createMul(instruction.getResult(),t,RHS));
                        }
                        case SUB -> {
                            IRVariable t = IRVariable.temp();
                            newIRList.add(Instruction.createMov(t,(IRImmediate)LHS));
                            newIRList.add(Instruction.createSub(instruction.getResult(),t,RHS));
                        }
                    }
                }else if(RHS.isImmediate()&&instruction.getKind().equals(InstructionKind.MUL)){
                    IRVariable t = IRVariable.temp();
                    newIRList.add(Instruction.createMov(t,(IRImmediate)RHS));
                    newIRList.add(Instruction.createMul(instruction.getResult(),LHS,t));
                }else {
                    newIRList.add(instruction);
                }
            }else if(instruction.getKind().isReturn()) {
                newIRList.add(instruction);
                return;
            }else {
                newIRList.add(instruction);
            }
            index++;
        }
    }

    /**
     * 不完备的寄存器分配
     * @param irVariable 要为之分配寄存器的值
     * @return  可用的寄存器号
     */
    private int findReg(IRVariable irVariable) {
        if (regMap.containsKey(irVariable)) {
            for (int i = 0; i <= 6; i++) {
                if (regMap.containsValue(i) && regMap.getByValue(i).equals(irVariable)) {
                    return i;
                }
            }
        } else {
            for (int i = 0; i <= 6; i++){
                if (!regMap.containsValue(i)) {
                    regMap.replace(irVariable, i);
                    return i;
                }
            }
        }
        throw new RuntimeException("No empty reg");
    }
    /**
     * 执行代码生成.
     * <br>
     * 根据理论课的做法, 在代码生成时同时完成寄存器分配的工作. 若你觉得这样的做法不好,
     * 也可以将寄存器分配和代码生成分开进行.
     * <br>
     * 提示: 寄存器分配中需要的信息较多, 关于全局的与代码生成过程无关的信息建议在代码生
     * 成前完成建立, 与代码生成的过程相关的信息可自行设计数据结构进行记录并动态维护.
     */
    public void run() {
        // TODO: 执行寄存器分配与代码生成
        preTreat();
        int index = 0;
        int reg,LHS,RHS;
        while (true){
            Instruction instruction = newIRList.get(index);
            switch (instruction.getKind()){
                case MOV ->{
                    if (instruction.getFrom().isImmediate()){
                        reg = findReg(instruction.getResult());
                        asmList.add("li t" + reg + ", " + ((IRImmediate)instruction.getFrom()).getValue());
                    }else {
                        LHS = findReg((IRVariable)instruction.getFrom());
                        reg = findReg(instruction.getResult());
                        asmList.add("mv t" + reg + ", t" + LHS);

                        if (((IRVariable) instruction.getFrom()).isTemp()){
                            regMap.removeByValue(LHS);
                        }
                    }
                }
                case ADD -> {
                    reg = findReg(instruction.getResult());
                    LHS = findReg((IRVariable)instruction.getLHS());
                    if(instruction.getRHS().isImmediate()){
                        asmList.add("addi t" + reg + ", t" + LHS + ", " + Integer.toString(((IRImmediate)instruction.getRHS()).getValue()));

                        if (((IRVariable) instruction.getLHS()).isTemp()) {
                            regMap.removeByValue(LHS);
                        }
                    }else {
                        RHS = findReg((IRVariable)instruction.getRHS());
                        asmList.add("add t" + reg + ", t" + LHS + ", t" + RHS);

                        if (((IRVariable) instruction.getLHS()).isTemp()) {
                            regMap.removeByValue(LHS);
                        }
                        if (((IRVariable) instruction.getRHS()).isTemp()) {
                            regMap.removeByValue(RHS);
                        }
                    }
                }
                case SUB -> {
                    reg = findReg(instruction.getResult());
                    LHS = findReg((IRVariable)instruction.getLHS());
                    if(instruction.getRHS().isImmediate()){
                        asmList.add("subi t" + reg + ", t" + LHS + ", " + Integer.toString(((IRImmediate)instruction.getRHS()).getValue()));

                        if (((IRVariable) instruction.getLHS()).isTemp()) {
                            regMap.removeByValue(LHS);
                        }
                    }else {
                        RHS = findReg((IRVariable)instruction.getRHS());
                        asmList.add("sub t" + reg + ", t" + LHS + ", t" + RHS);

                        if (((IRVariable) instruction.getLHS()).isTemp()) {
                            regMap.removeByValue(LHS);
                        }
                        if (((IRVariable) instruction.getRHS()).isTemp()) {
                            regMap.removeByValue(RHS);
                        }
                    }
                }
                case MUL -> {
                    reg = findReg(instruction.getResult());
                    LHS = findReg((IRVariable)instruction.getLHS());
                    RHS = findReg((IRVariable)instruction.getRHS());
                    asmList.add("mul t" + reg + ", t" + LHS + ", t" + RHS);
                    if (((IRVariable) instruction.getLHS()).isTemp()) {
                        regMap.removeByValue(LHS);
                    }
                    if (((IRVariable) instruction.getRHS()).isTemp()) {
                        regMap.removeByValue(RHS);
                    }
                }
                case RET -> {
                    reg = findReg((IRVariable)instruction.getReturnValue());
                    asmList.add("mv a0, t"+reg);
                    return;
                }
            }
            index++;
        }
    }


    /**
     * 输出汇编代码到文件
     *
     * @param path 输出文件路径
     */
    public void dump(String path) {
        // TODO: 输出汇编代码到文件
        FileUtils.writeLines(path, asmList);
    }
}

