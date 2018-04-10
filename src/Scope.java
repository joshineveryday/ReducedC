//---------------------------------------------------------------------
// CSE 131 Reduced-C Compiler Project
// Copyright (C) 2008-2015 Garo Bournoutian and Rick Ord
// University of California, San Diego
//---------------------------------------------------------------------

import java.util.Vector;

class Scope
{
	private Vector<STO> m_lstLocals;

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public Scope()
	{
		m_lstLocals = new Vector<STO>();
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public STO access(String strName)
	{
		return accessLocal(strName);
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public STO accessLocal(String strName)
	{
		STO sto = null;

		for (int i = 0; i < m_lstLocals.size(); i++)
		{
			sto = m_lstLocals.elementAt(i);

			if (sto.getName().equals(strName))
				return sto;
		}

		return null;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public void InsertLocal(STO sto)
	{
		m_lstLocals.addElement(sto);
	}


	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public Vector<STO> getScopeVec() {
		return m_lstLocals;
	}
}
