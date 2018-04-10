//---------------------------------------------------------------------
// CSE 131 Reduced-C Compiler Project
// Copyright (C) 2008-2015 Garo Bournoutian and Rick Ord
// University of California, San Diego
//---------------------------------------------------------------------

import java.util.Vector;
class FuncSTO extends STO
{
	private Type m_returnType;

	private Vector<Type> m_returnTypes;

	public Vector<Vector<STO>> paramLists; 
	public Vector<STO> curParams;
	private int m_topLvl;
	private boolean hasTopLvlReturn;
	public boolean hadOverload;
	public int offsetAddr;
	public boolean isCTor;
	public String structName;


	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public FuncSTO(String strName)
	{
		super (strName);
		setReturnType(null);
		//TODO ask if this should be conditional for when it also
		//returns by ref 
		setIsAddressable(true);
		setIsModifiable(false);
		setIsRef(false);
		paramLists = new Vector();
		m_topLvl = 0;
		hasTopLvlReturn = false;
		hadOverload = false;
		setReturnType(new VoidType());
		m_returnTypes = new Vector();
	 	isCTor = false;
	 	structName = "";
	}

	public FuncSTO(String strName, boolean refFlag, Type retType)
	{
		this (strName);
		setReturnType(retType);
		setIsRef(refFlag);
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public boolean isFunc() 
	{ 
		return true;
		// You may want to change the isModifiable and isAddressable                      
		// fields as necessary
	}


	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public void addReturnType(Type typ) {
		m_returnTypes.addElement(typ);
	}


	//----------------------------------------------------------------
	// find which return types corresponds to parameters and set it as the current return type
	//This is a helper function for checkAllParams
	//----------------------------------------------------------------
	private void setReturnTypeFromNdx(int ndx) {
		setReturnType(m_returnTypes.get(ndx));
	}

	//----------------------------------------------------------------
	// This is the return type of the function. This is different from 
	// the function's type (for function pointers - which we are not 
	// testing in this project).
	//----------------------------------------------------------------
	public void setReturnType(Type typ)
	{
		m_returnType = typ;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public Type getReturnType ()
	{
		return m_returnType;
	}


	//----------------------------------------------------------------

	//----------------------------------------------------------------
	public void setTopLevel(int lvl)
	{
		m_topLvl = lvl;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public int getTopLevel ()
	{
		return m_topLvl;
	}

	//----------------------------------------------------------------

	//----------------------------------------------------------------
	public void setHasTopLvlRet(boolean hasLvl)
	{
		hasTopLvlReturn = hasLvl;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public boolean getHasTopLvlRet ()
	{
		return hasTopLvlReturn;
	}



	//----------------------------------------------------------------
	// Return the argSize if the number of arguments matches the number
	// of parameters for any of the fxn definitions
	// Return 
	//----------------------------------------------------------------
	public boolean checkNumArgs(Vector<STO> args, Vector<STO> params) {
		if((args == null) && (params.size() == 0)) return true;
		if(args == null && params.size() > 0) return false;

		if(args.size() == params.size()) {
			return true;
		} else {
			return false;
		}
	}

	//----------------------------------------------------------------
	// checks to see if the params exist in the param list.
	// Check that the order and types of the parameters are the same
	// passing by reference is ignored
	//----------------------------------------------------------------

	public boolean hasParams(Vector<STO> newParamList) {
		if(paramLists == null) return false;
		if(paramLists.size() == 0) return false;

	 	boolean doskip = false;
	 	for(Vector<STO> paramList : paramLists) {
	 		doskip = false;
	 		if(newParamList == null && paramList.size() == 0) return true;
	 		if(newParamList == null && paramList.size() > 0) continue;

	 		if(newParamList.size() == 0 && paramList.size() == 0) return true;

	 		if(newParamList.size() != paramList.size()) {
	 			//size mismatch, skip to the next param list
	 			continue;	
	 		}

	 		for(int i = 0; i<paramList.size(); i++) {
	 			//sizes are equal check the types in the param list
	 			Type newParamType = newParamList.get(i).getType();
	 			Type existingParamType = paramList.get(i).getType();
	 			if(!(newParamType.isEquivalentTo(existingParamType))) {
	 				doskip = true;
	 			}
	 		}
	 		if(doskip) continue;
	 		return true;
	 	}
	 	return false;
	}

	//Check the arguments of a function against it's parameters. 
	//Use this for non-overloaded calls
	public Vector<STO> checkParams(Vector<STO> args, Vector<STO> params) {
		//Only check first element for now,
		//may need to change for overloading later
		Vector<STO> errorSTOs = new Vector();

		boolean had_error = false;
		for(int i=0; i<params.size(); i++) {

			had_error = false;
			STO param = params.get(i);
			String paramID = param.getName();
			STO arg = args.get(i);


			if(param instanceof ErrorSTO || arg instanceof ErrorSTO) continue;

			if(param.isRef()) {
				if(!arg.getType().isEquivalentTo(param.getType()) && !had_error) {
					//ref argument type not equiv to parameter type
					String argTyp = arg.getType().getName();
					String paramTyp = param.getType().getName();
					had_error = true;
					errorSTOs.addElement(new ErrorSTO(Formatter.toString(ErrorMsg.error5r_Call, argTyp, paramID, paramTyp)));
				}

				if(!arg.isModLValue() && !(arg.getType() instanceof ArrayType) && !had_error) {
					//ref argument is not a modifiable L value
					String paramTyp = param.getType().getName();
					had_error = true;
					errorSTOs.addElement(new ErrorSTO(Formatter.toString(ErrorMsg.error5c_Call, paramID, paramTyp)));
				} 

			} else {
				boolean assignorequiv;
				if(!isOverloaded()) {
					assignorequiv = arg.getType().isAssignableTo(param.getType());
				} else {
					assignorequiv = arg.getType().isEquivalentTo(param.getType());
				}

				if(!assignorequiv && !had_error) {
					//value arg is not assignable to param
					String argTyp = arg.getType().getName();
					String paramTyp = param.getType().getName();
					had_error = true;
					errorSTOs.addElement(new ErrorSTO(Formatter.toString(ErrorMsg.error5a_Call, argTyp, paramID, paramTyp)));
				}	
			}
		}

		if(errorSTOs.size() == 0) curParams = params;

		return errorSTOs;
	}

	public boolean checkHasErrorSTO(Vector<STO> args, Vector<STO> params) {
		boolean ret = false; 

		if(args == null) return ret;
		for(STO arg : args) {
			if(arg instanceof ErrorSTO) ret = true;
		}


		if(params == null) return ret;
		for(STO param : params) {
			if(param instanceof ErrorSTO) ret = true;
		}

		return ret;
	}

	//Use this to check parameters of overloaded functions
	public STO checkAllParamLists(Vector<STO> args) {
		boolean has_match = false;
		for(int i=0; i < paramLists.size(); i++) {
			Vector<STO> paramList = paramLists.get(i);
			if(!checkNumArgs(args, paramList)) continue;
			Vector<STO> paramCheck = checkParams(args, paramList);
			boolean hasErrorSTO = checkHasErrorSTO(args, paramList);
			if(paramCheck.size() == 0 && !hasErrorSTO) { 
				setReturnTypeFromNdx(i);
				curParams = paramList;
				has_match = true;
			}
		}
		if(has_match) {
			return new ExprSTO("OVERLOADED_PARAM_SUCCESS");
		}
		return new ErrorSTO(Formatter.toString(ErrorMsg.error9_Illegal, this.getName()));
	}

	public boolean isOverloaded() {
		return (paramLists.size() > 1)? true : false;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public String mangledName(Vector<STO> params) {
		String mangledName = getName();
		if(structName.length() > 0) {
			mangledName = structName + "." + mangledName;
		} 

		if(params != null && params.size() > 0) {
			for(STO funcParam : params) {
				if(funcParam instanceof ErrorSTO) continue;
				mangledName = mangledName + "." + funcParam.getType().getName();
			}
		}
		else {
			mangledName = mangledName + ".void"; 
		}

		//Had regex problems so using replace instead of replaceAll
		mangledName = mangledName.replace('*', '$');
		mangledName = mangledName.replace('[', '$');
		mangledName = mangledName.replace(']', '$');
		mangledName = mangledName.replace('~', '$');

		return mangledName;

	}
}