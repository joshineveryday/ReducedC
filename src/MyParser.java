//---------------------------------------------------------------------
// CSE 131 Reduced-C Compiler Project
// Copyright (C) 2008-2015 Garo Bournoutian and Rick Ord
// University of California, San Diego
//---------------------------------------------------------------------

import java_cup.runtime.*;
import java.util.Vector;
import java.util.Stack;
import java.util.Map;
import java.util.HashMap;
import java.io.File;
import java.math.BigDecimal;

class MyParser extends parser
{
	private Lexer m_lexer;
	private ErrorPrinter m_errors;
	private boolean m_debugMode;
	private int m_nNumErrors;
	private String m_strLastLexeme;
	private boolean m_bSyntaxError = true;
	private int m_nSavedLineNum;
	public AssemblyCodeGenerator m_asm; 

	public SymbolTable m_symtab;
	public Stack<STO> loopStack; //Add every time loop opens, remove every time loop closes
	public Vector<STO> returnStmts;
	public Vector<STO> exitStmts;
	public boolean hadAryExprError;

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public MyParser(Lexer lexer, ErrorPrinter errors, boolean debugMode)
	{
		m_lexer = lexer;
		m_symtab = new SymbolTable();
		m_errors = errors;
		m_debugMode = debugMode;
		m_nNumErrors = 0;
		loopStack = new Stack();
		returnStmts = new Vector();
		exitStmts = new Vector();
		hadAryExprError = false;
		m_asm = new AssemblyCodeGenerator("rc.s");
		//TODO MAKE SURE PATH IS CORRECT WHEN TURNING IN OR PUSHING
		m_asm.writeAssemblyFromFile("src/asm/boilerplate.sparc");
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public boolean Ok()
	{
		return m_nNumErrors == 0;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public Symbol scan()
	{
		Token t = m_lexer.GetToken();

		//	We'll save the last token read for error messages.
		//	Sometimes, the token is lost reading for the next
		//	token which can be null.
		m_strLastLexeme = t.GetLexeme();

		switch (t.GetCode())
		{
			case sym.T_ID:
			case sym.T_ID_U:
			case sym.T_STR_LITERAL:
			case sym.T_FLOAT_LITERAL:
			case sym.T_INT_LITERAL:
				return new Symbol(t.GetCode(), t.GetLexeme());
			default:
				return new Symbol(t.GetCode());
		}
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public void syntax_error(Symbol s)
	{
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public void report_fatal_error(Symbol s)
	{
		m_nNumErrors++;
		if (m_bSyntaxError)
		{
			m_nNumErrors++;

			//	It is possible that the error was detected
			//	at the end of a line - in which case, s will
			//	be null.  Instead, we saved the last token
			//	read in to give a more meaningful error 
			//	message.
			m_errors.print(Formatter.toString(ErrorMsg.syntax_error, m_strLastLexeme));
		}
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public void unrecovered_syntax_error(Symbol s)
	{
		report_fatal_error(s);
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public void DisableSyntaxError()
	{
		m_bSyntaxError = false;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public void EnableSyntaxError()
	{
		m_bSyntaxError = true;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public String GetFile()
	{
		return m_lexer.getEPFilename();
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public int GetLineNum()
	{
		return m_lexer.getLineNumber();
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public void SaveLineNum()
	{
		m_nSavedLineNum = m_lexer.getLineNumber();
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public int GetSavedLineNum()
	{
		return m_nSavedLineNum;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	void DoProgramStart()
	{
		// Opens the global scope.
		m_symtab.openScope();
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	void DoProgramEnd()
	{
		m_asm.programTearDown();
		m_symtab.closeScope();
	}

	//TODO: Test DoBinary Expr
	//Ask what the do stuff part should do and 
	//How to propogate the error message
	STO DoBinaryExpr(STO a, BinaryOp o, STO b) {
		if ((a instanceof ErrorSTO) || (b instanceof ErrorSTO))
			return new ErrorSTO("Error!!!");
		STO result = o.checkOperands(a, b);
		if (result instanceof ErrorSTO) {
			m_nNumErrors++;
			m_errors.print(result.getName());
		}
		//added to handle constant folding
		if(result instanceof ExprSTO) {
			if(m_symtab.isGlobal()) {
				//Buffer the elements to be written later
				m_asm.operandBuffer.addElement(a);
				m_asm.operandBuffer.addElement(b);
				m_asm.operatorBuffer.addElement(o);
				m_asm.opResultBuffer.addElement(result);
			} else {
				m_asm.asmBinaryExpr(a, b, o, result);
			}
		}
		//If its in the global scope, add the STOs to a buffer to be 
		//evaluated after the global var is initialized
		return result;
	}

	STO DoUnaryExpr(STO a, UnaryOp o) {
		if ((a != null) && (a instanceof ErrorSTO))
			return a;
		STO result = o.checkOperand(a);
		if (result instanceof ErrorSTO) {
			m_nNumErrors++;
			m_errors.print(result.getName());
		 }

		if(m_symtab.isGlobal()) {
			m_asm.operandBuffer.addElement(a);
			m_asm.operatorBuffer.addElement(o);
			m_asm.opResultBuffer.addElement(result);
		} else {
			m_asm.asmUnaryExpr(a, o, result);
		}
		return result;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	STO DoVarDecl(String optStatic, String id, Type t)
	{
		if (m_symtab.accessLocal(id) != null)
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.redeclared_id, id));
		}

		STO sto;

		if(t instanceof ArrayType) {
			sto = new ConstSTO(id, t);
		} else {
			sto = new VarSTO(id, t);
		}
		m_symtab.insert(sto);
		m_asm.asmVarDecl(optStatic, sto, null, m_symtab.isGlobal());
		return sto;
	}

	STO DoVarDecl(String optStatic, String id, Type t, STO expr)
	{

		if (m_symtab.accessLocal(id) != null)
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.redeclared_id, id));
		}

		VarSTO sto = new VarSTO(id, t, false);
		m_symtab.insert(sto);

		if (expr instanceof ErrorSTO)
			return expr;

		if(expr.getName() != null && !(t instanceof ArrayType)) {
			//variable is being initialized, check the type
			Type exprType = expr.getType();
			if(!exprType.isAssignableTo(t)){
				m_nNumErrors++;
				m_errors.print(Formatter.toString(ErrorMsg.error8_Assign, exprType.getName(), t.getName()));
				return new ErrorSTO("Bad_Decl");
			}
		}
		m_asm.asmVarDecl(optStatic, sto, expr, m_symtab.isGlobal());
		return sto;
	}


	//Declare and array var
	STO DoVarDecl(String optStatic, String id, Vector<STO> constAryVec, Type inTyp) {
			ArrayType ary = VectorToArrayType(constAryVec, inTyp);
        	STO var =  DoVarDecl (optStatic, id, ary);
        	return var;
	}

	public void DoStructVarDecl(Vector<STO> aryVec, Type structType, Vector<STO> optCtorCall, String structID, 
		STO cTorVar, Type aryTyp, STO arySTO) {

		if(!hasBadArray(aryVec)) {
		    if(aryTyp == null) {
		    	if(m_symtab.isGlobal()) {
		    		cTorVar.setOffsetAddr(structID);
		    	}
		        DoCtorCall (structType, optCtorCall, cTorVar, false);
		    } else {
		        int arySize = ((ConstSTO) arySTO).getIntValue();
		        STO res = new VarSTO("res", structType);
		        res.setArrayDeref(true);
		     
		        STO aryTypeSTO = new VarSTO(structID, aryTyp);

		        if(m_symtab.isGlobal()) {
		            aryTypeSTO.setBaseAddr("%g0");
		            aryTypeSTO.setOffsetAddr(structID);
		        } else {
		            aryTypeSTO.setBaseAddr("%fp");
		            aryTypeSTO.setOffsetAddr(cTorVar.getOffsetAddr());
		        }

		        for(int i = 0; i<arySize; i++) {
		            ConstSTO idx = new ConstSTO(Integer.toString(i), new IntType(), i);
		            m_asm.asmArrayDeref(aryTypeSTO, idx, res);
		            DoCtorCall (structType, optCtorCall, res, true);
		        }

		        String funcLbl = ".$.init." + cTorVar.getName();
		      	if(m_symtab.isGlobal()) {
					m_asm.asmFuncEnd(funcLbl, cTorVar);
					m_asm.initCtor(cTorVar);
				}
		    }
		}
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	STO checkArrayExpr(STO expr) {
		if(hadAryExprError) 
			return expr;

		if (expr instanceof ErrorSTO)
			return expr;

		if(!(expr.getType().isEquivalentTo(new IntType()))) {
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error10i_Array,
								expr.getType().getName()));
			hadAryExprError = true; //set global
			return new ErrorSTO("BAD_ARRAY");
		}

		if((expr == null) || !(expr instanceof ConstSTO)) {
			m_nNumErrors++;
			m_errors.print(ErrorMsg.error10c_Array);
			hadAryExprError = true; //set global
			return new ErrorSTO("BAD_ARRAY");
		}

		ConstSTO constExpr = (ConstSTO) expr; 
		if(constExpr.getValue() == null) {
			m_nNumErrors++;
			m_errors.print(ErrorMsg.error10c_Array);
			return new ErrorSTO("BAD_ARRAY");
		}

		if(constExpr.getIntValue() <= 0) {
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error10z_Array, 
				constExpr.getIntValue()));
			hadAryExprError = true; //set global
			return new ErrorSTO("BAD_ARRAY");
		}	
		// asm array declaration

		return constExpr;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	boolean hasBadArray(Vector<STO> arrayList) {
		if(arrayList == null) return false;

		for(STO arySTO : arrayList) {
			if(arySTO instanceof ErrorSTO) return true;
		}
		return false;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	void DoExternDecl(String id, Type t)
	{
		if (m_symtab.accessLocal(id) != null)
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.redeclared_id, id));
		}

		VarSTO sto = new VarSTO(id, t);
		m_symtab.insert(sto);
	}

	void DoExternDecl(String id, Vector<STO> t, Type baseType) {
		ArrayType ary = VectorToArrayType(t, baseType);
		DoExternDecl(id, ary);
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	void DoFuncExternDecl(Type returnType, String funcID, Vector<STO> params) {
		DoExternFuncDecl_1(funcID, false, returnType, params);
		DoExternFormalParams(params, returnType);
		DoExternFuncDecl_2();
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	void DoConstDecl(String optStatic, String id, Type t, STO expr)
	{
		if ( expr instanceof ErrorSTO)
			return;
		if (m_symtab.accessLocal(id) != null)
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.redeclared_id, id));
		};

		if(!(expr instanceof ConstSTO)) {
			//cant assign value, not a constSTO
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error8_CompileTime, id));
			return;
		}

		ConstSTO constSTO = new ConstSTO(id, t, ((ConstSTO) expr).getValue());
		m_symtab.insert(constSTO);
		m_asm.asmVarDecl(optStatic, constSTO, expr, m_symtab.isGlobal());

		Type exprType = expr.getType();
		if(!exprType.isAssignableTo(t)) {
			//expression not assignable to const STO
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error8_Assign, exprType.getName(), t.getName()));
			return; 
		}
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	void DoStructdefDecl_1(String id)
	{
		if (m_symtab.accessLocal(id) != null)
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.redeclared_id, id));
		}
		StructdefSTO sto = new StructdefSTO(id, new StructType(id));
		m_symtab.insert(sto);
		m_symtab.setStruct(sto);

		m_symtab.openScope();
		m_asm.in_struct = true;
	}


	void DoDefaultCtor() {
		StructdefSTO struct = m_symtab.getStruct();
		String structName = struct.getName();
		//Create default constructor if none exist
		if(struct.hasCTor == false) {
			DoFuncDecl_1(structName, false, new VoidType(), new Vector());
			DoFormalParams(new Vector(), new VoidType());
	    	if(struct != null) {
	       		((StructType) struct.getType()).getScope().InsertLocal(m_symtab.getFunc());
	    	}
			DoFuncDecl_2();
		}


	}

	void DoDefaultDtor() {
		StructdefSTO struct = m_symtab.getStruct();
		if(struct.hasDTor == false) {
			m_asm.asmDefaultDTor(struct);
		}
	}

	void DoAddFieldVar(STO fieldVar) {
        if(fieldVar instanceof ErrorSTO) return;
        Scope structScope =  ((StructType) m_symtab.getStruct().getType()).getScope();

        if(structScope.access(fieldVar.getName()) != null) {
        	m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error13a_Struct, fieldVar.getName()));
        } 
        structScope.InsertLocal(fieldVar);
        m_asm.asmAddFieldVar(fieldVar);
	}

	void DoAddCTor(STO cTor) {
		StructdefSTO struct = m_symtab.getStruct();
		Scope structScope =  ((StructType) struct.getType()).getScope();
		String structName = m_symtab.getStruct().getName();
		struct.hasCTor = true;

		if(!(cTor.getName().equals(structName))) {
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error13b_Ctor, cTor.getName(), structName));
			struct.hasCTor = false;
		}
		structScope.InsertLocal(cTor);
	}

	void DoAddDTor(STO dTor) {
		StructdefSTO struct = m_symtab.getStruct();
		Scope structScope =  ((StructType) m_symtab.getStruct().getType()).getScope();
		String structName = m_symtab.getStruct().getName();
		String dTorName = dTor.getName().substring(1, dTor.getName().length());
		struct.hasDTor = true;

		if(structScope.access(dTor.getName()) != null) {
        	m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error9_Decl, dTor.getName()));

        } else if(!(dTorName.equals(structName))) {
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error13b_Dtor, dTor.getName(), structName));
		}
		structScope.InsertLocal(dTor);

		m_asm.asmCustomDtor();

	}

	void DoAddFieldFunc(STO fieldFunc) {
		if(fieldFunc != null) {
			if(fieldFunc instanceof ErrorSTO) return;
	        Scope structScope =  ((StructType) m_symtab.getStruct().getType()).getScope();

	        STO scopeSTO = structScope.access(fieldFunc.getName());
	        if(scopeSTO != null) {
	        	if(!(scopeSTO instanceof FuncSTO)) {
		        	m_nNumErrors++;
					m_errors.print(Formatter.toString(ErrorMsg.error13a_Struct, fieldFunc.getName()));
				}
	        } 
	        structScope.InsertLocal(fieldFunc);
    	}
	}

	void DoStructdefDecl_2() {
		//Unset the current scope and nullify m_struct in symbol table
		m_asm.asmCloseStruct();
		m_symtab.closeScope();
		m_symtab.setStruct(null);
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	void DoFuncDecl_1(String id, boolean refFlag, Type retType, Vector<STO> params)
	{

		m_asm.in_func = true;
		if (m_symtab.accessLocal(id) != null)
		{
			if(!(m_symtab.accessLocal(id) instanceof FuncSTO)) {
				//TODO may need to change this error message
				m_nNumErrors++;
				m_errors.print(Formatter.toString(ErrorMsg.redeclared_id, id));
				return;
			}

			FuncSTO sto = (FuncSTO) m_symtab.accessLocal(id);
			m_symtab.setFunc(sto);
			m_symtab.getFunc().setReturnType(retType);
			m_asm.asmFuncDecl_1(m_symtab.getFunc(), m_symtab.isGlobal(), params, m_symtab.getStruct());
			m_symtab.openScope();
			m_symtab.getFunc().setTopLevel(m_symtab.getLevel());
			return;
		}

		FuncSTO sto = new FuncSTO(id, refFlag, retType);
		if(m_symtab.getStruct() == null) {
			m_symtab.insert(sto);
		}

		m_symtab.setFunc(sto);
		if(m_symtab.getStruct() == null) m_asm.asmFuncLbl(m_symtab.getFunc(), m_symtab.isGlobal());
		m_asm.asmFuncDecl_1(m_symtab.getFunc(), m_symtab.isGlobal(), params, m_symtab.getStruct());
		m_symtab.openScope();
		m_symtab.getFunc().setTopLevel(m_symtab.getLevel());
	}

	//----------------------------------------------------------------
	// DoExternFuncDecl_1 - Same is DoFuncDecl_1 except it does
	// not write to the Symbol Table
	//----------------------------------------------------------------
	void DoExternFuncDecl_1(String id, boolean refFlag, Type retType, Vector<STO> params)
	{
		if (m_symtab.accessLocal(id) != null)
		{
			if(!(m_symtab.accessLocal(id) instanceof FuncSTO)) {
				//TODO may need to change this error message
				m_nNumErrors++;
				m_errors.print(Formatter.toString(ErrorMsg.redeclared_id, id));
				return;
			}

			FuncSTO sto = (FuncSTO) m_symtab.accessLocal(id);
			m_symtab.setFunc(sto);
			m_symtab.getFunc().setReturnType(retType);
			m_symtab.openScope();
			m_symtab.getFunc().setTopLevel(m_symtab.getLevel());
			return;
		}

		FuncSTO sto = new FuncSTO(id, refFlag, retType);
		if(m_symtab.getStruct() == null) {
			m_symtab.insert(sto);
		}

		m_symtab.setFunc(sto);
		m_symtab.openScope();
		m_symtab.getFunc().setTopLevel(m_symtab.getLevel());
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	void DoFuncDecl_2()
	{
		m_asm.asmFuncDecl_2(m_symtab.getFunc());
		m_asm.in_func = false;
		m_symtab.closeScope();
		m_symtab.setFunc(null);
	}

	//Same as DoFuncDecl_2 but does not affect assembly
	void DoExternFuncDecl_2()
	{
		m_symtab.closeScope();
		m_symtab.setFunc(null);
	}


	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	STO DoForEachCheck(STO iterationVar, STO expr) {
		if(expr instanceof ErrorSTO) {
			return new ErrorSTO(expr.getName());
		}
		if(iterationVar instanceof ErrorSTO) {
			return new ErrorSTO(iterationVar.getName());
		}

		if (!(expr.getType() instanceof ArrayType)) {
			m_nNumErrors++;
			m_errors.print(ErrorMsg.error12a_Foreach);
			return new ErrorSTO(expr.getName());
		}

		Type exprType = ((ArrayType) expr.getType()).getNext();
		if(!(iterationVar.isRef()) && !(exprType.isAssignableTo(iterationVar.getType()))) {
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error12v_Foreach,
					exprType.getName(), iterationVar.getName(), iterationVar.getType().getName()));
			return new ErrorSTO(expr.getName());
		}

		if((iterationVar.isRef()) && !(exprType.isEquivalentTo(iterationVar.getType()))) {
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error12r_Foreach, 
												exprType.getName(), iterationVar.getName(), iterationVar.getType().getName()));
			return new ErrorSTO(expr.getName());
		}

		STO res = new ExprSTO("FOREACH_EXPR_SUCCESS", iterationVar.getType());


		return res;

	}
	//Type:_2 OptRef:_4 T_ID:_3 T_COLON Expr:_5
	void DoForEachStmt(Type t, boolean OptRef, String ID, STO expr){

		STO iterVar = DoVarDecl (null, ID,t);
		STO ptr = new VarSTO("arrayPtr", iterVar.getType());
		iterVar.setIsRef(OptRef);
		m_asm.asmForeachStmt(iterVar, expr, ptr);
	}

	//----------------------------------------------------------------
	// 
	//----------------------------------------------------------------
	void DoFormalParams(Vector<STO> params, Type retType)
	{
		if (m_symtab.getFunc() == null)
		{
			m_nNumErrors++;
			m_errors.print("internal: DoFormalParams says no proc!");
			return;
		}

		FuncSTO curFunc; 
		if(m_symtab.getStruct() == null) {
			curFunc = m_symtab.getFunc();
		} else {
			//Inside a struct, find the funcSTO inside the structs scope
			Scope structScope = ((StructType) m_symtab.getStruct().getType()).getScope();
			String fieldName = m_symtab.getFunc().getName();

			if(structScope.access(fieldName) != null) {
				STO structField = structScope.access(fieldName);
				if(structField instanceof FuncSTO) {
					curFunc = (FuncSTO) structField;
				} else {
					curFunc = m_symtab.getFunc();
				}
			} else {
				curFunc = m_symtab.getFunc();
			}
		}

		//Check for redeclared identifiers in param list
		if (params != null && params.size() != 0) {
			Vector<String> dupVec = getFuncParamDuplicates(params);
			if(dupVec.size() > 0) {
				for(String id : dupVec) {
					m_nNumErrors++;
					m_errors.print(Formatter.toString(ErrorMsg.redeclared_id, id));
				}
			}
		}

		if(curFunc.hasParams(params)) {
			m_nNumErrors++;
			String func_id = curFunc.getName();
			curFunc.hadOverload = true;
			m_errors.print(Formatter.toString(ErrorMsg.error9_Decl, func_id));
		}

		m_asm.asmFuncParams(params, m_symtab.getStruct());
		if (params == null || params.size() == 0) {
			curFunc.paramLists.addElement(new Vector<STO>());
			curFunc.addReturnType(retType);
			return;
		}

		curFunc.paramLists.addElement(params);
		curFunc.addReturnType(retType);

		for (STO param : params) {
			m_symtab.insert(param);
		}
	}

	//----------------------------------------------------------------
	// Same as DoFormalParams except is does not
	// affect the assembly
	//----------------------------------------------------------------
	void DoExternFormalParams(Vector<STO> params, Type retType)
	{
		if (m_symtab.getFunc() == null)
		{
			m_nNumErrors++;
			m_errors.print("internal: DoFormalParams says no proc!");
			return;
		}

		FuncSTO curFunc; 
		if(m_symtab.getStruct() == null) {
			curFunc = m_symtab.getFunc();
		} else {
			//Inside a struct, find the funcSTO inside the structs scope
			Scope structScope = ((StructType) m_symtab.getStruct().getType()).getScope();
			String fieldName = m_symtab.getFunc().getName();

			if(structScope.access(fieldName) != null) {
				STO structField = structScope.access(fieldName);
				if(structField instanceof FuncSTO) {
					curFunc = (FuncSTO) structField;
				} else {
					curFunc = m_symtab.getFunc();
				}
			} else {
				curFunc = m_symtab.getFunc();
			}
		}

		//Check for redeclared identifiers in param list
		if (params != null && params.size() != 0) {
			Vector<String> dupVec = getFuncParamDuplicates(params);
			if(dupVec.size() > 0) {
				for(String id : dupVec) {
					m_nNumErrors++;
					m_errors.print(Formatter.toString(ErrorMsg.redeclared_id, id));
				}
			}
		}

		if(curFunc.hasParams(params)) {
			m_nNumErrors++;
			String func_id = curFunc.getName();
			curFunc.hadOverload = true;
			m_errors.print(Formatter.toString(ErrorMsg.error9_Decl, func_id));
		}

		if (params == null || params.size() == 0) {
			curFunc.paramLists.addElement(new Vector<STO>());
			curFunc.addReturnType(retType);
			return;
		}

		curFunc.paramLists.addElement(params);
		curFunc.addReturnType(retType);

		for (STO param : params) {
			m_symtab.insert(param);
		}
	}


	//struct helper fxn, Return a vector of all duplicate STOs
	//paramListNdx stores the order of indices in which param list in the vector of param lists or called in the program
	public Vector<String> getFuncParamDuplicates(Vector<STO> params) {
		Map nameFreq = new HashMap(); 
		Vector<String> dupVec = new Vector();

		for (STO param : params) {
		 	String id = param.getName();

		 	if(nameFreq.containsKey(id)) {
		 		nameFreq.put(id, ((Integer) nameFreq.get(id)) + 1);
		 		dupVec.addElement(id);
		 	} else {
		 		nameFreq.put(id, 1);
		 	}
		}

		return dupVec;
	}

	void addReturnSTO(STO retSTO) {
		returnStmts.addElement(retSTO);
	}

	void addExitSTO(STO exitSTO) {
		exitStmts.addElement(exitSTO);
	}

	void wipeReturnStmts() {
		returnStmts = new Vector();
	}

	void wipeExitStmts() {
		exitStmts = new Vector();
	}

	STO DoReturnStmt(STO returnExpr) {

			Type retType = m_symtab.getFunc().getReturnType();
			int topLvl = m_symtab.getFunc().getTopLevel();

			if(topLvl == m_symtab.getLevel()) {
				m_symtab.getFunc().setHasTopLvlRet(true);
			}

			if(returnExpr instanceof ErrorSTO) return returnExpr;

			if((returnExpr.getName() == null) && !retType.isVoid()) {
				m_nNumErrors++;
				m_errors.print(ErrorMsg.error6a_Return_expr);
				return new ErrorSTO(m_symtab.getFunc().getName());
			}

			if(returnExpr.getName() == null && retType.isVoid()) {
				m_asm.asmReturnStmt(m_symtab.getFunc(), returnExpr);
				return new ExprSTO("RETURN_STMT_SUCCESS"); //skip the rest of the checks and look at other return statments
			}

		 	Type returnExprType = returnExpr.getType();
		 	if(returnExprType != null) {
				if(m_symtab.getFunc().isRef()) {
					//return by reference tests
					if(!returnExprType.isEquivalentTo(retType)) {
						m_nNumErrors++;
						m_errors.print(Formatter.toString(ErrorMsg.error6b_Return_equiv, returnExprType.getName(), retType.getName()));
						return new ErrorSTO(m_symtab.getFunc().getName());
					}

					if(!returnExpr.isModLValue()) {
						m_nNumErrors++;
						m_errors.print(ErrorMsg.error6b_Return_modlval);
						return new ErrorSTO(m_symtab.getFunc().getName());
					}
				} else {
					//return by value tests
					if(!returnExprType.isAssignableTo(retType)) {
						m_nNumErrors++;
						m_errors.print(Formatter.toString(ErrorMsg.error6a_Return_type, returnExprType.getName(), retType.getName()));		
						return new ErrorSTO(m_symtab.getFunc().getName());
					}
				}
		 	}
		m_asm.asmReturnStmt(m_symtab.getFunc(), returnExpr);
		return new ExprSTO("RETURN_STMT_SUCCESS");
	}

	//----------------------------------------------------------------
	// Checks to see if return stmt is valid. This MUST be called before DoFuncDecl_2()
	//----------------------------------------------------------------
	STO DoNeedsReturnStmts()
	{
		Vector<STO> returnExprs = returnStmts;
		Type retType = m_symtab.getFunc().getReturnType();
		
		if(
			(returnExprs == null && !retType.isVoid()) ||
			(returnExprs.size() == 0 && !retType.isVoid()) ||
			(m_symtab.getFunc().getHasTopLvlRet() == false && !retType.isVoid())
		) {
			m_nNumErrors++;
			m_errors.print(ErrorMsg.error6c_Return_missing);
		}

		return new ExprSTO("Return_Stmt_Success");
	}


	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	STO DoFuncExitStmts() {
		Vector<STO> exitExprs = exitStmts;
		boolean hasErrors = false;
		for(STO exitExpr : exitExprs) {
			Type exitType = exitExpr.getType();

			if(exitType instanceof ErrorType) continue;

			if(!exitType.isAssignableTo(new IntType())) {
				m_nNumErrors++;
				m_errors.print(Formatter.toString(ErrorMsg.error7_Exit, exitType.getName()));
				hasErrors = true;
			}

		}

		if(hasErrors) return new ErrorSTO(m_symtab.getFunc().getName());
		ExprSTO res = new ExprSTO("EXIT_STMT_SUCCESS");
		return res;
	}

	void DoLoopStmtCheck(String stmtName) {
		if(loopStack.isEmpty()) {
				m_nNumErrors++;
				if(stmtName == "break") {
					m_errors.print(ErrorMsg.error12_Break);
				} else {
					m_errors.print(ErrorMsg.error12_Continue);
				}
		}

		else{
			m_asm.asmBreakContinue(stmtName);
		}
	}

	void DoEndLoop(){
		m_asm.asmEndLoop();
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	void DoBlockOpen()
	{
		// Open a scope.
		m_symtab.openScope();
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	void DoBlockClose()
	{
		/*if(! m_asm.getIfLables().empty()){
			m_asm.asmEndIF();
		}*/

		m_symtab.closeScope();
	}

	void EndIfBlock(){
		m_asm.asmEndIF();
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	STO DoAssignExpr(STO stoDes, STO expr)
	{

		if(stoDes instanceof ErrorSTO) {
			return stoDes;
		}
		if(expr instanceof ErrorSTO) {
			return expr;
		}

		if(expr.getType() instanceof ErrorType)
			return expr;

		if(stoDes.getType() instanceof ErrorType)
			return stoDes;

		//If either sides are composite types, check if there is an errorType within them
		if((stoDes.getType() instanceof PointerType) && ((PointerType) stoDes.getType()).hasErrorType()) {
			return stoDes;
		}

		if((expr.getType() instanceof PointerType) && ((PointerType) expr.getType()).hasErrorType()) {
			return expr;
		}

		if((stoDes.getType() instanceof ArrayType) && ((ArrayType) stoDes.getType()).hasErrorType()) {
			return stoDes;
		}

		if((expr.getType() instanceof ArrayType) && ((ArrayType) expr.getType()).hasErrorType()) {
			return expr;
		}

		if(!stoDes.isModLValue()) {
			m_nNumErrors++;
			m_errors.print (ErrorMsg.error3a_Assign);
			return new ErrorSTO("ASSIGN_NOT_MOD_L");
		}

		//See if expr is assignable to stoDes
		if(!(expr.getType().isAssignableTo(stoDes.getType()))) {
			m_nNumErrors++;
			m_errors.print (Formatter.toString(ErrorMsg.error3b_Assign, expr.getType().getName(), stoDes.getType().getName()));
			return new ErrorSTO("ASSIGN_NOT_ASSIGNABLE");
		}

		m_asm.asmAssignExpr(stoDes, expr);
		STO retExpr = new ExprSTO("(" + stoDes.getName() +  ") = (" + expr.getName() + ")", stoDes.getType());
		retExpr.setBaseAddr(stoDes.getBaseAddr());
		retExpr.setOffsetAddr(stoDes.getOffsetAddr());
		return retExpr;
	}

	//----------------------------------------------------------------
	// Use this for checking types with if and while stmts
	//----------------------------------------------------------------
	STO DoBoolExpr(STO expr) {
		if ( expr instanceof ErrorSTO)
			return expr;
		if(!(expr.getType() instanceof BoolType)) {
			m_nNumErrors++;
			m_errors.print (Formatter.toString(ErrorMsg.error4_Test,  
								expr.getType().getName()));
		}
		return expr;
	}

	void DoIfStmt(STO expr){
		m_asm.asmIfstm(expr);
	}

	void DoWhileStmt() {
		m_asm.asmWhileStmt();
	}

	void DoLoopCheck(STO exp){
		m_asm.asmLoopCheck(exp);
	}

	void DoElseBlock(){
		m_asm.asmElseBlock();
	}

	STO DoAddressOf(STO sto) {
		if(!sto.getIsAddressable()) {
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error18_AddressOf,
					sto.getType().getName()));
			return new ErrorSTO("ADDROF_NOT_ADDRESSABLE");
		}

		Type base = (sto).getType();
		Vector<Type> ptrTypes = new Vector();
		ptrTypes.addElement(new PointerType());

		STO res =  new ExprSTO("&"+(sto).getName(), VectorToPointerType(ptrTypes, base));

		//TODO: check what to pass
		m_asm.asmAddressOf(sto, res);

		return res;
	}


	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	STO DoFuncCall(STO sto, Vector<STO> exprList)
	{
		if(sto == null) {
			return new ErrorSTO("STO_IS_NULL");
		}

		if(sto instanceof ErrorSTO) {
			return sto;
		}

		if (!sto.isFunc())
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.not_function, sto.getName()));
			return new ErrorSTO(sto.getName());
		}
		
		FuncSTO stoObj = (FuncSTO) sto;
		if(!stoObj.isOverloaded()) {
			if(!stoObj.checkNumArgs(exprList, stoObj.paramLists.firstElement())) {
				m_nNumErrors++;
				int expectedSize = 0;

				if(stoObj.paramLists.size() > 0) {
					expectedSize = stoObj.paramLists.firstElement().size();
				}

				int exprListSize = 0;
				if(exprList != null) {
					exprListSize = exprList.size();
				}

				m_errors.print(Formatter.toString(ErrorMsg.error5n_Call, exprListSize, 
					expectedSize));
				return new ErrorSTO(sto.getName());
			}
		}

		Vector<STO> paramCheck = new Vector();
		STO overloadedParamCheck = null;
		if(!stoObj.isOverloaded()) {
			paramCheck = stoObj.checkParams(exprList, stoObj.paramLists.firstElement());
		} else {
			overloadedParamCheck = stoObj.checkAllParamLists(exprList);
		}

		if(!stoObj.isOverloaded()) {
			if(paramCheck.size() > 0) {
				for(STO bad_param : paramCheck) {
					m_nNumErrors++;
					String errMsg = bad_param.getName();
					m_errors.print(errMsg);
				}
				return new ErrorSTO(stoObj.getName());
			}
		} else {
			if((overloadedParamCheck != null) && (overloadedParamCheck instanceof ErrorSTO)) {
				m_nNumErrors++;
				String errMsg = overloadedParamCheck.getName();
				m_errors.print(errMsg);
				return new ErrorSTO(stoObj.getName());
			}
		}

		STO tmpSTO = m_asm.asmFuncCall(stoObj, exprList, m_symtab.isGlobal());
		if(stoObj.isRef()) {
			//return as a modifiable L-val
			STO retSTO = new VarSTO(stoObj.getName(), stoObj.getReturnType());
			retSTO.setIsRef(true);
			retSTO.setOffsetAddr(tmpSTO.getOffsetAddr());
			retSTO.setBaseAddr(tmpSTO.getBaseAddr());
			return retSTO;
		}  

		STO retSTO = new ExprSTO(stoObj.getName(), stoObj.getReturnType());

		retSTO.setOffsetAddr(tmpSTO.getOffsetAddr());
		retSTO.setBaseAddr(tmpSTO.getBaseAddr());
		return retSTO;
	}


	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	STO DoCtorCall(Type structType, Vector<STO> exprList, STO cTorVar, boolean isArray) {

		STO structSTO = m_symtab.access(structType.getName());
		Scope structScope = ((StructType) structSTO.getType()).getScope();
		STO cTor = structScope.access(structType.getName());
		if(structSTO != null) {
			((FuncSTO) cTor).isCTor = true;

			if(m_symtab.isGlobal() && !isArray) {
				cTor.setOffsetAddr(cTorVar.getOffsetAddr());
			} else if (isArray) {
				cTor.setOffsetAddr(cTorVar.getOffsetAddr());
				cTor.setArrayDeref(true);
				cTorVar.setArrayDeref(true);
			}

			STO funcRes = DoFuncCall(cTor, exprList);

			m_asm.asmFinishCtorCall(cTorVar);

			if(m_symtab.isGlobal() && !isArray) {
				String funcLbl = ".$.init." + cTorVar.getName();
				m_asm.asmFuncEnd(funcLbl, cTorVar);
				m_asm.initCtor(cTorVar);
			}
			return funcRes;
		} else {
			return new ErrorSTO("STRUCT_NOT_FOUND");
		}
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	STO DoDesignator2_Dot(STO sto, String strID)
	{
		if (sto instanceof ErrorSTO)
			return sto;
		if(!(sto.getType() instanceof StructType)) {
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error14t_StructExp, sto.getType().getName()));
			return new ErrorSTO(strID);
		}
		StructType structType;
		if(m_symtab.getStruct() != null) {
			structType = ((StructType) m_symtab.getStruct().getType());
		} else {
			structType = ((StructType) sto.getType());
		}

		Scope structScope = structType.getScope();
		STO structField = structScope.access(strID);
		if (structField == null) {
			m_nNumErrors++;
			if(m_symtab.getStruct() != null) {
				m_errors.print(Formatter.toString(ErrorMsg.error14c_StructExpThis, strID));
			} else {
				m_errors.print(Formatter.toString(ErrorMsg.error14f_StructExp, strID, sto.getType().getName()));
			}
			return new ErrorSTO(strID);
		}

		boolean inStruct = false;
		if(m_symtab.getStruct() != null) inStruct = true;
		m_asm.asmStructDot(sto, structField, inStruct);

		return structField;
	}

	//----------------------------------------------------------------
	// setup the recursive clearay chain, assume already constSTO's due to
	// other check array fxn
	//----------------------------------------------------------------
	ArrayType VectorToArrayType(Vector<STO> constExprs, Type baseType) {
		
		//pre-initialize beggining of the Array chain
		STO firstConstSTO = constExprs.firstElement();

		if(firstConstSTO instanceof ErrorSTO) {
			return new ArrayType(0);
		}

		ArrayType header;
		if(firstConstSTO instanceof ConstSTO) {
			header = new ArrayType(((ConstSTO) firstConstSTO).getIntValue());
			constExprs.remove(0); //just used up first element, throw away
		} else {
			return new ArrayType(0);
		}

		for(STO constExpr : constExprs) {
			if(constExpr instanceof ErrorSTO) {
				return new ArrayType(0);
			}
			header.addLast(new ArrayType(((ConstSTO) constExpr).getIntValue()));
		}

		header.addLast(baseType);
		//Array is initialized and its base type set, calculate size
		header.setArraySize();

		return header;
	}   

	Type VectorToPointerType(Vector<Type> ptrTypes, Type baseType) {
		if(ptrTypes != null) {
			//pre-initialize beggining of the Array chain
			Type firstType = ptrTypes.firstElement();
			PointerType header;
			if(firstType instanceof PointerType) {
				header = (PointerType) firstType;
				ptrTypes.remove(0); //just used up first element, throw away
			} else {
				return baseType;
			}
			for(Type ptrType : ptrTypes) {
				header.addLast(ptrType);
			}
			header.addLast(baseType);
			return header;
		} else {
			return baseType;
		}
	} 

	STO DoDereference(STO sto) {
		Type type = sto.getType();

		if(sto instanceof ErrorSTO || sto.getType() instanceof ErrorType) {
			return new ErrorSTO("BAD_DEREF");
		}

		if(type instanceof NullPointerType) {
			m_nNumErrors++;
			m_errors.print(ErrorMsg.error15_Nullptr);
			return new ErrorSTO("NULLPTR_DEREF");
		}

		if(type instanceof PointerType) {
			type = ((PointerType) type).getNext();
			STO newSTO = new ExprSTO("*"+sto.getName(), type);
			newSTO.setIsAddressable(true);
			newSTO.setForceLVal(true);
			newSTO.setPtrDeref(true);
			m_asm.asmPtrDeref(sto, newSTO);
			return newSTO;
		} else {
			//error
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error15_Receiver, type.getName()));
			return new ErrorSTO("DEREFERENCE_NON_POINTER");
		}
	}

	STO DoDesignator2_Arrow(STO sto, String strID) {
		if(sto instanceof ErrorSTO || sto.getType() instanceof ErrorType) {
			return new ErrorSTO("BAD_DEREF");
		}


		Type type = sto.getType();

		if(type instanceof NullPointerType) {
			m_nNumErrors++;
			m_errors.print(ErrorMsg.error15_Nullptr);
			return new ErrorSTO("NULLPTR_DEREF");
		}

		if(!(type instanceof PointerType) || !(((PointerType) type).getNext() instanceof StructType)) {
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error15_ReceiverArrow, type.getName()));
			return new ErrorSTO("DEREFERENCE_NON_POINTER");
		}

		STO deref = DoDereference(sto);
		if(!(deref.getType() instanceof StructType) ) {
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error15_ReceiverArrow, deref.getType().getName()));
		}
		return DoDesignator2_Dot(deref, strID);
	}
	

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	STO DoDesignator2_Array(STO sto, STO aryExpr)
	{
		if(sto instanceof ErrorSTO) return sto;

		if(aryExpr instanceof ErrorSTO) return aryExpr;
		
		Type stoType = sto.getType();

		if(stoType instanceof NullPointerType) {
			m_nNumErrors++;
			m_errors.print(ErrorMsg.error15_Nullptr);
			return new ErrorSTO("NULLPTR_DEREF");
		}

		if(!(stoType instanceof ArrayType) && !(stoType instanceof PointerType)) {
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error11t_ArrExp, stoType.getName()));
			return new ErrorSTO("BAD_ARRAY_DECL");
		}

		Type aryExprType = aryExpr.getType();


		if(!(aryExprType instanceof IntType)) {
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error11i_ArrExp, aryExprType.getName()));
			return new ErrorSTO("BAD_ARRAY_DECL");
		}

		if((aryExpr instanceof ConstSTO) && !(stoType instanceof PointerType)) {
			//check the array bounds
			int requestedNdx = ((ConstSTO) aryExpr).getIntValue();
			int aryLen = ((ArrayType) stoType).getLen();
			if(requestedNdx > (aryLen - 1) || requestedNdx < 0) {
				m_nNumErrors++;
				m_errors.print(Formatter.toString(ErrorMsg.error11b_ArrExp, requestedNdx, (aryLen)));

				return new ErrorSTO("BAD_ARRAY_DECL_BOUNDS");
			} 
		}

		Type nextType;
		if(sto.getType() instanceof PointerType) {
			nextType = ((PointerType) sto.getType()).getNext();
		} else {
			nextType = ((ArrayType) sto.getType()).getNext();
		}

		// return an array sto
		if(sto.getType() instanceof ArrayType) {

			//ConstSTO csto = new ConstSTO(sto.getName(), sto.getType());
			//int idx = ((ConstSTO)aryExpr).getIntValue();
			String name = sto.getName()+"["+ aryExpr.getName() + "]";
			VarSTO res = new VarSTO(name, ((ArrayType)sto.getType()).getBase());
			res.setArrayDeref(true);
			//do array derefrence
			//m_asm.arrayDeref = true;
			m_asm.asmArrayDeref(sto, aryExpr,res );
			return res;
		}
		if(sto.getType() instanceof PointerType) {

			String name = sto.getName()+"["+ aryExpr.getName() + "]";
			VarSTO res = new VarSTO(name, ((PointerType)sto.getType()).getBase());
			res.setPtrDeref(true);
			m_asm.asmPtrDeref(sto, aryExpr,res );
			return res;
		}



		return new VarSTO(sto.getName(), nextType);	
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	void DoNewStmt(STO obj, Vector<STO> exprs) {
		if(!obj.isModLValue() && (exprs == null || exprs.size() == 0)) {
			m_nNumErrors++; 	
			m_errors.print(ErrorMsg.error16_New_var);
			return;
		}

		if(!(obj.getType() instanceof PointerType)) {
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error16_New, obj.getType().getName()));
			return;
		}

		if(! (((PointerType)obj.getType()).getBase() instanceof StructType) && exprs == null){
			//new for none struct types
			m_asm.asmNewStmt(obj);
			return;

		}


		STO struct = DoDereference(obj);

		Type type = struct.getType(); 
		if(type instanceof StructType) {
			StructType structType  = ((StructType) type);
			Scope structScope = structType.getScope();

			//Get the ctor from withing the struct scope
			FuncSTO cTor = ((FuncSTO) structScope.access(type.getName()));
			if(exprs == null) exprs = new Vector();
			DoFuncCall(cTor, exprs);
		} else {
			if(exprs != null) {
				m_nNumErrors++;
				m_errors.print(Formatter.toString(ErrorMsg.error16b_NonStructCtorCall, obj.getType().getName()));
				return;
			}

		}
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	void DoDeleteStmt(STO obj) {
		if(obj instanceof ErrorSTO) return;

		if(!obj.isModLValue()) {
			m_nNumErrors++;
			m_errors.print(ErrorMsg.error16_Delete_var);
			return;
		}

		if(!(obj.getType() instanceof PointerType)) {
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error16_Delete, obj.getType().getName()));
			return;
		}

		m_asm.asmDeleteStmt(obj);
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	
	//Gets the size of other types 
	STO DoSizeOf(STO sto) {
		if(!sto.getIsAddressable() || (sto.getType() instanceof VoidType) || (sto.getType() == null)) {
			m_nNumErrors++;
			m_errors.print(ErrorMsg.error19_Sizeof);
		}

		STO retSto; 
		if(sto.getType() instanceof ArrayType) {
			int size = ((ArrayType) sto.getType()).getBase().getSize();
			Type curType = ((ArrayType) sto.getType());
			while((curType instanceof ArrayType) && ((ArrayType) curType).getNext() != null) {
				size = size * ((ArrayType) curType).getLen();
				curType = ((ArrayType) curType).getNext();
			}
			retSto = new ConstSTO("size", new IntType(), size);
		} else {
			retSto = new ConstSTO("size", new IntType(), sto.getType().getSize());
		}
		retSto.setIsAddressable(false);
		return retSto; 
	}

	//Gets the size of arrays
	STO DoSizeOf(Type type, Vector<STO> stos) {
		if(stos == null || stos.size() == 0) {
			return new ConstSTO("size", new IntType(), type.getSize());
		}

		int size = type.getSize();
		for(STO sto : stos) { 
			size = size*((ConstSTO) sto).getIntValue(); }
 		STO retSto = new ConstSTO("size", new IntType(), size);

 		retSto.setIsAddressable(false);
		return retSto;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	STO DoTypeCast(Type castType, STO expr) {
		if(!validTypeCast(castType, expr)) {
			m_nNumErrors++;
		 	m_errors.print(Formatter.toString(ErrorMsg.error20_Cast, expr.getType().getName(), castType.getName()));
		 	return new ErrorSTO("INVALID_CAST");
		}

		BigDecimal exprVal = null;
		if((expr instanceof ConstSTO) && ((ConstSTO) expr).getValue() != null) {
			ConstSTO constExpr = (ConstSTO) expr;
			if(castType instanceof IntType) {
				exprVal = new BigDecimal(constExpr.getIntValue());
			} else if(castType instanceof FloatType) {
				exprVal = new BigDecimal(constExpr.getFloatValue());
			} else if(castType instanceof BoolType) {
				int intVal = (constExpr.getIntValue() != 0)? 1 : 0;
				exprVal = new BigDecimal(intVal);
			}

			STO sto = new ConstSTO(expr.getName(), castType, exprVal);
			sto.setIsAddressable(false);
			return sto;
		} else {
			ExprSTO res = new ExprSTO(expr.getName(), castType);
			m_asm.asmTypeCast(castType, expr, res);
			return res;
		}
	}

	//helper for DoTypeCast
	boolean validTypeCast(Type castType, STO expr) {
		boolean validCast = true;

		//Check the cast Type
		if(!(castType instanceof BoolType) && 
		   !(castType instanceof IntType) && 
		   !(castType instanceof FloatType) &&
		   !(castType instanceof PointerType)) {
				validCast = false;
		}

		if(castType instanceof NullPointerType) {
			validCast = false;
		}


		//Check the expr Type
		Type exprType = expr.getType();
		if(!(exprType instanceof BoolType) && 
		   !(exprType instanceof IntType) && 
		   !(exprType instanceof FloatType) &&
		   !(exprType instanceof PointerType)) {
				validCast = false;
		}

		if(exprType instanceof NullPointerType) {
			validCast = false;
		}

		return validCast;

	} 

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	STO DoDesignator3_ID(String strID)
	{
		STO sto;
		if ((sto = m_symtab.access(strID)) == null)
		{
			m_nNumErrors++;
		 	m_errors.print(Formatter.toString(ErrorMsg.undeclared_id, strID));
			sto = new ErrorSTO(strID);
		}

		return sto;
	}

	STO DoDesignator4_ID(String strID)
	{
		STO sto;
		if ((sto = m_symtab.accessGlobal(strID)) == null)
		{
			m_nNumErrors++;
		 	m_errors.print(Formatter.toString(ErrorMsg.error0g_Scope, strID));
			sto = new ErrorSTO(strID);
		}

		return sto;
	}
	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	Type DoStructType_ID(String strID)
	{
		STO sto;

		if ((sto = m_symtab.accessGlobal(strID)) == null)
		{
			m_nNumErrors++;
		 	m_errors.print(Formatter.toString(ErrorMsg.undeclared_id, strID));
			return new ErrorType();
		}

		if (!sto.isStructdef())
		{
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.not_type, sto.getName()));
			return new ErrorType();
		}

		return sto.getType();
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------

	void DoWriteStmt(STO sto){
			if (sto instanceof ErrorSTO) return;
			m_asm.asmWriteStmt(sto);
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	void DoExitStmt (STO sto){
		IntType inttype = new IntType();
		if(sto instanceof ErrorSTO)
			return;
		if (!(sto.getType().isAssignableTo(inttype))) {
			m_nNumErrors++;
			m_errors.print(Formatter.toString(ErrorMsg.error7_Exit,
					(sto.getType()).getName()));
		}
		else {
			m_asm.asmExitStmt(sto);
		}
	}

	//----------------------------------------------------------------
	// Cin statements
	//----------------------------------------------------------------

	void DoCin(STO sto){
		if(sto instanceof ErrorSTO)
			return;
		if(!sto.isModLValue()) return;
		if(!(sto.getType() instanceof IntType || sto.getType() instanceof FloatType))
			return;
		m_asm.asmCin(sto);
	}
}
