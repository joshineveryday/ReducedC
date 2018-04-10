//IMPORTANT TODOS:
//TODO: FIX YUNO TEST CASES FOR PROJECT ONE, SOME STUFF BROKE
//TODO: Fix Double Appending Problem. Clear the rc.s file if it exists
//TODO: Make Arrows for structs work

import java.io.*;
import java.util.Date;
import java.util.Vector;
import java.util.Stack;

public class AssemblyCodeGenerator {
    private int indent_level = 0;
    private int num_floats = 0;
    private int num_strs = 0;
    private int num_cmp = 0;
    private int num_if  = 0;
    private int num_loops = 0;
    private int num_andOrSkip = 0;
    private int num_ctordtor = 0;
    private int num_dtor_locals = 0;
    public boolean in_struct = false;
    public boolean in_func = false;
    private Stack<Integer> ifLables = new Stack<Integer>();
    private Stack<Integer> loopLabels = new Stack<Integer>();
    public boolean arrayDeref = false;
    private Stack<STO> dTorSTOStack = new Stack<STO> ();
    private Stack<Integer> dTorIDStack = new Stack<Integer> ();

    //Buffer Queue for global variable operations
    //If binary operator, remove two elements from operand buffer
    //If unary operator, remove one element
    public Vector<STO> operandBuffer = new Vector<STO>();
    public Vector<Operator> operatorBuffer = new Vector<Operator>();
    public Vector<STO> opResultBuffer = new Vector<STO>();

    public String funcLabel = ""; //The label for the function being declared or invoked

    static public int frameptr_offset = 0;
    static private int gbl_offset = 0;

    private static final String ERROR_IO_CLOSE = 
        "Unable to close outFile";
    private static final String ERROR_IO_CONSTRUCT = 
        "Unable to construct FileWriter for file %s";
    private static final String ERROR_IO_WRITE = 
        "Unable to write to outFile";
    private FileWriter outFile;
    
    private static final String FILE_HEADER = 
        "/*\n" +
        " * Generated %s\n" + 
        " */\n\n";
        
    private static final String SEPARATOR = "\t";
    private static final String NEWLINE = "\n";

    //Define Assembly Statements
    private static final String SECTION_STMT = ".section";
    private static final String ALIGN_STMT = ".align  ";
    private static final String GLOBAL_STMT = ".global ";
    private static final String SKIP_STMT = ".skip   ";
    private static final String SINGLE_STMT = ".single ";
    private static final String WORD_STMT = ".word   ";
    private static final String NOP_STMT = "nop";
    private static final String RET_STMT = "ret";
    private static final String RESTORE_STMT = "restore";
    private static final String Float_SMT = ".$$.float.";
    private static final String STR_SMT = ".$$.str.";
    private static final String ASCI_SMT = ".asciz";
    
    //Define Assembly Operations 
    private static final String SET_OP = "set     ";
    private static final String SAVE_OP = "save    ";
    private static final String CALL_OP = "call    ";
    private static final String LD_OP = "ld      ";
    private static final String ST_OP = "st      ";
    private static final String ADD_OP = "add     ";
    private static final String MUL_OP = ".mul";
    private static final String FADD_OP = "fadds   ";
    private static final String SUB_OP = "sub     ";
    private static final String FSUB_OP = "fsubs   ";
    private static final String FMUL_OP = "fmuls   ";
    private static final String FDIV_OP = "fdivs   ";
    private static final String MOV_OP = "mov     ";
    private static final String FMOV_OP = "fmovs   ";
    private static final String AND_OP = "and     ";
    private static final String OR_OP = "or      ";
    private static final String XOR_OP = "xor     ";
    private static final String NEG_OP = "neg     ";
    private static final String FNEG_OP = "fnegs   ";
    private static final String CMP_OP = "cmp     ";
    private static final String FCMP_OP = "fcmps   ";
    private static final String BE_OP = "be      ";
    private static final String BNE_OP = "bne     ";
    private static final String BLE_OP = "ble     ";
    private static final String BL_OP = "bl      ";
    private static final String BGE_OP = "bge      ";
    private static final String BG_OP = "bg      ";
    private static final String FBLE_OP = "fble     ";
    private static final String FBL_OP = "fbl      ";
    private static final String FBGE_OP = "fbge      ";
    private static final String FBG_OP = "fbg      ";
    private static final String FBE_OP = "fbe     ";
    private static final String FBNE_OP = "fbne     ";
    private static final String INC_OP = "inc     ";
    private static final String BA_OP = "ba      ";
    private static final String FITOS_OP = "fitos   ";
    private static final String FSTOI_OP = "fstoi   ";
    //Define Assembly Templates
    private static final String LABEL = "%s:" + NEWLINE;
    private static final String THREE_PARAM = "%s" + SEPARATOR + "%s, %s, %s" + NEWLINE;
    private static final String TWO_PARAM = "%s" + SEPARATOR + "%s, %s" + NEWLINE;
    private static final String ONE_PARAM = "%s" + SEPARATOR + "%s" + NEWLINE;
    private static final String COMMENT = "! %s" + NEWLINE;
    private static final String NO_PARAM = "%s" + NEWLINE;
    private static final String ASSIGNMENT = "%s = %s" + NEWLINE;
   
    public AssemblyCodeGenerator(String fileToWrite) {
        try {
            outFile = new FileWriter(fileToWrite, false);
            outFile.write("");
            outFile.close();

            outFile = new FileWriter(fileToWrite, true);
            
            writeAssembly(FILE_HEADER, (new Date()).toString());
        } catch (IOException e) {
            System.err.printf(ERROR_IO_CONSTRUCT, fileToWrite);
            e.printStackTrace();
            System.exit(1);
        }
    }

    public Stack getIfLables(){
        return ifLables;
    }
    
    public void decreaseIndent() {
        indent_level--;
    }

    public void resetIndent() {
        indent_level = 0;
    }
    
    public void dispose() {
        try {
            outFile.close();
        } catch (IOException e) {
            System.err.println(ERROR_IO_CLOSE);
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    public void increaseIndent() {
        indent_level++;
    }
    
    public void writeAssembly(String template, String ... params) {
        StringBuilder asStmt = new StringBuilder();
    
        for (int i=0; i < indent_level; i++) {
            asStmt.append(SEPARATOR);
        }
        
        asStmt.append(String.format(template, (Object[])params));
        
        try {
            outFile.write(asStmt.toString());
            outFile.flush();
        } catch (IOException e) {
            System.err.println(ERROR_IO_WRITE);
            e.printStackTrace();
        }
    }

    //Appends contents of a file to rc.s
    public void writeAssemblyFromFile(String inFilename) {
         try {
            FileReader asmFileReader = new FileReader(inFilename);
            BufferedReader asmBufReader = new BufferedReader(asmFileReader);
            String asmLine = "";
            while((asmLine = asmBufReader.readLine()) != null) { 
                try {
                    outFile.write(asmLine + "\n");
                    outFile.flush();
                } catch (IOException e) {
                    System.err.println(ERROR_IO_WRITE);
                    e.printStackTrace();
                }
            } 

            asmFileReader.close(); 
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }  

    //----------------------------------------------------------------
    // 
    //----------------------------------------------------------------
    public void asmAddFieldVar(STO fieldVar) {
        fieldVar.setBaseAddr("%fp");
        setStructOffset(fieldVar);
    }

    //----------------------------------------------------------------
    // handles unititialized variables
    //----------------------------------------------------------------
    //TODO handle assignment to functions
    public void asmVarDecl(String optStatic, STO var, STO expr, boolean isGlobal) {
        Type varType = var.getType();

        if(isGlobal) { 
            //Store in bss or data, use %g0 and label for offset
            var.setBaseAddr("%g0");
            var.setOffsetAddr(var.getName()); //the label of the global var

            writeAssembly(NEWLINE);
            increaseIndent();
            if(expr instanceof ConstSTO) {
                //Set the the value and store in data section
                writeAssembly(ONE_PARAM, SECTION_STMT, "\".data\"");
                writeAssembly(ONE_PARAM, ALIGN_STMT, "4");
                if(optStatic == null) writeAssembly(ONE_PARAM, GLOBAL_STMT, var.getName());
                decreaseIndent();
                writeAssembly(LABEL, var.getName());
                increaseIndent();

                ConstSTO constExpr = (ConstSTO) expr;
                if(varType instanceof FloatType) {
                    writeAssembly(ONE_PARAM, SINGLE_STMT, "0r" + Float.toString(constExpr.getFloatValue()));
                } else if(varType instanceof IntType) {
                    writeAssembly(ONE_PARAM, WORD_STMT, Integer.toString(constExpr.getIntValue()));
                } else if (varType instanceof BoolType) {
                    writeAssembly(ONE_PARAM, WORD_STMT, Integer.toString(constExpr.getIntValue()));
                }
            
                writeAssembly(NEWLINE);
                writeAssembly(ONE_PARAM, SECTION_STMT, "\".text\"");
              
                writeAssembly(ONE_PARAM, ALIGN_STMT, "4");
                decreaseIndent();

            } else {
                resetIndent();
                increaseIndent();
                //Store the var in bss section
                writeAssembly(ONE_PARAM, SECTION_STMT, "\".bss\"");
                writeAssembly(ONE_PARAM, ALIGN_STMT, "4");
                if(optStatic == null) writeAssembly(ONE_PARAM, GLOBAL_STMT, var.getName());
                decreaseIndent();
                writeAssembly(LABEL, var.getName());
                increaseIndent();
                writeAssembly(ONE_PARAM, SKIP_STMT, Integer.toString(varType.getSize()));

                writeAssembly(NEWLINE);
                writeAssembly(ONE_PARAM, SECTION_STMT, "\".text\"");
                
                writeAssembly(ONE_PARAM, ALIGN_STMT, "4");
                decreaseIndent();

                String gblInitLbl = ".$.init." + var.getName();

                if(varType instanceof StructType || 
                  (varType instanceof ArrayType 
                            && ((ArrayType) varType).getBase() instanceof StructType)) {
                    resetIndent();
                    writeAssembly(LABEL, gblInitLbl);
                    increaseIndent();
                    //Initialize the variable for the constructor
                    writeAssembly(TWO_PARAM, SET_OP, "SAVE." + gblInitLbl, "%g1");
                    writeAssembly(THREE_PARAM, SAVE_OP, "%sp", "%g1", "%sp");

                    increaseIndent();

                    writeAssembly(NEWLINE);
                } else if(expr != null && expr.getType() != null && expr.getName() != null) {
                    asmFuncDeclBody(gblInitLbl);
                    increaseIndent();
                    //TODO: IS there a better way than flushing?
                    flushAsmOperation();

                    asmAssignExpr(var, expr);
                    asmFuncEnd(gblInitLbl, expr);

                    //Initialize the global variable
                    increaseIndent();
                    writeAssembly(NEWLINE);
                    writeAssembly(ONE_PARAM, SECTION_STMT, "\".init\"");
                    writeAssembly(ONE_PARAM, ALIGN_STMT, "4");
                    writeAssembly(ONE_PARAM, CALL_OP, gblInitLbl);
                    writeAssembly(NO_PARAM, NOP_STMT);
                    writeAssembly(NEWLINE);
                    writeAssembly(ONE_PARAM, SECTION_STMT, "\".text\"");
                    writeAssembly(ONE_PARAM, ALIGN_STMT, "4");
                    resetIndent();
                }
            } 
        } else {
            //Write Assembly for local var

            //TODO: Ask TA how function assignment works. What is the -32?
            Type typ = var.getType();
            if(optStatic ==null) {
                setFpOffset(var);
                var.setBaseAddr("%fp");
            }
            if(expr != null && expr.getName() != null && optStatic ==null) {
                //Set the ConstExpr if it exists
                ConstSTO constExpr = null;
                if(expr instanceof ConstSTO) constExpr = (ConstSTO) expr;

                if (typ instanceof FloatType){
                    if(constExpr != null) {
                        String asmFloatVal = Float.toString(constExpr.getFloatValue());
                        num_floats++;
                        String floatLbl =  ".$$.float." + Integer.toString(num_floats);
                       
                        writeAssembly(NEWLINE);
                        writeAssembly(COMMENT, var.getName() + " = " + asmFloatVal);
                   
                        //Run set and add Ops
                        writeAssembly(TWO_PARAM, SET_OP, Integer.toString(frameptr_offset), "%o1");
                        writeAssembly(THREE_PARAM, ADD_OP, "%fp", "%o1", "%o1");

                        writeAssembly(NEWLINE);
                        writeAssembly(ONE_PARAM, SECTION_STMT, "\".rodata\"");
                      
                        writeAssembly(ONE_PARAM, ALIGN_STMT, "4");
                        decreaseIndent();
                        writeAssembly(LABEL,floatLbl);
                        increaseIndent();
                        writeAssembly(ONE_PARAM, SINGLE_STMT, "0r" + asmFloatVal);
                        writeAssembly(NEWLINE);
                        writeAssembly(ONE_PARAM, SECTION_STMT, "\".text\"");
                   
                        writeAssembly(ONE_PARAM, ALIGN_STMT, "4");
                        writeAssembly(TWO_PARAM, SET_OP, floatLbl, "%l7");
                    } else {
                        String exprName = expr.getName();                    
                        writeAssembly(NEWLINE);
                        writeAssembly(COMMENT, var.getName() + " = " + exprName);
        
                        writeAssembly(TWO_PARAM, SET_OP, Integer.toString(frameptr_offset), "%o1");
                        writeAssembly(THREE_PARAM, ADD_OP, "%fp", "%o1", "%o1");
                        String exprOffsetAddr = expr.getOffsetAddr();
                        writeAssembly(TWO_PARAM, SET_OP, exprOffsetAddr, "%l7");
                        writeAssembly(THREE_PARAM, ADD_OP, expr.getBaseAddr(), "%l7", "%l7");
                    } 

                    //Load and Store The Float
                    if(expr.isRef() || expr.isDotted() || expr.isArrayDeref()) writeAssembly(TWO_PARAM, LD_OP, "[%l7]", "%l7");
                    writeAssembly(TWO_PARAM, LD_OP, "[%l7]", "%f0");
                    writeAssembly(TWO_PARAM, ST_OP, "%f0", "[%o1]");
                }
                if(typ instanceof IntType){
                    if(constExpr != null) {
                        String intVal = Integer.toString(constExpr.getIntValue());
                        writeAssembly(NEWLINE);
                        writeAssembly(COMMENT, var.getName() + " = " + intVal);
               
                        writeAssembly(TWO_PARAM, SET_OP, Integer.toString(frameptr_offset),"%o1");
                        writeAssembly(THREE_PARAM, ADD_OP, "%fp", "%o1", "%o1");
                        writeAssembly(TWO_PARAM, SET_OP, intVal,"%o0");
                        writeAssembly(TWO_PARAM, ST_OP, "%o0", "[%o1]");

                    } else {
                        String exprName = expr.getName();                    
                        writeAssembly(NEWLINE);
                        writeAssembly(COMMENT, var.getName() + " = " + exprName);
                    
                        writeAssembly(TWO_PARAM, SET_OP, Integer.toString(frameptr_offset), "%o1");
                        writeAssembly(THREE_PARAM, ADD_OP, "%fp", "%o1", "%o1");
                        String exprOffsetAddr = expr.getOffsetAddr();
                        writeAssembly(TWO_PARAM, SET_OP, exprOffsetAddr, "%l7");
                        writeAssembly(THREE_PARAM, ADD_OP, expr.getBaseAddr(), "%l7", "%l7");
                        if(expr.isRef() || expr.isDotted() || expr.isArrayDeref()) writeAssembly(TWO_PARAM, LD_OP, "[%l7]", "%l7");
                        writeAssembly(TWO_PARAM, LD_OP, "[%l7]", "%o0");
                        writeAssembly(TWO_PARAM,ST_OP, "%o0", "[%o1]");       
                    } 
                }
                if(typ instanceof BoolType){
                    if(constExpr != null) {
                        boolean val = constExpr.getBoolValue();
                        String boolVal = val? "1":"0";
                        writeAssembly(NEWLINE);
                        writeAssembly(COMMENT, var.getName() + " = " + Boolean.toString(val));
                       
                        writeAssembly(TWO_PARAM, SET_OP, Integer.toString(frameptr_offset),"%o1");
                        writeAssembly(THREE_PARAM, ADD_OP, "%fp", "%o1", "%o1");
                        writeAssembly(TWO_PARAM, SET_OP, boolVal,"%o0");
                        writeAssembly(TWO_PARAM, ST_OP, "%o0", "[%o1]");
                    } else {
                        String exprName = expr.getName();
                        writeAssembly(NEWLINE);
                        writeAssembly(COMMENT, var.getName() + " = " + exprName);
                    
                        writeAssembly(TWO_PARAM, SET_OP, Integer.toString(frameptr_offset), "%o1");
                        writeAssembly(THREE_PARAM, ADD_OP, "%fp", "%o1", "%o1");
                        String exprOffsetAddr = expr.getOffsetAddr();
                        writeAssembly(TWO_PARAM, SET_OP, exprOffsetAddr, "%l7");
                        writeAssembly(THREE_PARAM, ADD_OP, expr.getBaseAddr(), "%l7", "%l7");
                        if(expr.isRef() || expr.isDotted() || expr.isArrayDeref()) writeAssembly(TWO_PARAM, LD_OP, "[%l7]", "%l7");
                        writeAssembly(TWO_PARAM, LD_OP, "[%l7]", "%o0");
                        writeAssembly(TWO_PARAM,ST_OP, "%o0", "[%o1]");
                        
                    } 
                }

                if(typ instanceof PointerType){
                    String exprName = expr.getName();
                    writeAssembly(NEWLINE);
                    writeAssembly(COMMENT, var.getName() + " = " + exprName);

                    writeAssembly(TWO_PARAM, SET_OP, Integer.toString(frameptr_offset), "%o1");
                    writeAssembly(THREE_PARAM, ADD_OP, "%fp", "%o1", "%o1");
                    String exprOffsetAddr = expr.getOffsetAddr();
                    writeAssembly(TWO_PARAM, SET_OP, exprOffsetAddr, "%l7");
                    writeAssembly(THREE_PARAM, ADD_OP, expr.getBaseAddr(), "%l7", "%l7");
                    writeAssembly(TWO_PARAM, LD_OP, "[%l7]", "%o0");
                    writeAssembly(TWO_PARAM,ST_OP, "%o0", "[%o1]");
                }
            }

            //local static vars
            if(optStatic !=null){
                var.setBaseAddr("%g0");
                var.setOffsetAddr(funcLabel + "." + var.getName());
                //non init local static vars
                if(expr == null || expr.getName()==null){
                    //Store the var in bss section
                    writeAssembly(NEWLINE);
                    writeAssembly(ONE_PARAM, SECTION_STMT, "\".bss\"");
                    writeAssembly(ONE_PARAM, ALIGN_STMT, "4");
                    decreaseIndent();
                    writeAssembly(LABEL, funcLabel + "." + var.getName());
                    increaseIndent();
                    writeAssembly(ONE_PARAM, SKIP_STMT, Integer.toString(varType.getSize()));

                    writeAssembly(NEWLINE);
                    writeAssembly(ONE_PARAM, SECTION_STMT, "\".text\"");

                    writeAssembly(ONE_PARAM, ALIGN_STMT, "4");
                }
                // initialized local static vars
                else{
                    if(expr instanceof ConstSTO) {
                        //Set the the value and store in data section
                        writeAssembly(NEWLINE);
                        writeAssembly(ONE_PARAM, SECTION_STMT, "\".data\"");
                        writeAssembly(ONE_PARAM, ALIGN_STMT, "4");
                        decreaseIndent();
                        writeAssembly(LABEL, funcLabel+"."+var.getName());
                        increaseIndent();

                        ConstSTO constExpr = (ConstSTO) expr;
                        if(varType instanceof FloatType) {
                            writeAssembly(ONE_PARAM, SINGLE_STMT, "0r" + Float.toString(constExpr.getFloatValue()));
                        } else if(varType instanceof IntType) {
                            writeAssembly(ONE_PARAM, WORD_STMT, Integer.toString(constExpr.getIntValue()));
                        } else if (varType instanceof BoolType) {
                            writeAssembly(ONE_PARAM, WORD_STMT, Integer.toString(constExpr.getIntValue()));
                        }

                        writeAssembly(NEWLINE);
                        writeAssembly(ONE_PARAM, SECTION_STMT, "\".text\"");

                        writeAssembly(ONE_PARAM, ALIGN_STMT, "4");

                    }
                    //expr is not constant
                    else{
                        String initLabel = ".$.init.main.void."+var.getName();
                        writeAssembly(NEWLINE);
                        writeAssembly(ONE_PARAM, SECTION_STMT, "\".bss\"");
                        writeAssembly(ONE_PARAM, ALIGN_STMT, "4");
                        decreaseIndent();
                        writeAssembly(LABEL, funcLabel + "." + var.getName());
                        increaseIndent();
                        writeAssembly(ONE_PARAM, SKIP_STMT, Integer.toString(varType.getSize()));
                        writeAssembly(NEWLINE);
                        writeAssembly(ONE_PARAM, SECTION_STMT, "\".text\"");
                        writeAssembly(ONE_PARAM, ALIGN_STMT, "4");
                        writeAssembly(NEWLINE);
                        writeAssembly(ONE_PARAM, SECTION_STMT, "\".bss\"");
                        writeAssembly(ONE_PARAM, ALIGN_STMT, "4");
                        decreaseIndent();
                        writeAssembly(LABEL, initLabel);
                        increaseIndent();
                        writeAssembly(ONE_PARAM, SKIP_STMT, Integer.toString(varType.getSize()));
                        writeAssembly(NEWLINE);
                        writeAssembly(ONE_PARAM, SECTION_STMT, "\".text\"");
                        writeAssembly(ONE_PARAM, ALIGN_STMT, "4");
                        writeAssembly(NEWLINE);
                        writeAssembly(COMMENT, "Start init guard");
                        writeAssembly(TWO_PARAM, SET_OP, initLabel, "%o0");
                        writeAssembly(TWO_PARAM, LD_OP,"[%o0]", "%o0" );
                        writeAssembly(TWO_PARAM, CMP_OP, "%o0", "%g0");
                        writeAssembly(ONE_PARAM, BNE_OP, initLabel+".done");
                        writeAssembly(NO_PARAM, NOP_STMT);
                        writeAssembly(NEWLINE);
                        writeAssembly(COMMENT, var.getName() + " = " + expr.getName());
                        writeAssembly(TWO_PARAM, SET_OP, var.getOffsetAddr(), "%o1");
                        writeAssembly(THREE_PARAM, ADD_OP, var.getBaseAddr(), "%o1", "%o1");
                        if(expr.getType() instanceof FloatType)setAddLd(expr, "%f0", "%l7");
                        else setAddLd(expr, "%o0","%l7");
                        if(varType instanceof FloatType && expr.getType() instanceof IntType) {
                            setAddLdPromoted(var,expr, "%o0","%f0");
                        }
                        if(expr.getType() instanceof FloatType)
                            writeAssembly(TWO_PARAM, ST_OP, "%f0", "[%o1]");
                        else writeAssembly(TWO_PARAM, ST_OP, "%o0", "[%o1]");
                        writeAssembly(NEWLINE);
                        writeAssembly(COMMENT, "End init guard");
                        writeAssembly(TWO_PARAM, SET_OP, initLabel, "%o0");
                        writeAssembly(TWO_PARAM, MOV_OP, "1", "%o1");
                        writeAssembly(TWO_PARAM, ST_OP, "%o1", "[%o0]");
                        decreaseIndent();
                        writeAssembly(LABEL, initLabel + ".done");
                        increaseIndent();
                    }
            }

        }
    }
    }


    //----------------------------------------------------------------
    // declare floats in mem
    //----------------------------------------------------------------
    //TODO update all applicable floats with floatSetLd
    //TODO, Remove incr and decr newline logic, replase with writeAssembly(NEWLINE)
    private void floatSetLd(STO sto, String reg){
        float val = ((ConstSTO) sto).getFloatValue();
        num_floats ++;
        writeAssembly(NEWLINE);
        writeAssembly(ONE_PARAM, SECTION_STMT, "\".rodata\"");
        writeAssembly(ONE_PARAM, ALIGN_STMT, "4");
        decreaseIndent();
        writeAssembly(LABEL, Float_SMT + num_floats);
        increaseIndent();
        writeAssembly(ONE_PARAM, SINGLE_STMT, "0r" + val);
        writeAssembly(NEWLINE);
        writeAssembly(ONE_PARAM, SECTION_STMT, "\".text\"");
        writeAssembly(ONE_PARAM, ALIGN_STMT, "4");
        writeAssembly(TWO_PARAM, SET_OP, Float_SMT+num_floats, "%l7");
        if(sto.isRef() || sto.isDotted() || sto.isArrayDeref()) {
            writeAssembly(TWO_PARAM, LD_OP, "[" + reg + "]", reg);
        }
        writeAssembly(TWO_PARAM,LD_OP,"[%l7]", reg);
    }


    //Flush buffer of operands and operator to assembly file
    public void flushAsmOperation() {
        //Get the operator and then remove it
        while(operatorBuffer.size() > 0) {
            Operator op = operatorBuffer.firstElement();
            operatorBuffer.remove(0);

            if(op instanceof UnaryOp) {
                STO operand = operandBuffer.firstElement();
                operandBuffer.remove(0);

                STO result = opResultBuffer.firstElement();
                opResultBuffer.remove(0);

                asmUnaryExpr(operand,(UnaryOp) op,result);
            }

            if(op instanceof BinaryOp) {
                STO operandLeft = operandBuffer.firstElement();
                operandBuffer.remove(0);
                
                STO operandRight = operandBuffer.firstElement();
                operandBuffer.remove(0);
                
                STO result = opResultBuffer.firstElement();
                opResultBuffer.remove(0);

                asmBinaryExpr(operandLeft,operandRight,(BinaryOp) op,result);
            }
        }
    }


    //----------------------------------------------------------------
    // handles arithmetic expressions
    //----------------------------------------------------------------


    //TODO: Make this work for ints, floats, bools that where one of the
    //operands may or may not be constant
    public void asmBinaryExpr(STO e1, STO e2, BinaryOp o, STO res){
        if(e1 instanceof ConstSTO && e2 instanceof ConstSTO) return;

        res.setBaseAddr("%fp");
        setFpOffset(res);

        String e1Name = e1.getName();
        String e2Name = e2.getName();
        String opName = o.getName();
        writeAssembly(NEWLINE);
        writeAssembly(COMMENT, "(" + e1Name + ")" + opName + "(" + e2Name + ")");
      
        int opMode = setOpMode(e1, e2);

        if(isBoolLogicExpr(opName)) {
            num_andOrSkip++;
        }

        //if e1 is not a declared const no load needed, else load from mem
        if(e1.getType() instanceof  IntType || e1.getType() instanceof BoolType) {
            if (e1 instanceof ConstSTO && e1.getOffsetAddr()== null) {
                writeAssembly(TWO_PARAM, SET_OP, Integer.toString(((ConstSTO) e1).getIntValue()), "%o0");
            } else {
                setAddLd(e1, "%o0", "%l7");
            }
            if(e2.getType() instanceof FloatType) {
                setAddLdPromoted(e1, e2,  "%o0", "%f0");
                opMode = 1;
            }
            if(isBoolLogicExpr(opName) && e1.getType() instanceof BoolType) {
                asmShortCircuitLogic(e1, opName, "%o0");
            }
        }

        if((e1.getType() instanceof PointerType) && (e2.getType() instanceof PointerType)){
            if(e1.getType() instanceof NullPointerType){
                writeAssembly(TWO_PARAM, SET_OP, "0","%o0" );
            }
            else{
                setAddLd(e1, "%o0", "%l7");
            }
            if(e2.getType() instanceof NullPointerType){
                writeAssembly(TWO_PARAM, SET_OP, "0", "%o1");
            }

            else {
                setAddLd(e2, "%o1", "%l7");
            }
        }

        if(e1.getType() instanceof  FloatType){
            if (e1 instanceof ConstSTO && e1.getOffsetAddr() == null) {
                floatSetLd(e1, "%f0");
            } else {
                setAddLd(e1, "%f0", "%l7");
            }
        }

        if(e2.getType() instanceof IntType || e2.getType() instanceof BoolType) {
            if (e2 instanceof ConstSTO && e2.getOffsetAddr() == null) {
                String special_reg = "%o1";
                if(o instanceof BooleanOp) special_reg = "%o0";
                writeAssembly(TWO_PARAM, SET_OP, Integer.toString(((ConstSTO) e2).getIntValue()), special_reg);
            } else {
                if(isBoolLogicExpr(opName)) {
                    setAddLd(e2, "%o0", "%l7");
                } else {
                    setAddLd(e2, "%o1", "%l7");
                }
            }
            if(e1.getType() instanceof FloatType){
                setAddLdPromoted(e2, e1, "%o1", "%f1");
                opMode = 1;
            }
            if(isBoolLogicExpr(opName) && e1.getType() instanceof BoolType) {
                asmShortCircuitLogic(e2, opName, "%o0");
            }
        }

        if(e2.getType() instanceof  FloatType){
            if (e2 instanceof ConstSTO && e2.getOffsetAddr() == null) {
                floatSetLd(e2, "%f1");
            } else {
                setAddLd(e2, "%f1", "%l7");
            }
        }

        switch(opName) {
            case "+": { 
                if(opMode == 0) writeAssembly(THREE_PARAM, ADD_OP, "%o0", "%o1", "%o0"); 
                if(opMode == 1) writeAssembly(THREE_PARAM, FADD_OP, "%f0", "%f1", "%f0");  
                break;
            }
            case "-": {
                if(opMode == 0) writeAssembly(THREE_PARAM, SUB_OP, "%o0", "%o1", "%o0"); 
                if(opMode == 1) writeAssembly(THREE_PARAM, FSUB_OP, "%f0", "%f1", "%f0");  
                break;
            }
            case "*": {
                if(opMode == 0) {
                 writeAssembly(ONE_PARAM, CALL_OP, ".mul"); 
                 writeAssembly(NO_PARAM, NOP_STMT);
                 writeAssembly(TWO_PARAM, MOV_OP, "%o0", "%o0");
                }
                if(opMode == 1) writeAssembly(THREE_PARAM, FMUL_OP, "%f0", "%f1", "%f0");  
                break;
            }
            case "/": {
                if(opMode == 0) {
                    writeAssembly(ONE_PARAM, CALL_OP, ".div"); 
                    writeAssembly(NO_PARAM, NOP_STMT);
                    writeAssembly(TWO_PARAM, MOV_OP, "%o0", "%o0");
                }
                if(opMode == 1) writeAssembly(THREE_PARAM, FDIV_OP, "%f0", "%f1", "%f0");  
                break;
            }
            case "%": {
                 writeAssembly(ONE_PARAM, CALL_OP, ".rem"); 
                 writeAssembly(NO_PARAM, NOP_STMT);
                 writeAssembly(TWO_PARAM, MOV_OP, "%o0", "%o0");
                 break;
            }
            case "&": {
                 writeAssembly(THREE_PARAM, AND_OP, "%o0", "%o1", "%o0"); 
                 break;
            }
            case "|": {
                 writeAssembly(THREE_PARAM, OR_OP, "%o0", "%o1", "%o0"); 
                 break;
            }
            case "^": {
                 writeAssembly(THREE_PARAM, XOR_OP, "%o0", "%o1", "%o0"); 
                 break;
            }
            case "||":
            case "&&": {
                asmBoolBinLogicExpr(opName);
                break;
            }
            case "<":
            case ">":
            case ">=":
            case "<=": 
            case "==":
            case "!=":{ 
                asmCmpOp(opMode, opName);  
                break;
            }
        }

        if(opMode == 1 && !(o instanceof ComparisonOp)) {
            setAddSt(res, "%f0");
        } else {
            setAddSt(res, "%o0");
        }

        if (e1.isDotted()) {
            e1.setIsDotted(false);
        }

        if (e2.isDotted()) {
            e2.setIsDotted(false);
        }

    }

    public boolean isBoolLogicExpr(String operator) {
        if(operator.equals("&&") || operator.equals("||") || operator.equals("!"))
            return true;
        return false;
    }

    //----------------------------------------------------------------
    // Writes assembly for <, >, <=, >= operators
    //----------------------------------------------------------------
    public void asmCmpOp(int opMode, String operator) {
        num_cmp ++;
        if(opMode == 0 || opMode == 2 ) writeAssembly(TWO_PARAM, CMP_OP,"%o0","%o1");
        if(opMode == 1) {
             writeAssembly(TWO_PARAM, FCMP_OP,"%f0","%f1");
             writeAssembly(NO_PARAM, NOP_STMT);
        }
        switch(operator) {
            case "<": {
                if(opMode == 0) writeAssembly(ONE_PARAM, BGE_OP, ".$$.cmp." + num_cmp);;
                if(opMode == 1) writeAssembly(ONE_PARAM, FBGE_OP, ".$$.cmp." + num_cmp);;
                break; 
            }
            case ">": {
                if(opMode == 0) writeAssembly(ONE_PARAM,BLE_OP, ".$$.cmp." + num_cmp);
                if(opMode == 1) writeAssembly(ONE_PARAM,FBLE_OP, ".$$.cmp." + num_cmp);
                break;
            }
            case "<=": {
                if(opMode == 0) writeAssembly(ONE_PARAM, BG_OP, ".$$.cmp." + num_cmp);
                if(opMode == 1) writeAssembly(ONE_PARAM, FBG_OP, ".$$.cmp." + num_cmp);;
                break;
            }
            case ">=": {
                if(opMode == 0) writeAssembly(ONE_PARAM,BL_OP, ".$$.cmp." + num_cmp);
                if(opMode == 1) writeAssembly(ONE_PARAM, FBL_OP, ".$$.cmp." + num_cmp);
                break;
            }

            case "==": {
                if(opMode == 0 || opMode == 2) writeAssembly(ONE_PARAM,BNE_OP, ".$$.cmp." + num_cmp);
                if(opMode == 1) writeAssembly(ONE_PARAM, FBNE_OP, ".$$.cmp." + num_cmp);
                break;
            }

            case "!=": {
                if(opMode == 0 || opMode == 2) writeAssembly(ONE_PARAM,BE_OP, ".$$.cmp." + num_cmp);
                if(opMode == 1) writeAssembly(ONE_PARAM, FBE_OP, ".$$.cmp." + num_cmp);
                break;
            }
        }
        writeAssembly(TWO_PARAM, MOV_OP, "%g0", "%o0");
        writeAssembly(ONE_PARAM, INC_OP, "%o0");
        decreaseIndent();
        writeAssembly(LABEL,".$$.cmp."+num_cmp);
        increaseIndent();
    }

    //Numerical flag for types in binaryExpr
    //0 - ints only
    //1 - floats only
    //2  - bools only
    //3 - mixed ints and float (not sure if needed) 
    private int setOpMode(STO e1, STO e2) {
        int opMode = 0;
        if(e1.getType() instanceof IntType 
        && e2.getType() instanceof IntType) opMode = 0; //both are int
        
        if(e1.getType() instanceof FloatType 
        && e2.getType() instanceof FloatType) opMode = 1; //both are float

        if(e1.getType() instanceof BoolType
        && e2.getType() instanceof BoolType) opMode = 2; //both are bool

        if((e1.getType() instanceof FloatType 
        && e2.getType() instanceof IntType) 
        || (e1.getType() instanceof IntType
        && e2.getType() instanceof FloatType
        )) opMode = 3; //one is int one is float
        return opMode;
    }


    //Performs short circuit assembly ops 
    //For boolean logical binary expr
    public void asmShortCircuitLogic(STO sto, String opName, String reg) {
        if(!isBoolLogicExpr(opName)) return;

        String skipLbl = ".$$.andorSkip." + Integer.toString(num_andOrSkip);

        writeAssembly(TWO_PARAM, CMP_OP, reg, "%g0");
        switch(opName) {
            case "&&": {
                writeAssembly(ONE_PARAM, BE_OP, skipLbl);
                break;
            }
            case "||": {
                writeAssembly(ONE_PARAM, BNE_OP, skipLbl);
                break;
            }
        }
        writeAssembly(NO_PARAM, NOP_STMT);
    }

    public void asmBoolBinLogicExpr(String opName) {
        String skipLbl = ".$$.andorSkip." + Integer.toString(num_andOrSkip);
        String endLbl = ".$$.andorEnd." + Integer.toString(num_andOrSkip);
        writeAssembly(ONE_PARAM, BA_OP, endLbl);
        if(opName.equals("||")) writeAssembly(TWO_PARAM, MOV_OP, "0", "%o0");
        if(opName.equals("&&")) writeAssembly(TWO_PARAM, MOV_OP, "1", "%o0");
        decreaseIndent();
        writeAssembly(LABEL, skipLbl);
        increaseIndent();
        if(opName.equals("||")) writeAssembly(TWO_PARAM, MOV_OP, "1", "%o0");
        if(opName.equals("&&")) writeAssembly(TWO_PARAM, MOV_OP, "0", "%o0");
        decreaseIndent();
        writeAssembly(LABEL, endLbl);
        increaseIndent();
    }

    //----------------------------------------------------------------
    // handles if statements
    //----------------------------------------------------------------

    public void asmIfstm(STO res){
        writeAssembly(NEWLINE);
        writeAssembly(COMMENT, "if" + "(" + res.getName() + ")");
     
        setAddLd(res, "%o0", "%l7");
        num_if ++;
        ifLables.push(num_if);
        writeAssembly(TWO_PARAM, CMP_OP, "%o0", "%g0");
        writeAssembly(ONE_PARAM, BE_OP, ".$$.else." + num_if);
        writeAssembly(NO_PARAM, NOP_STMT);
        increaseIndent();
    }

    public void asmElseBlock(){

        String endIfLable = ".$$.endif." + ifLables.peek();
        writeAssembly(NEWLINE);
        writeAssembly(ONE_PARAM, BA_OP, endIfLable);
        writeAssembly(NO_PARAM, NOP_STMT);
        writeAssembly(NEWLINE);

        String elseLable = ".$$.else." + ifLables.peek();
        decreaseIndent();
        writeAssembly(COMMENT, "else");
        decreaseIndent();
        writeAssembly(LABEL, elseLable);
        writeAssembly(NEWLINE);
        increaseIndent();
        increaseIndent();

    }

    public void asmEndIF(){

        String endIfLable = ".$$.endif." + ifLables.peek();

        writeAssembly(NEWLINE);
        decreaseIndent();
        writeAssembly(COMMENT, "endif");
        decreaseIndent();
        writeAssembly(LABEL, endIfLable);
        increaseIndent();
        writeAssembly(NEWLINE);
        ifLables.pop();
    }
    //----------------------------------------------------------------
    // handles while statements
    //----------------------------------------------------------------
    public void asmWhileStmt(){
        num_loops ++;
        loopLabels.push(num_loops);
        writeAssembly(NEWLINE);
        writeAssembly(COMMENT, "while ( ... )");
        decreaseIndent();
        writeAssembly(LABEL, ".$$.loopCheck." + num_loops);
        increaseIndent();
        increaseIndent();
    }

    public void asmLoopCheck(STO exp){
        writeAssembly(NEWLINE);
        writeAssembly(COMMENT, "Check loop condition");
        setAddLd(exp, "%o0", "%l7");
        writeAssembly(TWO_PARAM, CMP_OP, "%o0", "%g0");
        writeAssembly(ONE_PARAM, BE_OP, ".$$.loopEnd." + loopLabels.peek());
        writeAssembly(NO_PARAM, NOP_STMT);
        writeAssembly(NEWLINE);
        writeAssembly(COMMENT, "Start of loop body");
        increaseIndent();
    }

    public void asmBreakContinue(String stmt){
        if(stmt == "continue"){
            writeAssembly(COMMENT,"continue" );
            writeAssembly(ONE_PARAM, BA_OP, ".$$.loopCheck."+loopLabels.peek());
            writeAssembly(NO_PARAM, NOP_STMT);
        }
        if(stmt == "break"){
            writeAssembly(COMMENT, "break");
            writeAssembly(ONE_PARAM, BA_OP, ".$$.loopEnd."+loopLabels.peek());
            writeAssembly(NO_PARAM, NOP_STMT);
        }
    }

    public void asmEndLoop(){
        decreaseIndent();
        writeAssembly(NEWLINE);
        writeAssembly(COMMENT, "End of loop body");
        writeAssembly(ONE_PARAM, BA_OP, ".$$.loopCheck." + loopLabels.peek());
        writeAssembly(NO_PARAM, NOP_STMT);
        decreaseIndent();
        writeAssembly(LABEL, ".$$.loopEnd." + loopLabels.peek());
        writeAssembly(NEWLINE);
        loopLabels.pop();
    }
    //----------------------------------------------------------------
    // handles foreach stmts
    //----------------------------------------------------------------
    public void asmForeachStmt(STO iterationVar, STO array, STO ptr){

        num_loops ++;
        loopLabels.push(num_loops);

        Type elmType = ((ArrayType)array.getType()).getBase();
        int arrSize = ((ArrayType)array.getType()).getSize();
        //iterationVar.setBaseAddr("%fp");
        //setFpOffset(iterationVar);
        ptr.setBaseAddr("%fp");
        setFpOffset(ptr);

        writeAssembly(COMMENT, "foreach ( ... )");
        writeAssembly(COMMENT, "traversal ptr = --array");
        writeAssembly(TWO_PARAM, SET_OP, array.getOffsetAddr(), "%o0");
        writeAssembly(THREE_PARAM, ADD_OP, array.getBaseAddr(), "%o0", "%o0");
        if(array.isDotted) writeAssembly(TWO_PARAM, LD_OP, "[%o0]", "%o0");
        writeAssembly(TWO_PARAM, SET_OP, Integer.toString(elmType.getSize()), "%o1");
        writeAssembly(THREE_PARAM, SUB_OP, "%o0", "%o1", "%o0");
        setAddSt(ptr, "%o0");
        decreaseIndent();
        writeAssembly(LABEL, ".$$.loopCheck." + num_loops);
        increaseIndent();
        increaseIndent();

        writeAssembly(COMMENT, "++traversal ptr");
        setAddLd(ptr, "%o0", "%o1");
        writeAssembly(TWO_PARAM, SET_OP, Integer.toString(elmType.getSize()), "%o2");
        writeAssembly(THREE_PARAM, ADD_OP, "%o0", "%o2", "%o0");
        writeAssembly(TWO_PARAM, ST_OP, "%o0", "[%o1]");

        writeAssembly(COMMENT,"traversal ptr < array end addr?" );
        writeAssembly(TWO_PARAM, SET_OP, array.getOffsetAddr(), "%o0");
        writeAssembly(THREE_PARAM, ADD_OP, array.getBaseAddr(), "%o0", "%o0");
        if(array.isDotted) writeAssembly(TWO_PARAM, LD_OP, "[%o0]", "%o0");
        writeAssembly(TWO_PARAM, SET_OP, Integer.toString(arrSize), "%o1");
        writeAssembly(THREE_PARAM, ADD_OP, "%o0", "%o1", "%o1");
        setAddLd(ptr, "%o0", "%o0");
        writeAssembly(TWO_PARAM, CMP_OP, "%o0", "%o1");
        writeAssembly(ONE_PARAM, BGE_OP, ".$$.loopEnd." + loopLabels.peek());
        writeAssembly(NO_PARAM, NOP_STMT);

        writeAssembly(COMMENT, "iterVar = currentElem");
        if((iterationVar.getType() instanceof FloatType) && (!iterationVar.isRef)) {
            writeAssembly(TWO_PARAM, SET_OP, iterationVar.getOffsetAddr(), "%o1");
            writeAssembly(THREE_PARAM, ADD_OP, iterationVar.getBaseAddr(), "%o1", "%o1");
            writeAssembly(TWO_PARAM, LD_OP, "[%o0]", "%f0");
            if(((ArrayType) array.getType()).getBase() instanceof IntType)
                writeAssembly(TWO_PARAM, FITOS_OP, "%f0", "%f0");
            writeAssembly(TWO_PARAM, ST_OP, "%f0", "[%o1]");
        }
        else {
            writeAssembly(TWO_PARAM, SET_OP, iterationVar.getOffsetAddr(), "%o1");
            writeAssembly(THREE_PARAM, ADD_OP, iterationVar.getBaseAddr(), "%o1", "%o1");
            if(!iterationVar.isRef)writeAssembly(TWO_PARAM, LD_OP, "[%o0]", "%o0");
            writeAssembly(TWO_PARAM, ST_OP, "%o0", "[%o1]");
        }

        writeAssembly(NEWLINE);
        writeAssembly(COMMENT,"Start of loop body");
        increaseIndent();
    }
    //----------------------------------------------------------------
    // handles unary expr
    //----------------------------------------------------------------
    public void asmUnaryExpr(STO e, UnaryOp o, STO res){
        if(e instanceof ConstSTO) return;

        res.setBaseAddr("%fp");
        setFpOffset(res);

        String eName = e.getName();
        String opName = o.getName();
        writeAssembly(NEWLINE);
        writeAssembly(COMMENT, opName + "(" + eName + ")");
    
        if(e.getType() instanceof IntType || e.getType() instanceof PointerType ) {
            setAddLd(e, "%o0", "%l7");
        }
        if(e.getType() instanceof FloatType) {
            setAddLd(e, "%f0", "%l7");
        }
        switch(opName) {
            case "+": { 
                if(e.getType() instanceof IntType) {
                    writeAssembly(TWO_PARAM, MOV_OP, "%o0", "%o0"); 
                    setAddSt(res, "%o0");
                }
                if(e.getType() instanceof FloatType) {
                    writeAssembly(TWO_PARAM, FMOV_OP, "%f0", "%f0"); 
                    setAddSt(res, "%f0");
                }
                break;
            }

            case "-": {
                if(e.getType() instanceof IntType) {
                    writeAssembly(TWO_PARAM, NEG_OP, "%o0", "%o0");
                    setAddSt(res, "%o0"); 
                } 
                if(e.getType() instanceof FloatType) {
                    writeAssembly(TWO_PARAM, FNEG_OP, "%f0", "%f0");
                    setAddSt(res, "%f0");
                }
                
                break;
            }
            case "++": {
                IncDecOp incDecOp = (IncDecOp) o;
                String opFullName = incDecOp.getFullName();
                if(e.getType() instanceof IntType) {
                    writeAssembly(TWO_PARAM, SET_OP, "1", "%o1");
                    writeAssembly(THREE_PARAM, ADD_OP, "%o0", "%o1", "%o2");
                    writeAssembly(TWO_PARAM, SET_OP, res.getOffsetAddr(), "%o1");
                    writeAssembly(THREE_PARAM, ADD_OP, "%fp", "%o1", "%o1");
                }

                if(e.getType() instanceof FloatType) {
                    ConstSTO addOne = new ConstSTO("incrSto", new FloatType(), 1.0);
                    writeAssembly(NEWLINE);
                    floatSetLd(addOne, "%f1");
                    writeAssembly(THREE_PARAM, FADD_OP, "%f0", "%f1", "%f2");
                    writeAssembly(TWO_PARAM, SET_OP, res.getOffsetAddr(), "%o1");
                    writeAssembly(THREE_PARAM, ADD_OP, "%fp", "%o1", "%o1");
                }

                if(e.getType() instanceof PointerType){
                    int bsize = ((PointerType)e.getType()).getBase().getSize();
                    writeAssembly(TWO_PARAM, SET_OP, Integer.toString(bsize),"%o1");
                    writeAssembly(THREE_PARAM, ADD_OP, "%o0", "%o1", "%o2");
                    writeAssembly(TWO_PARAM, SET_OP, res.getOffsetAddr(), "%o1");
                    writeAssembly(THREE_PARAM, ADD_OP, "%fp", "%o1", "%o1");

                }

                if(opFullName.equals("a++")) {
                    //Post-Increment
                    if(e.getType() instanceof IntType || e.getType() instanceof PointerType) {
                        writeAssembly(TWO_PARAM, ST_OP, "%o0", "[%o1]");
                    }
                    if(e.getType() instanceof FloatType) {
                        writeAssembly(TWO_PARAM, ST_OP, "%f0", "[%o1]");
                    }

                } 
                if(opFullName.equals("b++")) {
                    //Pre-Increment
                    if(e.getType() instanceof IntType || e.getType() instanceof PointerType ) {
                        writeAssembly(TWO_PARAM, ST_OP, "%o2", "[%o1]");
                    }
                    if(e.getType() instanceof FloatType) {
                        writeAssembly(TWO_PARAM, ST_OP, "%f2", "[%o1]");
                    }
                }
                writeAssembly(TWO_PARAM, SET_OP, e.getOffsetAddr(), "%o1");
                writeAssembly(THREE_PARAM, ADD_OP, e.getBaseAddr(), "%o1", "%o1");
                if(e.getType() instanceof IntType || e.getType() instanceof PointerType) {
                    writeAssembly(TWO_PARAM, ST_OP, "%o2", "[%o1]");
                }
                if(e.getType() instanceof FloatType) {
                    writeAssembly(TWO_PARAM, ST_OP, "%f2", "[%o1]");
                }
                break;
            }
            case "--": {
                IncDecOp incDecOp = (IncDecOp) o;
                String opFullName = incDecOp.getFullName();
                if(e.getType() instanceof IntType) {
                    writeAssembly(TWO_PARAM, SET_OP, "1", "%o1");
                    writeAssembly(THREE_PARAM, SUB_OP, "%o0", "%o1", "%o2");
                    writeAssembly(TWO_PARAM, SET_OP, res.getOffsetAddr(), "%o1");
                    writeAssembly(THREE_PARAM, ADD_OP, "%fp", "%o1", "%o1");
                }
                if(e.getType() instanceof FloatType) {
                    ConstSTO addOne = new ConstSTO("incrSto", new FloatType(), 1.0);
                    writeAssembly(NEWLINE);
                    floatSetLd(addOne, "%f1");
                    writeAssembly(THREE_PARAM, FSUB_OP, "%f0", "%f1", "%f2");
                    writeAssembly(TWO_PARAM, SET_OP, res.getOffsetAddr(), "%o1");
                    writeAssembly(THREE_PARAM, ADD_OP, "%fp", "%o1", "%o1");
                }

                if(e.getType() instanceof PointerType){
                    int bsize = ((PointerType)e.getType()).getBase().getSize();
                    writeAssembly(TWO_PARAM, SET_OP, Integer.toString(bsize), "%o1");
                    writeAssembly(THREE_PARAM, SUB_OP, "%o0", "%o1", "%o2");
                    writeAssembly(TWO_PARAM, SET_OP, res.getOffsetAddr(), "%o1");
                    writeAssembly(THREE_PARAM, ADD_OP, "%fp", "%o1", "%o1");

                }

                if(opFullName.equals("a--")) {
                    //Post-Decrement
                    if(e.getType() instanceof IntType || e.getType() instanceof PointerType) {
                        writeAssembly(TWO_PARAM, ST_OP, "%o0", "[%o1]");
                    }
                    if(e.getType() instanceof FloatType) {
                        writeAssembly(TWO_PARAM, ST_OP, "%f0", "[%o1]");
                    }


                } 
                if(opFullName.equals("b--")) {
                    //Pre-Decrement
                    if(e.getType() instanceof IntType || e.getType() instanceof PointerType) {
                        writeAssembly(TWO_PARAM, ST_OP, "%o2", "[%o1]");
                    }
                    if(e.getType() instanceof FloatType) {
                        writeAssembly(TWO_PARAM, ST_OP, "%f2", "[%o1]");
                    }
                }
                writeAssembly(TWO_PARAM, SET_OP, e.getOffsetAddr(), "%o1");
                writeAssembly(THREE_PARAM, ADD_OP, e.getBaseAddr(), "%o1", "%o1");
                if(e.getType() instanceof IntType || e.getType() instanceof PointerType) {
                    writeAssembly(TWO_PARAM, ST_OP, "%o2", "[%o1]");
                }
                if(e.getType() instanceof FloatType) {
                    writeAssembly(TWO_PARAM, ST_OP, "%f2", "[%o1]");
                }
                break;
            }
        }
    }

    //changed for local and global
    private void setAddLd(STO sto, String ldReg, String addReg){
        writeAssembly(TWO_PARAM, SET_OP, sto.getOffsetAddr(), addReg);
        writeAssembly(THREE_PARAM, ADD_OP, sto.getBaseAddr(), addReg, addReg);
        if(sto.isRef() || sto.isDotted() || sto.isArrayDeref() || sto.isPtrDeref())
            writeAssembly(TWO_PARAM, LD_OP, "[" + addReg + "]", addReg);

        writeAssembly(TWO_PARAM, LD_OP, "[" + addReg + "]", ldReg);
    }

    private void setAddSt(STO sto, String stReg) {
        writeAssembly(TWO_PARAM, SET_OP, sto.getOffsetAddr(), "%o1");
        writeAssembly(THREE_PARAM, ADD_OP, sto.getBaseAddr(), "%o1", "%o1");
        writeAssembly(TWO_PARAM, ST_OP, stReg, "[%o1]");
    }

    //----------------------------------------------------------------
    // setFpOffset(STO var)
    //----------------------------------------------------------------
    public void setFpOffset(STO var) {
        frameptr_offset -= var.getType().getSize();
        var.setOffsetAddr(Integer.toString(frameptr_offset));
    }

    //----------------------------------------------------------------
    // setStructOffset(STO var)
    //----------------------------------------------------------------
    private void setStructOffset(STO var) {
        int typSize = var.getType().getSize();
        var.setStructOffset(Integer.toString(gbl_offset));
        gbl_offset += typSize;
    }

    //----------------------------------------------------------------
    // resetFp()
    //----------------------------------------------------------------
    public void resetFp() {
        frameptr_offset = 0;
    }

    //----------------------------------------------------------------
    // Define the global label for the fxn name if it does not exist yet
    //----------------------------------------------------------------
    public void asmFuncLbl(STO sto, boolean isGlobal) {
        if(!(sto instanceof FuncSTO)) return;
        FuncSTO func = (FuncSTO) sto;

        writeAssembly(NEWLINE);
        if(isGlobal) writeAssembly(ONE_PARAM, GLOBAL_STMT, func.getName());
        resetIndent();
        writeAssembly(LABEL, func.getName());
    }

    //----------------------------------------------------------------
    // 
    //----------------------------------------------------------------
    public void asmDefaultDTor(STO structDef) {
        FuncSTO structFunc = new FuncSTO("$" + structDef.getName());
        asmFuncDecl_1(structFunc, false, new Vector(), structDef);
        asmFuncParams(new Vector(), structDef);
        asmFuncDecl_2(structFunc);
    }

    //----------------------------------------------------------------
    // 
    //----------------------------------------------------------------
    public void asmCustomDtor() {
        increaseIndent();
        increaseIndent();
        writeAssembly(NEWLINE);
        writeAssembly(COMMENT, "Store Params");
        writeAssembly(NEWLINE);
        writeAssembly(TWO_PARAM, ST_OP, "%i0", "[%fp+68]");
    }

    //----------------------------------------------------------------
    // 
    //----------------------------------------------------------------
    public void asmCloseStruct() {
        gbl_offset = 0;
        in_struct = false;
    }

    //----------------------------------------------------------------
    // 
    //----------------------------------------------------------------
    public void asmStructDot(STO struct, STO field, boolean inStruct) {
        String ptrName = struct.getName();


        if(inStruct) { 
            ptrName = "this";
        }

        System.out.println("asmStructDot: for " + field.getName() + " in " + ptrName);

        String newName = ptrName + "." + field.getName();

        writeAssembly(NEWLINE);
        writeAssembly(COMMENT, newName);

        System.out.println();


        writeAssembly(TWO_PARAM, SET_OP, struct.getOffsetAddr(), "%o0");
        writeAssembly(THREE_PARAM, ADD_OP, struct.getBaseAddr(), "%o0", "%o0");
        if(struct.isRef() || struct.isDotted() || struct.isArrayDeref() || struct.isPtrDeref()) writeAssembly(TWO_PARAM, LD_OP, "[" + "%o0" + "]", "%o0");
        if(in_struct) writeAssembly(TWO_PARAM, LD_OP, "[%o0]", "%o0");
   

         STO tmpField;
         if(field.getType() instanceof ArrayType) {
            tmpField = new VarSTO(newName, ((ArrayType) field.getType()).getBase()); 
         } else {
            tmpField = new VarSTO(newName, field.getType()); 
         }

         tmpField.getType().setSize(4);
         if(field instanceof FuncSTO) {
             
         } else {
            writeAssembly(TWO_PARAM, SET_OP, field.getStructOffset(), "%o1");
            writeAssembly(THREE_PARAM, ADD_OP, "%g0", "%o1", "%o1");
            writeAssembly(THREE_PARAM, ADD_OP, "%o0", "%o1", "%o0");     
            setFpOffset(tmpField);
            tmpField.setBaseAddr("%fp");
            setAddSt(tmpField, "%o0");
        }

        field.setIsDotted(true); 
        field.setOffsetAddr(tmpField.getOffsetAddr());
    }

    //----------------------------------------------------------------
    // 
    //----------------------------------------------------------------
    public void asmFinishCtorCall(STO cTorVar) {
        //Store the var in bss section
        String funcLbl = ".$$.ctorDtor." + Integer.toString(++num_ctordtor);

        //Add dTor to stacks
        dTorSTOStack.push(cTorVar);
        dTorIDStack.push(num_ctordtor);
        if(in_func) {
            num_dtor_locals++;
        }

        writeAssembly(NEWLINE);
        writeAssembly(ONE_PARAM, SECTION_STMT, "\".bss\"");
        writeAssembly(ONE_PARAM, ALIGN_STMT, "4");
        decreaseIndent();
        writeAssembly(LABEL, funcLbl);
        increaseIndent();
        writeAssembly(ONE_PARAM, SKIP_STMT, "4");

        writeAssembly(NEWLINE);
        writeAssembly(ONE_PARAM, SECTION_STMT, "\".text\"");
        writeAssembly(ONE_PARAM, ALIGN_STMT, "4");
        writeAssembly(NEWLINE);

        writeAssembly(TWO_PARAM, SET_OP, funcLbl, "%o0");
        writeAssembly(TWO_PARAM, SET_OP, cTorVar.getOffsetAddr(), "%o1");
        writeAssembly(THREE_PARAM, ADD_OP, cTorVar.getBaseAddr(), "%o1", "%o1");
        if(cTorVar.arrayDeref) writeAssembly(TWO_PARAM, LD_OP, "[%o1]", "%o1");
        writeAssembly(TWO_PARAM, ST_OP, "%o1", "[%o0]");
        
    }


    //----------------------------------------------------------------
    // 
    //----------------------------------------------------------------
    public void initCtor(STO cTorVar) {
        increaseIndent();
        writeAssembly(NEWLINE);
        writeAssembly(ONE_PARAM, SECTION_STMT, "\".init\"");
        writeAssembly(ONE_PARAM, ALIGN_STMT, "4");
        writeAssembly(ONE_PARAM, CALL_OP, ".$.init." + cTorVar.getName());
        writeAssembly(NO_PARAM, NOP_STMT);

        writeAssembly(NEWLINE);

        writeAssembly(ONE_PARAM, SECTION_STMT, "\".text\"");
        writeAssembly(ONE_PARAM, ALIGN_STMT, "4");
    }

    public void asmDTorTearDown() {
        int dTorID = dTorIDStack.pop();
        STO dTor = dTorSTOStack.pop();
        Type struct_type = dTor.getType();
        String ctorDtorLabel = ".$$.ctorDtor." + dTorID;


        String structLabel = struct_type.getName() + ".$" + struct_type.getName() + ".void"; 

        writeAssembly(TWO_PARAM, SET_OP, ctorDtorLabel, "%o0");
        writeAssembly(TWO_PARAM, LD_OP, "[%o0]", "%o0");
        writeAssembly(TWO_PARAM, CMP_OP, "%o0", "%g0");
        writeAssembly(ONE_PARAM, BE_OP, ctorDtorLabel + ".fini.skip");
        writeAssembly(NO_PARAM, NOP_STMT);
        writeAssembly(ONE_PARAM, CALL_OP, structLabel);
        writeAssembly(NO_PARAM, NOP_STMT);
        writeAssembly(TWO_PARAM, SET_OP, ctorDtorLabel, "%o0");
        writeAssembly(TWO_PARAM, ST_OP, "%g0", "[%o0]");
        decreaseIndent();
        writeAssembly(LABEL, ctorDtorLabel + ".fini.skip");
        increaseIndent();
    }


    //----------------------------------------------------------------
    // functions related to function definitions
    //----------------------------------------------------------------
    public void asmFuncDecl_1(STO sto, boolean isGlobal, Vector<STO> params, STO structDef) {
        if(!(sto instanceof FuncSTO)) return;
        FuncSTO func = (FuncSTO) sto;

        if(isGlobal) {
            func.setBaseAddr("%g0");
        } else {
            func.setBaseAddr("%fp");
        }
       
 
        if(structDef != null) {
            func.structName = structDef.getName();
            structDef.setBaseAddr("%fp");
        }

        funcLabel = func.mangledName(params);

        asmFuncDeclBody(funcLabel);
        decreaseIndent();
    }

    private void asmFuncDeclBody(String label) {
        resetIndent();
        writeAssembly(LABEL, label);
        increaseIndent();
        writeAssembly(TWO_PARAM, SET_OP, "SAVE." + label, "%g1");
        writeAssembly(THREE_PARAM, SAVE_OP, "%sp", "%g1", "%sp");
    } 

    public void asmFuncParams(Vector<STO> params, STO structDef) {
        increaseIndent();
        increaseIndent();
        writeAssembly(NEWLINE);
        writeAssembly(COMMENT, "Store Params");

        writeAssembly(NEWLINE);

        int regNdx = 0;
        int offset = 68;

        if(structDef != null) {
            String paramReg = "%i" + regNdx;
            regNdx++;
            writeAssembly(TWO_PARAM, ST_OP, paramReg, "[%fp+" + offset +"]");
            structDef.setOffsetAddr(Integer.toString(offset));
            offset += 4;
        }

        if(params == null || params.size() == 0) return;

        for (STO param : params) {
            param.setBaseAddr("%fp");
            Type paramType = param.getType();
            String paramReg = "";
            if(param.getType() instanceof FloatType && !param.isRef()) {
                paramReg = "%f";
            } else {
                paramReg = "%i";
            }
            paramReg = paramReg + regNdx;
            writeAssembly(TWO_PARAM, ST_OP, paramReg, "[%fp+" + offset +"]");
            param.setOffsetAddr(Integer.toString(offset));
            regNdx++;
            offset += 4;
        }
    }

    public void asmFuncDecl_2(FuncSTO func) {
        asmFuncEnd(funcLabel, func);
        funcLabel = "";
    }

    public void asmFuncEnd(String label, STO func) {
        decreaseIndent();
        writeAssembly(NEWLINE);
        writeAssembly(COMMENT, "End of function " + label);
        writeAssembly(ONE_PARAM, CALL_OP, label + ".fini");
        writeAssembly(NO_PARAM, NOP_STMT);
        writeAssembly(NO_PARAM, RET_STMT);
        writeAssembly(NO_PARAM, RESTORE_STMT);

        //TODO calculate the offset addr
        int offsetAddr = -1*frameptr_offset;
        writeAssembly(ASSIGNMENT, "SAVE." + label, "-(92 + " + Integer.toString(offsetAddr) + ") & -8");
        decreaseIndent();
        writeAssembly(NEWLINE);
        resetIndent();
        writeAssembly(LABEL, label + ".fini");
        increaseIndent();

        //TODO Check if the -96 changes
        writeAssembly(THREE_PARAM, SAVE_OP, "%sp", "-96", "%sp");
        if(func instanceof FuncSTO) {
            while(num_dtor_locals > 0) {
                asmDTorTearDown();
                num_dtor_locals--;
            }
        }
        writeAssembly(NO_PARAM, RET_STMT);
        writeAssembly(NO_PARAM, RESTORE_STMT);
        resetIndent();
        resetFp();
    }

    public STO asmFuncCall(STO sto, Vector<STO> exprList, boolean isGlobal){        
        if(!(sto instanceof FuncSTO)) return new ErrorSTO("NOT_A_FUNC");

        FuncSTO func = (FuncSTO) sto;
        func.setType(func.getReturnType());

        Type returnType = func.getReturnType();

        if(isGlobal) {
            func.setBaseAddr("%g0");
        } else {
           func.setBaseAddr("%fp"); 
        }
          
        if(!(func.isCTor  && func.arrayDeref) && !isGlobal) {
            setFpOffset(sto);
        }

        if(func.isCTor  && func.arrayDeref) {
            func.setBaseAddr("%fp");
        }

        Vector<STO> params = func.curParams;

        String funcName = func.mangledName(params);

        writeAssembly(COMMENT, funcName + "(...)");
        int regNdx = 0;

        if(func.isCTor) {
            writeAssembly(TWO_PARAM, SET_OP, func.getOffsetAddr(), "%o0");
            writeAssembly(THREE_PARAM, ADD_OP, func.getBaseAddr(), "%o0", "%o0");
        }

        if(func.isCTor && func.arrayDeref) {
            writeAssembly(TWO_PARAM, LD_OP, "[%o0]", "%o0");
        }

        if(in_struct || func.isCTor || func.structName.length() > 0) {
            regNdx++;
        }

        if(exprList != null) {
            for (int i = 0; i<exprList.size(); i++) {
                STO expr = exprList.get(i);
                STO param = params.get(i);

                if(expr instanceof ConstSTO && !(expr.getType() instanceof ArrayType)) {
                    ConstSTO constExpr = (ConstSTO) expr;
                    if(constExpr.getType() instanceof FloatType) {
                       writeAssembly(COMMENT, param.getName() + " <- " + constExpr.getFloatValue());
                       floatSetLd(constExpr, "%f"+regNdx);
                    } else {
                        if(expr.getType() instanceof BoolType) {
                            writeAssembly(COMMENT, param.getName() + " <- " + constExpr.getBoolValue());
                        } else {
                            writeAssembly(COMMENT, param.getName() + " <- " + constExpr.getIntValue());
                        }
                        writeAssembly(TWO_PARAM, SET_OP, Integer.toString(constExpr.getIntValue()), "%o"+regNdx);
                    }
                } else {
                    writeAssembly(COMMENT, param.getName() + " <- " + expr.getName());
                    if(param.isRef()) {
                        String regName = "%o" + regNdx;
                        writeAssembly(TWO_PARAM, SET_OP, expr.getOffsetAddr(),regName);
                        writeAssembly(THREE_PARAM, ADD_OP, expr.getBaseAddr(), regName, regName);
                    } else {
                        //Its an expression
                        String ldReg = "%";
                        if(expr.getType() instanceof FloatType) {
                            ldReg = ldReg + "f";
                        } else {
                            ldReg = ldReg + "o";
                        }
                        ldReg = ldReg + regNdx;


                        setAddLd(expr, ldReg, "%l7");
                    }
                }
                regNdx += 1;
            }
        }

        writeAssembly(ONE_PARAM, CALL_OP, funcName);
        writeAssembly(NO_PARAM, NOP_STMT);

        if(returnType instanceof FloatType && !func.isRef ) {
            setAddSt(func, "%f0");
        } else if (!(returnType instanceof VoidType)) {
            setAddSt(func, "%o0");
        }

        return func;
    }

    public void asmReturnStmt( FuncSTO func, STO retExpr) {
        writeAssembly(NEWLINE);
        if(retExpr instanceof ConstSTO) {
          ConstSTO retExprConst = (ConstSTO) retExpr;
          if(retExpr.getType() instanceof FloatType) {
                float retFloatVal = retExprConst.getFloatValue();
                writeAssembly(COMMENT, "return " + retFloatVal + ";");
                floatSetLd(retExprConst, "%f0");
            } else if(retExpr.getType() instanceof IntType || retExpr.getType() instanceof BoolType) {
                int retIntVal = retExprConst.getIntValue();
                writeAssembly(COMMENT, "return " + retIntVal + ";");
                writeAssembly(TWO_PARAM, SET_OP, Integer.toString(retIntVal), "%i0");
            } 
        } else if(func.isRef) {
            writeAssembly(COMMENT, "return " + retExpr.getName() + ";");
            writeAssembly(TWO_PARAM, SET_OP, retExpr.getOffsetAddr(), "%i0");
            writeAssembly(THREE_PARAM, ADD_OP, retExpr.getBaseAddr(), "%i0", "%i0");

        } else if(!(retExpr.getType() instanceof VoidType)) {
             writeAssembly(COMMENT, "return " + retExpr.getName() + ";");
             if (retExpr.getType() instanceof FloatType) {
                setAddLd(retExpr, "%f0", "%l7");
             } else {
                setAddLd(retExpr, "%i0", "%l7");
             }
        } else {
             //Its a void type with a return stmt
             writeAssembly(COMMENT, "return;");
        }

        if(func.getReturnType() instanceof FloatType &&
            retExpr.getType() instanceof IntType) {
            setAddLdPromoted(func, retExpr, "%i0", "%f0");
        }

        String funcIniLbl = funcLabel + ".fini";
        writeAssembly(ONE_PARAM, CALL_OP, funcIniLbl);
        writeAssembly(NO_PARAM, NOP_STMT);
        writeAssembly(NO_PARAM, RET_STMT);
        writeAssembly(NO_PARAM, RESTORE_STMT);
    }

    //----------------------------------------------------------------
    // handles variables assignment
    //----------------------------------------------------------------
    public void asmAssignExpr(STO stoDes, STO expr) {
        writeAssembly(NEWLINE);
        writeAssembly(COMMENT, stoDes.getName() + " = " + expr.getName());

        if(stoDes.getType() instanceof StructType) {
            writeAssembly(TWO_PARAM, SET_OP, stoDes.getOffsetAddr(), "%o0");
            writeAssembly(THREE_PARAM, ADD_OP, stoDes.getBaseAddr(), "%o0", "%o0");
            writeAssembly(TWO_PARAM, SET_OP, expr.getOffsetAddr(), "%o1");
            writeAssembly(THREE_PARAM, ADD_OP, expr.getBaseAddr(), "%o1", "%o1");
            writeAssembly(TWO_PARAM, SET_OP, Integer.toString(stoDes.getType().getSize()), "%o2");
            writeAssembly(ONE_PARAM, CALL_OP, "memmove");
            writeAssembly(NO_PARAM, NOP_STMT);
        } else {
            writeAssembly(TWO_PARAM, SET_OP, stoDes.getOffsetAddr(), "%o1");
            writeAssembly(THREE_PARAM, ADD_OP, stoDes.getBaseAddr(), "%o1", "%o1");
            if(stoDes.isArrayDeref() || stoDes.isDotted || stoDes.isPtrDeref()  || stoDes.isRef ) writeAssembly(TWO_PARAM, LD_OP, "[%o1]", "%o1");

            //des is int
            if (stoDes.getType() instanceof IntType) {
                if (expr instanceof ConstSTO) {
                    writeAssembly(TWO_PARAM, SET_OP, Integer.toString(((ConstSTO) expr).getIntValue()), "%o0");
                    writeAssembly(TWO_PARAM, ST_OP, "%o0", "[%o1]");

                } else {
                    setAddLd(expr,"%o0", "%l7");
                    writeAssembly(TWO_PARAM, ST_OP, "%o0", "[%o1]");
                }
            }

            // des is float
            if (stoDes.getType() instanceof FloatType) {

                //promoting int to float
                if (expr.getType() instanceof IntType) {

                    if(expr instanceof ConstSTO){
                        writeAssembly(TWO_PARAM, SET_OP,Integer.toString(((ConstSTO) expr).getIntValue()), "%o0");
                    }
                    else{
                       setAddLd(expr,"%o0", "%l7");
                    }

                    setAddLdPromoted(stoDes, expr, "%o0", "%f0");
                    writeAssembly(TWO_PARAM, ST_OP, "%f0", "[%o1]");

                }
                // expr is a float itself
                if (expr.getType() instanceof FloatType) {

                    if(expr instanceof ConstSTO){
                        floatSetLd(expr, "%f0");
                    }
                    else{
                        setAddLd(expr,"%f0", "%l7");
                    }

                    writeAssembly(TWO_PARAM, ST_OP, "%f0", "[%o1]");
                }

            }

            //des is bool
            if (stoDes.getType() instanceof BoolType) {
                if(expr instanceof ConstSTO){
                    String boolVal = ((ConstSTO)expr).getBoolValue() ? "1" : "0";
                    writeAssembly(TWO_PARAM, SET_OP,boolVal, "%o0" );
                }
                else {
                    setAddLd(expr,"%o0", "%l7");
                }
                writeAssembly(TWO_PARAM, ST_OP, "%o0", "[%o1]");
            }
        }

        if (stoDes.getType() instanceof PointerType) {
            setAddLd(expr,"%o0", "%l7");
            writeAssembly(TWO_PARAM, ST_OP, "%o0", "[%o1]");
        }

        writeAssembly(NEWLINE);
    }

    //----------------------------------------------------------------
    // Type cast
    //----------------------------------------------------------------

    public void asmTypeCast(Type cast, STO expr, STO res){
        res.setBaseAddr("%fp");
        setFpOffset(res);
        writeAssembly(NEWLINE);
        writeAssembly(COMMENT, "(" + cast.getName() + ")" + expr.getName());
        if(expr.getType() instanceof FloatType) setAddLd(expr,"%f0","%l7");
        else setAddLd(expr, "%o0", "%l7");

        if(expr.getType().isEquivalentTo(cast)){
            if(cast instanceof BoolType) {
                num_cmp++;
                writeAssembly(TWO_PARAM, CMP_OP, "%o0", "%g0");
                writeAssembly(ONE_PARAM, BE_OP, ".$$.cmp." + num_cmp);
                writeAssembly(TWO_PARAM, MOV_OP, "%g0", "%o0");
                writeAssembly(TWO_PARAM, MOV_OP, "1", "%o0");
                decreaseIndent();
                writeAssembly(LABEL, ".$$.cmp." + num_cmp);
                increaseIndent();
            }
            if(cast instanceof FloatType) setAddSt(res, "%f0");
            else setAddSt(res, "%o0");
        }
        //type cast to a different type
        else{
            if(expr.getType() instanceof IntType){
                if(cast instanceof FloatType){
                    VarSTO temp = new VarSTO("temp", cast);
                    temp.setBaseAddr("%fp");
                    setFpOffset(temp);
                    writeAssembly(TWO_PARAM, SET_OP, temp.getOffsetAddr(), "%l7");
                    writeAssembly(THREE_PARAM, ADD_OP, temp.getBaseAddr(), "%l7", "%l7");
                    writeAssembly(TWO_PARAM, ST_OP, "%o0", "[%l7]");
                    writeAssembly(TWO_PARAM, LD_OP, "[%l7]", "%f0");
                    writeAssembly(TWO_PARAM, FITOS_OP, "%f0", "%f0");
                    writeAssembly(TWO_PARAM, SET_OP, res.getOffsetAddr(), "%o1");
                    writeAssembly(THREE_PARAM, ADD_OP,"%fp", "%o1", "%o1" );
                    writeAssembly(TWO_PARAM, ST_OP, "%f0", "[%o1]");

                }
                if(cast instanceof BoolType){
                    num_cmp++;
                    writeAssembly(TWO_PARAM, CMP_OP, "%o0", "%g0");
                    writeAssembly(ONE_PARAM, BE_OP, ".$$.cmp." + num_cmp);
                    writeAssembly(TWO_PARAM, MOV_OP, "%g0", "%o0");
                    writeAssembly(TWO_PARAM, MOV_OP, "1", "%o0");
                    decreaseIndent();
                    writeAssembly(LABEL, ".$$.cmp." + num_cmp);
                    increaseIndent();
                    setAddSt(res,"%o0");
                }
            }

            if(expr.getType() instanceof FloatType){
                if(cast instanceof IntType){
                    writeAssembly(TWO_PARAM, FSTOI_OP, "%f0", "%f0");
                    setAddSt(res, "%f0");
                }
                if(cast instanceof BoolType){
                    VarSTO temp = new VarSTO("temp", cast);
                    temp.setBaseAddr("%fp");
                    setFpOffset(temp);
                    num_cmp++;
                    writeAssembly(TWO_PARAM, SET_OP, temp.getOffsetAddr(), "%l7");
                    writeAssembly(THREE_PARAM, ADD_OP, temp.getBaseAddr(), "%l7", "%l7");
                    writeAssembly(TWO_PARAM, ST_OP, "%g0", "[%l7]");
                    writeAssembly(TWO_PARAM, LD_OP, "[%l7]", "%f1");
                    writeAssembly(TWO_PARAM,FITOS_OP,"%f1", "%f1" );
                    writeAssembly(TWO_PARAM, FCMP_OP, "%f0", "%f1");
                    writeAssembly(NO_PARAM, NOP_STMT);
                    writeAssembly(ONE_PARAM, FBE_OP, ".$$.cmp." + num_cmp);
                    writeAssembly(TWO_PARAM, MOV_OP, "%g0", "%o0");
                    writeAssembly(TWO_PARAM, MOV_OP, "1", "%o0");
                    decreaseIndent();
                    writeAssembly(LABEL, ".$$.cmp." + num_cmp);
                    increaseIndent();
                    setAddSt(res,"%o0");

                }
            }

            if(expr.getType() instanceof BoolType){

                if(cast instanceof IntType){
                    num_cmp++;
                    writeAssembly(TWO_PARAM, CMP_OP, "%o0", "%g0");
                    writeAssembly(ONE_PARAM, BE_OP, ".$$.cmp." + num_cmp);
                    writeAssembly(TWO_PARAM, MOV_OP, "%g0", "%o0");
                    writeAssembly(TWO_PARAM, MOV_OP, "1", "%o0");
                    decreaseIndent();
                    writeAssembly(LABEL, ".$$.cmp." + num_cmp);
                    increaseIndent();
                    setAddSt(res,"%o0" );
                }
                if(cast instanceof FloatType){
                    VarSTO temp = new VarSTO("temp", cast);
                    temp.setBaseAddr("%fp");
                    setFpOffset(temp);
                    num_cmp++;
                    writeAssembly(TWO_PARAM, CMP_OP, "%o0", "%g0");
                    writeAssembly(ONE_PARAM, BE_OP, ".$$.cmp." + num_cmp);
                    writeAssembly(TWO_PARAM, MOV_OP, "%g0", "%o0");
                    writeAssembly(TWO_PARAM, MOV_OP, "1", "%o0");
                    decreaseIndent();
                    writeAssembly(LABEL, ".$$.cmp." + num_cmp);
                    increaseIndent();
                    writeAssembly(TWO_PARAM, SET_OP, temp.getOffsetAddr(), "%l7");
                    writeAssembly(THREE_PARAM, ADD_OP, temp.getBaseAddr(), "%l7", "%l7");
                    writeAssembly(TWO_PARAM, ST_OP, "%o0", "[%l7]");
                    writeAssembly(TWO_PARAM, LD_OP, "[%l7]", "%f0");
                    writeAssembly(TWO_PARAM, FITOS_OP, "%f0", "%f0");
                    writeAssembly(TWO_PARAM, SET_OP, res.getOffsetAddr(), "%o1");
                    writeAssembly(THREE_PARAM, ADD_OP,"%fp", "%o1", "%o1" );
                    writeAssembly(TWO_PARAM, ST_OP, "%f0", "[%o1]");
                }

            }
        }
    }
    //----------------------------------------------------------------
    // handles array dereference
    //----------------------------------------------------------------
    public void asmArrayDeref(STO sto, STO idx, STO res){


        writeAssembly(NEWLINE);
        writeAssembly(COMMENT, sto.getName() + "[" + idx.getName() + "]");
        res.setBaseAddr("%fp");

        //setFpOffset(res);
        //Hacking around the offet for quick fix
        frameptr_offset -= 4;
        res.setOffsetAddr(Integer.toString(frameptr_offset));

        if(idx instanceof ConstSTO){
            writeAssembly(TWO_PARAM, SET_OP,Integer.toString(((ConstSTO)idx).getIntValue()), "%o0");
        }
        else{
            setAddLd(idx,"%o0","%l7");
        }
        int bSize = ((ArrayType)sto.getType()).getBase().getSize();
        int arrLen = ((ArrayType)sto.getType()).getLen();
        writeAssembly(TWO_PARAM, SET_OP, Integer.toString(arrLen), "%o1");
        writeAssembly(ONE_PARAM, CALL_OP,".$$.arrCheck" );
        writeAssembly(NO_PARAM, NOP_STMT);
        writeAssembly(TWO_PARAM, SET_OP, Integer.toString(bSize), "%o1");
        writeAssembly(ONE_PARAM, CALL_OP,MUL_OP);
        writeAssembly(NO_PARAM, NOP_STMT);
        writeAssembly(TWO_PARAM, MOV_OP, "%o0", "%o1");
        writeAssembly(TWO_PARAM, SET_OP, sto.getOffsetAddr(), "%o0");
        writeAssembly(THREE_PARAM, ADD_OP, sto.getBaseAddr(),"%o0", "%o0");
        if(sto.isRef || sto.isDotted) writeAssembly(TWO_PARAM, LD_OP, "[%o0]", "%o0");
        writeAssembly(ONE_PARAM, CALL_OP, ".$$.ptrCheck");
        writeAssembly(NO_PARAM, NOP_STMT);
        writeAssembly(THREE_PARAM, ADD_OP, "%o0", "%o1", "%o0");
        setAddSt(res, "%o0");

    }
    //----------------------------------------------------------------
    // handles ptr dereference
    //----------------------------------------------------------------
    public void asmPtrDeref(STO sto, STO res){

        writeAssembly(NEWLINE);
        writeAssembly(COMMENT, "*" +sto.getName());
        res.setBaseAddr("%fp");
        setFpOffset(res);
        setAddLd(sto, "%o0", "%l7");
        writeAssembly(ONE_PARAM, CALL_OP, ".$$.ptrCheck");
        writeAssembly(NO_PARAM, NOP_STMT);
        setAddSt(res,"%o0");

    }

    public void asmPtrDeref(STO sto, STO idx, STO res){

        writeAssembly(NEWLINE);
        writeAssembly(COMMENT, sto.getName() + "[" + idx.getName() + "]");
        res.setBaseAddr("%fp");
        setFpOffset(res);
        if(idx instanceof ConstSTO){
            writeAssembly(TWO_PARAM, SET_OP,Integer.toString(((ConstSTO)idx).getIntValue()), "%o0");
        }
        else{
            setAddLd(idx,"%o0","%l7");
        }
        int bSize = ((PointerType)sto.getType()).getBase().getSize();
        writeAssembly(TWO_PARAM, SET_OP, Integer.toString(bSize), "%o1");
        writeAssembly(ONE_PARAM, CALL_OP,MUL_OP);
        writeAssembly(NO_PARAM, NOP_STMT);
        writeAssembly(TWO_PARAM, MOV_OP, "%o0", "%o1");
        setAddLd(sto,"%o0","%l7");
        writeAssembly(ONE_PARAM, CALL_OP, ".$$.ptrCheck");
        writeAssembly(NO_PARAM, NOP_STMT);
        writeAssembly(THREE_PARAM, ADD_OP, "%o0", "%o1", "%o0");
        setAddSt(res, "%o0");

    }
    //----------------------------------------------------------------
    // handles addressof op
    //--------------------------------------------------------
    public void asmAddressOf(STO sto, STO res){
        res.setBaseAddr("%fp");
        setFpOffset(res);
        writeAssembly(NEWLINE);
        writeAssembly(COMMENT, res.getName());
        writeAssembly(TWO_PARAM, SET_OP, sto.getOffsetAddr(), "%o0");
        writeAssembly(THREE_PARAM, ADD_OP, sto.getBaseAddr(),"%o0","%o0" );
        if(sto.isArrayDeref() || sto.isRef) writeAssembly(TWO_PARAM, LD_OP, "[%o0]", "%o0");
        setAddSt(res,"%o0");
    }

    //TODO: Why do we not set the offset Addr?
    private void setAddLdPromoted(STO sto, STO expr, String reg, String fReg) {
        frameptr_offset -= expr.getType().getSize(); 
        writeAssembly(TWO_PARAM, SET_OP,Integer.toString(frameptr_offset), "%l7");
        writeAssembly(THREE_PARAM, ADD_OP,"%fp", "%l7", "%l7");
        writeAssembly(TWO_PARAM, ST_OP, reg, "[%l7]");
        writeAssembly(TWO_PARAM, LD_OP,"[%l7]", fReg);
        writeAssembly(TWO_PARAM, FITOS_OP,fReg, fReg);
    }

    //TODO: Make code indentation consistent
    public void asmWriteStmt(STO sto){

        Type typ = sto.getType();

        if(sto instanceof ConstSTO) {
            ConstSTO csto = (ConstSTO)sto;
            //increaseIndent();


            if(typ instanceof IntType){
                    int val = csto.getIntValue();
                    writeAssembly(COMMENT, "cout <<  " + val) ;
                    writeAssembly(TWO_PARAM, SET_OP, Integer.toString(val), "%o1");
                    writeAssembly(TWO_PARAM, SET_OP, ".$$.intFmt","%o0");
                    writeAssembly(ONE_PARAM, CALL_OP, "printf");
                    writeAssembly(NO_PARAM, NOP_STMT);
                    //TODO:check new line
                    writeAssembly(NEWLINE);
                    //decreaseIndent();

            }

            else if(typ instanceof FloatType){
                num_floats ++;
                float val = csto.getFloatValue();
                writeAssembly(COMMENT, "cout <<  " + val) ;
                writeAssembly(ONE_PARAM, SECTION_STMT, "\".rodata\"");
                writeAssembly(ONE_PARAM, ALIGN_STMT, "4");
                decreaseIndent();
                writeAssembly(LABEL, Float_SMT + num_floats);
                increaseIndent();
                writeAssembly(ONE_PARAM, SINGLE_STMT, "0r" + val);
                writeAssembly(NEWLINE);
                writeAssembly(ONE_PARAM, SECTION_STMT, "\".text\"");
                writeAssembly(ONE_PARAM, ALIGN_STMT, "4");
                writeAssembly(TWO_PARAM, SET_OP, Float_SMT+num_floats, "%l7");
                writeAssembly(TWO_PARAM,LD_OP,"[%l7]", "%f0");
                writeAssembly(ONE_PARAM, CALL_OP, "printFloat");
                writeAssembly(NO_PARAM, NOP_STMT);
                writeAssembly(NEWLINE);
                //decreaseIndent();

            }
            else if(typ instanceof BoolType){
                boolean val = csto.getBoolValue();
                String intVal = val? "1":"0";
                writeAssembly(COMMENT, "cout <<  " + val) ;
                writeAssembly(TWO_PARAM, SET_OP, intVal, "%o0");
                writeAssembly(ONE_PARAM, CALL_OP,".$$.printBool" );
                writeAssembly(NO_PARAM, NOP_STMT);
                writeAssembly(NEWLINE);
                //decreaseIndent();
            }

            //String constant
            else{
                String str = csto.getName();
                writeAssembly(NEWLINE);
                writeAssembly(COMMENT, "cout <<  " + "\"" +  str +  "\"") ;
                //if endln
                if(str.equals("endl")){
                    writeAssembly(TWO_PARAM, SET_OP, ".$$.strEndl", "%o0");
                    writeAssembly(ONE_PARAM, CALL_OP, "printf");
                    writeAssembly(NO_PARAM, NOP_STMT);
                    //TODO:check new line
                    writeAssembly(NEWLINE);
                    //decreaseIndent();
                }
                // if any string other than endl
                else{
                    num_strs++;
                    writeAssembly(ONE_PARAM, SECTION_STMT, "\".rodata\"");
                    writeAssembly(ONE_PARAM, ALIGN_STMT, "4");
                    decreaseIndent();
                    writeAssembly(LABEL, STR_SMT + num_strs);
                    increaseIndent();
                    writeAssembly(ONE_PARAM, ASCI_SMT, "\"" + str + "\"");
                    writeAssembly(NEWLINE);
                    writeAssembly(ONE_PARAM, SECTION_STMT, "\".text\"");
                    writeAssembly(ONE_PARAM, ALIGN_STMT, "4");
                    writeAssembly(TWO_PARAM, SET_OP, ".$$.strFmt","%o0");
                    writeAssembly(TWO_PARAM, SET_OP,STR_SMT + num_strs ,"%o1");
                    writeAssembly(ONE_PARAM, CALL_OP, "printf");
                    writeAssembly(NO_PARAM, NOP_STMT);
                    writeAssembly(NEWLINE);
                    //decreaseIndent();
                }
            }
        }
        //priniting vars
        else{

            writeAssembly(COMMENT, "cout << " + sto.getName());

            if(typ instanceof IntType){
                setAddLd(sto,"%o1","%l7");
                writeAssembly(TWO_PARAM, SET_OP, ".$$.intFmt","%o0");
                writeAssembly(ONE_PARAM, CALL_OP, "printf");
                writeAssembly(NO_PARAM, NOP_STMT);
                writeAssembly(NEWLINE);

            }

            if(typ instanceof FloatType){
                setAddLd(sto,"%f0","%l7");
                writeAssembly(ONE_PARAM, CALL_OP, "printFloat");
                writeAssembly(NO_PARAM, NOP_STMT);
                writeAssembly(NEWLINE);

            }

            if(typ instanceof BoolType){
                setAddLd(sto, "%o0", "%l7");
                writeAssembly(ONE_PARAM, CALL_OP,".$$.printBool" );
                writeAssembly(NO_PARAM, NOP_STMT);
                writeAssembly(NEWLINE);
            }
        }
    }

    //----------------------------------------------------------------
    // handles exit stmts
    //----------------------------------------------------------------

    public void asmExitStmt(STO sto){
        writeAssembly(COMMENT, "exit"+"("+ sto.getName() + ")");
        if(sto instanceof ConstSTO){
            writeAssembly(TWO_PARAM, SET_OP, Integer.toString(((ConstSTO) sto).getIntValue()), "%o0");
        }
        else{
            setAddLd(sto,"%o0", "%l7");
        }

        writeAssembly(ONE_PARAM, CALL_OP, "exit");
        writeAssembly(NO_PARAM, NOP_STMT);
        writeAssembly(NEWLINE);

    }
    //----------------------------------------------------------------
    // handles cin
    //----------------------------------------------------------------
    public void asmCin(STO sto){

        writeAssembly(NEWLINE);
        writeAssembly(COMMENT, "cin >> " + sto.getName());

        if(sto.getType() instanceof IntType){
            writeAssembly(ONE_PARAM, CALL_OP, "inputInt");
            writeAssembly(NO_PARAM, NOP_STMT);
            setAddSt(sto, "%o0");
        }

        if(sto.getType() instanceof FloatType){
            writeAssembly(ONE_PARAM, CALL_OP,"inputFloat");
            writeAssembly(NO_PARAM, NOP_STMT);
            setAddSt(sto, "%f0");
        }
    }

    //TODO Ask about the ArrayCheck boiler plate assembly
    //TODO handle var promotion (assign int to float, ASK)

    //----------------------------------------------------------------
    // handles new statement
    //----------------------------------------------------------------

    public void asmNewStmt(STO sto){
        writeAssembly(NEWLINE);
        writeAssembly(COMMENT, "new( " + sto.getName() + " )");
        writeAssembly(TWO_PARAM, MOV_OP, "1", "%o0");
        writeAssembly(TWO_PARAM, SET_OP, Integer.toString(sto.getType().getSize()), "%o1");
        writeAssembly(ONE_PARAM, CALL_OP, "calloc");
        writeAssembly(NO_PARAM, NOP_STMT);
        writeAssembly(TWO_PARAM, SET_OP, sto.getOffsetAddr(),"%o1");
        writeAssembly(THREE_PARAM, ADD_OP, sto.getBaseAddr(),"%o1","%o1" );
        if(sto.isArrayDeref())writeAssembly(TWO_PARAM, LD_OP, "[%o1]", "%o1");
        writeAssembly(TWO_PARAM, ST_OP, "%o0", "[%o1]");
    }

    public void asmDeleteStmt(STO sto){
        writeAssembly(NEWLINE);
        writeAssembly(COMMENT, "delete( " + sto.getName()+ " )");
        setAddLd(sto, "%o0", "%l7");
        writeAssembly(ONE_PARAM, CALL_OP, ".$$.ptrCheck");
        writeAssembly(NO_PARAM, NOP_STMT);
        setAddLd(sto, "%o0", "%l7");
        writeAssembly(ONE_PARAM, CALL_OP, "free");
        writeAssembly(NO_PARAM, NOP_STMT);
        writeAssembly(TWO_PARAM, SET_OP, sto.getOffsetAddr(), "%o1");
        writeAssembly(THREE_PARAM, ADD_OP, sto.getBaseAddr(),"%o1","%o1" );
        if(sto.isArrayDeref())writeAssembly(TWO_PARAM, LD_OP, "[%o1]", "%o1");
        writeAssembly(TWO_PARAM, ST_OP, "%g0", "[%o1]");

    }

    //----------------------------------------------------------------
    // 
    //----------------------------------------------------------------
    public void programTearDown() {
        increaseIndent();
        while(dTorSTOStack.size() > 0) {
            String dTorID = Integer.toString(dTorIDStack.peek());
            String dTorLabel = ".$$.ctorDtor."+dTorID+".fini";
            decreaseIndent();
            writeAssembly(LABEL, dTorLabel);
            increaseIndent();
            writeAssembly(THREE_PARAM, SAVE_OP, "%sp", "-96", "%sp");
            asmDTorTearDown();
            writeAssembly(NO_PARAM, RET_STMT);
            writeAssembly(NO_PARAM, RESTORE_STMT);
            writeAssembly(NEWLINE);
            writeAssembly(ONE_PARAM, SECTION_STMT, "\".fini\"");
            writeAssembly(ONE_PARAM, ALIGN_STMT, "4");
            writeAssembly(ONE_PARAM, CALL_OP, dTorLabel);
            writeAssembly(NO_PARAM, NOP_STMT);
            writeAssembly(NEWLINE);
            writeAssembly(ONE_PARAM, SECTION_STMT, "\".text\"");
            writeAssembly(ONE_PARAM, ALIGN_STMT, "4");
        }
        decreaseIndent();
    }
}