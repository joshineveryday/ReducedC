//---------------------------------------------------------------------
// CSE 131 Reduced-C Compiler Project
// Copyright (C) 2008-2015 Garo Bournoutian and Rick Ord
// University of California, San Diego
//---------------------------------------------------------------------

import java.util.*;

class SymbolTable
{
	private Stack<Scope> m_stkScopes;
	private int m_nLevel;
	private Scope m_scopeGlobal;
	private FuncSTO m_func = null;
	private StructdefSTO  m_struct = null;
    
	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public SymbolTable()
	{
		m_nLevel = 0;
		m_stkScopes = new Stack<Scope>();
		m_scopeGlobal = null;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public void insert(STO sto)
	{
		Scope scope = m_stkScopes.peek();
		scope.InsertLocal(sto);
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public STO accessGlobal(String strName)
	{
		return m_scopeGlobal.access(strName);
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public STO accessLocal(String strName)
	{
		Scope scope = m_stkScopes.peek();
		return scope.accessLocal(strName);
	}

	//----------------------------------------------------------------
	// TODO: Remember to test this in check 3, This should now be LIFO
	//----------------------------------------------------------------
	public STO access(String strName)
	{
		Stack stk = new Stack();
		Scope scope;
		STO stoTemp = null;
		STO stoReturn = null;	

		for (Enumeration<Scope> e = m_stkScopes.elements(); e.hasMoreElements();)
		{
			scope = e.nextElement();
			if ((stoTemp = scope.access(strName)) != null)
				stoReturn = stoTemp;
		}

		return stoReturn;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public void openScope()
	{
		Scope scope = new Scope();

		// The first scope created will be the global scope.
		if (m_scopeGlobal == null)
			m_scopeGlobal = scope;

		m_stkScopes.push(scope);
		m_nLevel++;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public Scope getCurScope()
	{
		return m_stkScopes.peek();
	}


	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public void closeScope()
	{
		m_stkScopes.pop();
		m_nLevel--;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public int getLevel()
	{
		return m_nLevel;
	}
	
	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public boolean isGlobal() {
		return (m_nLevel == 1)? true : false;
	}

	//----------------------------------------------------------------
	//	This is the function currently being parsed.
	//----------------------------------------------------------------
	public FuncSTO getFunc() { return m_func; }
	public void setFunc(FuncSTO sto) { m_func = sto; }

	//----------------------------------------------------------------
	//	This is the struct currently being parsed.
	//----------------------------------------------------------------
	public StructdefSTO getStruct() { return m_struct; }
	public void setStruct(StructdefSTO sto) { m_struct = sto; }

}
