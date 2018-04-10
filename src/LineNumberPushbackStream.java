//---------------------------------------------------------------------
// CSE 131 Reduced-C Compiler Project
// Copyright (C) 2008-2015 Garo Bournoutian and Rick Ord
// University of California, San Diego
//---------------------------------------------------------------------

import java.io.*;


class LineNumberPushbackStream extends PushbackInputStream
{
	//-----------------------------------------------------------------
	//
	//-----------------------------------------------------------------
	public 
	LineNumberPushbackStream ()
	{
		this (System.in);
		m_streamName = "(stdin)";
	}

	public 
	LineNumberPushbackStream (String strFilename)
	throws FileNotFoundException
	{
		this (new FileInputStream (strFilename));
		m_streamName = strFilename;
	}

	public 
	LineNumberPushbackStream (InputStream in)
	{
		//	You should never push back more than 3 characters
		//	(12e-X) but double up to be safe.
		super (in, 6);

		m_nLine = 1;
		m_streamName = "";
	}

	//-----------------------------------------------------------------
	//
	//-----------------------------------------------------------------
	public String
	getName ()
	{
		return	m_streamName;
	}


	//-----------------------------------------------------------------
	//
	//-----------------------------------------------------------------
	public int
	getLineNumber ()
	{
		return (m_nLine);
	}


	//-----------------------------------------------------------------
	//
	//-----------------------------------------------------------------
	public void
	incLineNumber ()
	{
		m_nLine++;
	}


	//-----------------------------------------------------------------
	//
	//-----------------------------------------------------------------
	public void
	decLineNumber ()
	{
		m_nLine--;
	}


	//-----------------------------------------------------------------
	//
	//-----------------------------------------------------------------
	public int
	read () 
	throws IOException
	{
		int	nextChar = super.read();

		if (nextChar == '\n')
			incLineNumber();

		return	nextChar;
	}


	//-----------------------------------------------------------------
	//
	//-----------------------------------------------------------------
	public void
	unread (int b) 
	throws IOException
	{
		super.unread (b);

		if (b == '\n')
			decLineNumber();
	}	


//-----------------------------------------------------------------
//	Instance variables.
//-----------------------------------------------------------------
	private int	m_nLine;

	//	I'm saving the name directly because I cannot see
	//	how to get the filename out of a created FileInputStream.
	//	Either I'm stupid or I'm totally missing something.
	private String	m_streamName;
}
