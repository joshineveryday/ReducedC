//---------------------------------------------------------------------
// CSE 131 Reduced-C Compiler Project
// Copyright (C) 2008-2015 Garo Bournoutian and Rick Ord
// University of California, San Diego
//---------------------------------------------------------------------

class ErrorPrinter
{
	private	Lexer		m_lexer;
	private boolean		m_lineon;

	public ErrorPrinter(Lexer lexer, boolean lineon)
	{
		m_lexer = lexer;
		m_lexer.setErrorPrinter(this);
		m_lineon = lineon;
	}

	public void print(String strMsg)
	{
		print(strMsg, 0);
	}

	public void print(String strMsg, int nOffset)
	{
		if(m_lineon)
		{
            System.out.println(
                "Error, \"" +
                m_lexer.getEPFilename() +
                "\", line " +
                (m_lexer.getLineNumber() + nOffset) + ": "
            );

            System.out.println("  " + strMsg);
		}
		else
		{
		    System.out.println("Error, \"" + 
			    m_lexer.getEPFilename() + 
			    "\": "
            );

		    System.out.println("  " + strMsg);
		}
	}
}
