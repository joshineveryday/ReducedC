//---------------------------------------------------------------------
// CSE 131 Reduced-C Compiler Project
// Copyright (C) 2008-2015 Garo Bournoutian and Rick Ord
// University of California, San Diego
//---------------------------------------------------------------------

abstract class STO
{
	private String m_strName;
	private Type m_type;
	private boolean m_isAddressable;
	private boolean m_isModifiable;
	protected boolean isRef; 
	protected boolean forceModLVal;
	protected String offset;
	protected String structOffset;
	protected boolean isDotted;
	protected String base;
	protected boolean arrayDeref = false;
	protected boolean ptrDeref;


	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public STO(String strName)
	{
		this(strName, null);
	}
 
	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public STO(String strName, Type typ)
	{
		setName(strName);
		setType(typ);
		setIsAddressable(false);
		setIsModifiable(false);
		setIsRef(false);
		setForceLVal(false);
		setIsDotted(false);
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public String getName()
	{
		return m_strName;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public void setName(String str)
	{
		m_strName = str;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public Type getType()
	{
		return m_type;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public void setType(Type type)
	{
		m_type = type;
	}

	//----------------------------------------------------------------
	// Addressable refers to if the object has an address. Variables
	// and declared constants have an address, whereas results from 
	// expression like (x + y) and literal constants like 77 do not 
	// have an address.
	//----------------------------------------------------------------
	public boolean getIsAddressable()
	{
		return m_isAddressable;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public void setIsAddressable(boolean addressable)
	{
		m_isAddressable = addressable;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public boolean getIsModifiable()
	{
		return m_isModifiable;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public void setIsModifiable(boolean modifiable)
	{
		m_isModifiable = modifiable;
	}

	//----------------------------------------------------------------
	// A modifiable L-value is an object that is both addressable and
	// modifiable. Objects like constants are not modifiable, so they 
	// are not modifiable L-values.
	//----------------------------------------------------------------
	public String getOffsetAddr()
	{
		return offset;
	}

	public String getStructOffset() {
		return structOffset;
	}

	public void setStructOffset(String _structOffset) {
		structOffset = _structOffset;
	}

	public void setOffsetAddr(String _offset)
	{
		offset = _offset;	
	}

	public String getBaseAddr()
	{
		return base;
	}

	public void setBaseAddr(String _base)
	{
		base = _base;
	}

	public boolean isDotted()
	{
		return isDotted;
	}

	public void setIsDotted(boolean _isDotted)
	{
		isDotted = _isDotted;
	}

	public boolean isModLValue()
	{
		return (getIsModifiable() && getIsAddressable()) || (getForceLVal());
	}

	//----------------------------------------------------------------
	//	It will be helpful to ask a STO what specific STO it is.
	//	The Java operator instanceof will do this, but these methods 
	//	will allow more flexibility (ErrorSTO is an example of the
	//	flexibility needed).
	//----------------------------------------------------------------
	public boolean isVar() { return false; }
	public boolean isConst() { return false; }
	public boolean isExpr() { return false; }
	public boolean isFunc() { return false; }
	public boolean isStructdef() { return false; }
	public boolean isError() { return false; }

	public void setIsRef(boolean refFlag) { isRef = refFlag; }
	public boolean isRef() { return isRef; }

	public void setForceLVal(boolean forceModL) { forceModLVal = forceModL; }
	public boolean getForceLVal() { return forceModLVal; }

	public void setArrayDeref(Boolean b){
		arrayDeref = b;
	}

	public boolean isArrayDeref(){
		return false;
	}
	public boolean isPtrDeref(){ return ptrDeref;}
	public void setPtrDeref(boolean val){ ptrDeref = val;}

}
