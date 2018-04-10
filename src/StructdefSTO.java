//---------------------------------------------------------------------
// CSE 131 Reduced-C Compiler Project
// Copyright (C) 2008-2015 Garo Bournoutian and Rick Ord
// University of California, San Diego
//---------------------------------------------------------------------

//---------------------------------------------------------------------
// For structdefs
//---------------------------------------------------------------------

class StructdefSTO extends STO
{
	public boolean hasCTor;
	public boolean hasDTor;

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public StructdefSTO(String strName)
	{
		super(strName);
		hasCTor = false;
		hasDTor = false;
	}

	public StructdefSTO(String strName, Type typ)
	{
		super(strName, typ);
		hasCTor = false;
		hasDTor = false;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public boolean isStructdef()
	{
		return true;
	}
}
