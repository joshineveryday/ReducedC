//---------------------------------------------------------------------
// CSE 131 Reduced-C Compiler Project
// Copyright (C) 2008-2015 Garo Bournoutian and Rick Ord
// University of California, San Diego
//---------------------------------------------------------------------

//---------------------------------------------------------------------
// This is the top of the Type hierarchy. You most likely will need to
// create sub-classes (since this one is abstract) that handle specific
// types, such as IntType, FloatType, ArrayType, etc.
//---------------------------------------------------------------------

abstract class Type
{
	// Name of the Type (e.g., int, bool, some structdef, etc.)
	private String m_typeName;
	protected int m_size;

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public Type(String strName, int size)
	{
		setName(strName);
		setSize(size);
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public String getName()
	{
		return m_typeName;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public void setName(String str)
	{
		m_typeName = str;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public int getSize()
	{
		return m_size;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	protected void setSize(int size)
	{
		m_size = size;
	}

	//----------------------------------------------------------------
	//	It will be helpful to ask a Type what specific Type it is.
	//	The Java operator instanceof will do this, but you may
	//	also want to implement methods like isNumeric(), isInt(),
	//	etc. Below is an example of isInt(). Feel free to
	//	change this around.
	//----------------------------------------------------------------
	public boolean isError()   { return false; }
	public boolean isBool() { return false; }
	public boolean isNumeric() { return false; }
	public boolean isFloat() { return false; }
	public boolean isInt() { return false; }
	public boolean isVoid() { return false; }
	public boolean isArray() { return false; }
	public boolean isPointer() { return false; }

	public boolean isAssignableTo(Type t) { return false; }
	public boolean isEquivalentTo(Type t) { return false; }
}
