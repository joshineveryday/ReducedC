//---------------------------------------------------------------------
// CSE 131 Reduced-C Compiler Project
// Copyright (C) 2008-2015 Garo Bournoutian and Rick Ord
// University of California, San Diego
//---------------------------------------------------------------------

import java.math.BigDecimal;

class ConstSTO extends STO
{
    //----------------------------------------------------------------
    //	Constants have a value, so you should store them here.
    //	Note: We suggest using Java's BigDecimal class, which can hold
    //	floats and ints. You can then do .floatValue() or 
    //	.intValue() to get the corresponding value based on the
    //	type. Booleans/Ptrs can easily be handled by ints.
    //	Feel free to change this if you don't like it!
    //----------------------------------------------------------------
    private BigDecimal		m_value;

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public ConstSTO(String strName)
	{
		super(strName);		
		setIsAddressable(true);
		setIsModifiable(false);
	}

	public ConstSTO(String strName, Type typ)
	{
		super(strName, typ);
		m_value = new BigDecimal(0); 
		setIsAddressable(true);
		setIsModifiable(false);
	}

	public ConstSTO(String strName, Type typ, int val)
	{
		super(strName, typ);		
		m_value = new BigDecimal(val);
		setIsAddressable(true);
		setIsModifiable(false);
	}
	
	//Only care about this Ctor in project 2
	public ConstSTO(String strName, Type typ, String val)
	{
		super(strName, typ);
		int int_val = 0;
		if(val == "true") int_val = 1;

		m_value = new BigDecimal(int_val);
		setIsAddressable(true);
		setIsModifiable(false);
	}

	public ConstSTO(String strName, Type typ, BigDecimal val)
	{
		super(strName, typ);
		m_value = val;
		setIsAddressable(true);
		setIsModifiable(false);
	}

	public ConstSTO(String strName, Type typ, double val)
	{
		super(strName, typ);
		m_value = new BigDecimal(val);
		setIsAddressable(true);
		setIsModifiable(false);
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public boolean isConst() 
	{
		return true;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public BigDecimal getValue() 
	{
		return m_value;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public int getIntValue() 
	{
		return m_value.intValue();
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public float getFloatValue() 
	{
		return m_value.floatValue();
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public boolean getBoolValue() 
	{
		return !BigDecimal.ZERO.equals(m_value);
	}
}
