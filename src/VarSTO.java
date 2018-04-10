//---------------------------------------------------------------------
// CSE 131 Reduced-C Compiler Project
// Copyright (C) 2008-2015 Garo Bournoutian and Rick Ord
// University of California, San Diego
//---------------------------------------------------------------------

class VarSTO extends STO
{
	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public VarSTO(String strName)
	{
		super(strName);
		setIsAddressable(true);
		setIsModifiable(true);
		setIsRef(false);
	}

	public VarSTO(String strName, Type typ)
	{
		super(strName, typ);
		setIsAddressable(true);
		setIsModifiable(true);
		setIsRef(false);
	}

	public VarSTO(String strName, Type typ, Boolean refFlag) {
		super(strName, typ);
		setIsAddressable(true);
		setIsModifiable(true);
		setIsRef(refFlag);
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public boolean isVar() 
	{
		return true;
	}

	public void setArrayDeref(Boolean b){
		arrayDeref = b;
	}

	public boolean isArrayDeref(){
		return arrayDeref;
	}
}
