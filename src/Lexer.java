//---------------------------------------------------------------------
// CSE 131 Reduced-C Compiler Project
// Copyright (C) 2008-2015 Garo Bournoutian and Rick Ord
// University of California, San Diego
//---------------------------------------------------------------------

import 	java.util.*;
import	java.io.*;

class Lexer
{
	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public 
	Lexer (Vector<String> filenames)	
	{
		//	We'll need to keep track of all files ever read to
		//	make sure no file is included twice.
		m_lstFiles = new Vector<String> ();

		//	These are the input files to read from
		m_stkInputs = new Stack<LineNumberPushbackStream> ();

		//	If there are no files in the stack, default to read
		//	from standard input.
		if (filenames.isEmpty())
			m_stkInputs.push (new LineNumberPushbackStream ());
		else
		{
			for (Enumeration<String> e = filenames.elements(); e.hasMoreElements(); )
				addAFile (e.nextElement(), false);
		}

		//	Finally, load up the RC keywords to compare against
		//	for IDs.
		loadKeywords ();
	}


	//----------------------------------------------------------------
	//	This gets the next token from the input stream.  This method
	//	should start with a lower case but CUP calls it directly and
	//	that's what it wants.
	//----------------------------------------------------------------
	public Token
	GetToken ()
	{
		Token		token = null;

		while (token == null)
		{
			token = getAToken ();

			//	If not an ID, this can go out right away
			if (token.GetCode () != sym.T_ID)
				return (token);

			//	Otherwise, check for RC includes first
			if (!token.GetLexeme ().equals ("INCLUDE"))
				return (token);

			token = getAToken ();
			if (token.GetCode () != sym.T_STR_LITERAL)
				m_errors.print ("illegal include directive \"" +
						token.GetLexeme () + "\"");
			else
				addAFile (token.GetLexeme (), true);

			token = null;
		}

		return (null);
	}


	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	private Token
	getAToken ()
	{
		Token		token = null;
		char		c;

		while (token == null)
		{
			c = getChar ();

			if (c == 0)
				token = new Token (sym.EOF, "");

			else if (Character.isDigit (c))
				token = getNumToken (c);

			else if (Character.isLetter (c) || c == '_')
				token = getAlphaToken (c);

			else if (c == '"' || c == '\'')
				token = getStrLitToken (c);	

			else if (c == '\n' || c == '\t' || c == ' ' || c == '\r')
			{
				//	Ignore all whitespace
			}
			else 
				token = getPunctToken (c);
		}

		return (token);
	}


	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	private Token
	getAlphaToken (char cFirst)
	{
		StringBuffer		buffer = new StringBuffer (MAXIDLEN);
		int 			nCount = 0;
		char			c;
		boolean			bDone = false;
		String			strLexeme;
		int 			nKeyword;

		buffer.append (cFirst);
		nCount++;
		while (!bDone)
		{
			c = getChar ();
			if (Character.isLetter (c) || Character.isDigit (c) || c == '_')
			{
				if (nCount < MAXIDLEN)
				{
					buffer.append (c);
				}
				else if (nCount == MAXIDLEN)
				{
					m_errors.print ("identifier too long");
				}
				else
				{
				}
				nCount++;
			}
			else
			{
				ungetChar (c);
				bDone = true;
			}
		}

		strLexeme = new String (buffer);
		if ((nKeyword = lookupKeyword (strLexeme)) != 0)
			return (new Token (nKeyword, new String (buffer)));
		else if (strLexeme.equals(strLexeme.toUpperCase()))	// all caps?
			return (new Token (sym.T_ID_U, new String (buffer)));
		else
			return (new Token (sym.T_ID, new String (buffer)));
	}


	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	private Token
	getNumToken (char cFirst)
	{
		int 			nState;
		char			c;
		StringBuffer		buffer = new StringBuffer (20);
		int 			nCount;
		int 			nExpCount = 0;
		Token			token = null;
		boolean			bAddChar;
		boolean 		bError = false;
		boolean			bExpError = false;

		final int 		S_ZEROES = 0;
		final int		S_DIGITS = 2;
		final int		S_MANTISSA = 4;
		final int		S_EXP = 5;
		final int		S_EXPSIGN = 6;
		final int		S_EXPONENT = 7;
		final int		S_OCTAL = 8;
		final int		S_HEX = 9;


		if (cFirst == '0')
		{
			nState = S_ZEROES;
			nCount = 0;
		}
		else
		{
			nState = S_DIGITS;

			buffer.append (cFirst);
			nCount = 1;
		}

		while (token == null)
		{
			c = getChar ();
			bAddChar = true;

      boolean switchThisChar = true;

      if (nState == S_ZEROES)
      {
        switchThisChar = false;

        while (c == '0')
        {
          c = getChar();
        }
        
        if (c == '.')
        {
          nState = S_MANTISSA;
        }
        else if (isOctal(c))
        {
          buffer.append('0');
          switchThisChar = true;
          nState = S_OCTAL;
        }
        else if (c == 'x' || c == 'X')
        {
          buffer.append('0');
          nState = S_HEX;
        }
        else
        {
          ungetChar (c);
          token = new Token (sym.T_INT_LITERAL, "0");
          break;
        }
      }

      if (switchThisChar)
      {
			  switch (nState)
        {
          case S_DIGITS:

            if (c == '.')
            {
              nState = S_MANTISSA;
            }
            else if (Character.isDigit (c))
            {
              nState = S_DIGITS;
            }
            else
            {
              ungetChar (c);
              token = new Token (sym.T_INT_LITERAL, 
                  new String (buffer));
            }
            break;


          case S_MANTISSA:
            
            if (c == 'E' || c == 'D')
            {
              nCount = 0;
              nState = S_EXP;
            }
            else if (Character.isDigit (c))
            {
              nState = S_MANTISSA;
            }
            else
            {
              ungetChar (c);
              token = new Token (sym.T_FLOAT_LITERAL, 
                  new String (buffer));
            }
            break;


          case S_HEX:
            
            if (Character.isDigit (c) || isHex (c))
            {
              nState = S_HEX;
            }
            else
            {
              ungetChar (c);
              Integer bufInt = Integer.parseInt(buffer.substring(2),16);
              buffer = new StringBuffer(bufInt.toString());
              token = new Token (sym.T_INT_LITERAL,
                  new String (buffer));
            }
            break;


          case S_OCTAL:
            
            if (isOctal(c))
            {
              nState = S_OCTAL;
            }
            else
            {
              ungetChar (c);
              Integer bufInt = Integer.parseInt(buffer.toString(), 8);
              buffer = new StringBuffer(bufInt.toString());
              token = new Token (sym.T_INT_LITERAL,
                  new String (buffer));
            }
            break;


          case S_EXP:
            
            bAddChar = false;
            if (c == '-' || c == '+')
            {
              buffer.append (c);
              nState = S_EXPSIGN;
            }
            else if (c == '0')
            {
              nState = S_EXPONENT;
              nExpCount = 0;
            }
            else if (Character.isDigit (c))
            {
              buffer.append (c);
              nState = S_EXPONENT;
              nExpCount = 1;
            }
            else
            {
              ungetChar (c);
              c = buffer.charAt (buffer.length () - 1);
              ungetChar (c);
              buffer.deleteCharAt (buffer.length () - 1);
              token = new Token (sym.T_FLOAT_LITERAL,
                  new String (buffer));		
            }
            break;

          
          case S_EXPSIGN:
              
            bAddChar = false;
            if (c == '0')
            {
              nState = S_EXPONENT;
              nExpCount = 0;
            }
            else if (Character.isDigit (c))
            {
              buffer.append (c);
              nState = S_EXPONENT;
              nExpCount = 1;
            }
            else
            {
              ungetChar (c);
              c = buffer.charAt (buffer.length () - 1);
              ungetChar (c);
              buffer.deleteCharAt (buffer.length () - 1);
              c = buffer.charAt (buffer.length () - 1);
              ungetChar (c);
              buffer.deleteCharAt (buffer.length () - 1);
              token = new Token (sym.T_FLOAT_LITERAL,
                  new String (buffer));		
            }
            break;


          case S_EXPONENT:

            bAddChar = false;
            if (c == '0' && nExpCount == 0)
            {
              nState = S_EXPONENT;
            }
            else if (Character.isDigit (c))
            {
              nExpCount++;
              if (nExpCount <= MAXEXPLEN)
              {
                buffer.append (c);
              }
              else
              {
                bExpError = true;
              }
            }
            else
            {
              ungetChar (c);
              if (nExpCount == 0)
                buffer.append ('0');
              token = new Token (sym.T_FLOAT_LITERAL,
                  new String (buffer));		
            }
         }
      }

			if (bAddChar)
			{
				if (nCount < MAXNUMLEN)
				{
					buffer.append (c);
				}
				else if (nCount == MAXNUMLEN)
				{
				}
				else
				{
					bError = true;
				}
				nCount++;
			}
		}

		
		if (bError)
		{
			if (token.GetCode () == sym.T_FLOAT_LITERAL)
			{
				m_errors.print ("float literal (mantissa) too long");
			}
			else
			{
				m_errors.print ("integer literal too long");
			}
		}

		if (bExpError)
		{
			m_errors.print ("float literal (exponent) too long");
		}
		
    return (token);
	}


	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	private Token
	getPunctToken (char cFirst)
	{
		Token		token = null;
		char		c;

		switch (cFirst)
		{
			case '&':
				if ((c = getChar ()) == '&')
				{
					token = new Token (sym.T_AND, "&&");
				}
				else
				{
					ungetChar (c);
					token = new Token (sym.T_AMPERSAND, "&");
				}
				break;

			case ':':
				c = getChar();
				if(c == ':')
				{
					token = new Token (sym.T_COLONCOLON, "::");
				}
				else
				{
					ungetChar (c);
					token = new Token (sym.T_COLON, ":");
				}
				break;


			case '=':
				if ((c = getChar ()) == '=')
				{
					token = new Token (sym.T_EQU, "==");
				}
				else
				{
					ungetChar (c);
					token = new Token (sym.T_ASSIGN, "=");
				}
				break;

			case '.':
				token = new Token (sym.T_DOT, ".");
				break;

			case '|':
				if ((c = getChar ()) == '|')
				{
					token = new Token (sym.T_OR, "||");
				}
				else
				{
					ungetChar (c);
					token = new Token (sym.T_BAR, "|");
				}
				break;

			case ',':
				token = new Token (sym.T_COMMA, ",");
				break;

			case '>':
				c = getChar();
				if(c == '>')
				{
					token = new Token (sym.T_ISTREAM, ">>");
				}
				else if (c == '=')
				{
					token = new Token (sym.T_GTE, ">=");
				}
				else
				{
					ungetChar (c);
					token = new Token (sym.T_GT, ">");
				}
				break;

			case '{':
				token = new Token (sym.T_LBRACE, "{");
				break;

			case '[':
				token = new Token (sym.T_LBRACKET, "[");
				break;

			case '(':
				token = new Token (sym.T_LPAREN, "(");
				break;

			case '<':
				c = getChar();
				if(c == '<')
				{
					token = new Token (sym.T_OSTREAM, "<<");
				}
				else if (c == '=')
				{
					token = new Token (sym.T_LTE, "<=");
				}
				else
				{
					ungetChar (c);
					token = new Token (sym.T_LT, "<");
				}
				break;

			case '-':
				c = getChar();
				if(c == '>')
				{
					token = new Token (sym.T_ARROW, "->");
				}
				else if(c == '-')
				{
					token = new Token (sym.T_MINUSMINUS, "--");
				}
				else
				{
					ungetChar (c);
					token = new Token (sym.T_MINUS, "-");
				}
				break;

			case '!':
				c = getChar();
				if(c == '=')
				{
					token = new Token (sym.T_NEQ, "!=");
				}
				else
				{
					ungetChar (c);
					token = new Token (sym.T_NOT, "!");
				}
				break;

			case '+':
				c = getChar();
				if(c == '+')
				{
					token = new Token (sym.T_PLUSPLUS, "++");
				}
				else
				{
					ungetChar (c);
					token = new Token (sym.T_PLUS, "+");
				}
				break;

			case '}':
				token = new Token (sym.T_RBRACE, "}");
				break;

			case ']':
				token = new Token (sym.T_RBRACKET, "]");
				break;

			case ')':
				token = new Token (sym.T_RPAREN, ")");
				break;

			case ';':
				token = new Token (sym.T_SEMI, ";");
				break;

			case '/':
				if ((c = getChar ()) == '*')
				{
					readComment ();
				}
				else if (c == '/')
				{
					readCommentLn ();
				}
				else
				{
					ungetChar (c);
					token = new Token (sym.T_SLASH, "/");
				}
				break;

			case '%':
				token = new Token (sym.T_MOD, "%");
				break;

			case '*':
				token = new Token (sym.T_STAR, "*");
				break;

			case '^':
				token = new Token (sym.T_CARET, "^");
				break;

			case '~':
				token = new Token (sym.T_TILDE, "~");
				break;

			default:
				m_errors.print ("unknown character '" + cFirst + "'");
				break;
		}

		return (token);
	}


	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	private void
	readComment ()
	{
		int 		nCount = 1;
		boolean		bSlash = false, bStar = false;
		char		c;

		while (nCount > 0)
		{
			c = getChar ();
			switch (c)
			{
				case 0:
					m_errors.print ("unterminated comment");
					nCount = 0;
					ungetChar (c);
					break;

				case '/':
					if (bStar)
					{
						nCount--;
					}
					else
					{
						bSlash = true;
					}
					bStar = false;
					break;

				case '*':
					if (bSlash)
					{
						nCount++;
						bStar = false;
					}
					else
					{
						bStar = true;
					}
					bSlash = false;
					break;
					
				default:
					bStar = bSlash = false;
					break;
			}
		}
	}


	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	private void
	readCommentLn ()
	{
		char		c;

		while (true)
		{
			c = getChar ();
			if(c == '\n'  || c == '\0')
				break;
		}
	}


	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	private Token
	getStrLitToken (char cFirst)
	{
		char			c;
		int 			nCount = 0;
		StringBuffer	buffer = new StringBuffer (MAXSTRLEN);
		boolean			bDone = false;

		while (!bDone)
		{
			c = getChar ();

			if (c == 0)
			{
				m_errors.print ("EOF in string literal");
				bDone = true;
			}
			else if (c == '\n')
			{
				m_errors.print ("newline in string literal", -1);
				bDone = true;
			}
			else if (c == cFirst)
			{
				bDone = true;
			}
			else
			{
				if (nCount < MAXSTRLEN)
				{
					buffer.append (c);
				}
				else if (nCount == MAXSTRLEN)
				{
					m_errors.print ("string literal too long");
				}
				else
				{
				}
				nCount++;
			}
		}

		return (new Token (sym.T_STR_LITERAL, new String (buffer)));
	}


	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	private boolean
	isHex (char c)
	{
    c = Character.toUpperCase(c);
		return (c == 'A' || c == 'B' || c == 'C' ||
		        c == 'D' || c == 'E' || c == 'F');
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	private boolean
	isOctal (char c)
	{
		return (c == '0' || c == '1' || c == '2' ||
		        c == '3' || c == '4' || c == '5' ||
		        c == '6' || c == '7');
	}


	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	private int
	lookupKeyword (String strLexeme)
	{
		Integer		n;

		if ((n = m_htKeywords.get (strLexeme)) != null)
			return (n.intValue ());
		else
			return (0);
	}


	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	private void
	loadKeywords ()
	{
		m_htKeywords = new Hashtable<String, Integer> ();
		m_htKeywords.put ("new", new Integer (sym.T_NEW));
		m_htKeywords.put ("delete", new Integer (sym.T_DELETE));
		m_htKeywords.put ("bool", new Integer (sym.T_BOOL));
		m_htKeywords.put ("break", new Integer (sym.T_BREAK));
		m_htKeywords.put ("cin", new Integer (sym.T_CIN));
		m_htKeywords.put ("continue", new Integer (sym.T_CONTINUE));
		m_htKeywords.put ("cout", new Integer (sym.T_COUT));
		m_htKeywords.put ("const", new Integer (sym.T_CONST));
		m_htKeywords.put ("else", new Integer (sym.T_ELSE));
		m_htKeywords.put ("endl", new Integer (sym.T_ENDL));
		m_htKeywords.put ("exit", new Integer (sym.T_EXIT));
		m_htKeywords.put ("extern", new Integer (sym.T_EXTERN));
		m_htKeywords.put ("false", new Integer (sym.T_FALSE));
		m_htKeywords.put ("float", new Integer (sym.T_FLOAT));
		m_htKeywords.put ("foreach", new Integer (sym.T_FOREACH));
		m_htKeywords.put ("function", new Integer (sym.T_FUNCTION));
		m_htKeywords.put ("if", new Integer (sym.T_IF));
		m_htKeywords.put ("int", new Integer (sym.T_INT));
		m_htKeywords.put ("nullptr", new Integer (sym.T_NULLPTR));
		m_htKeywords.put ("return", new Integer (sym.T_RETURN));
		m_htKeywords.put ("sizeof", new Integer (sym.T_SIZEOF));
		m_htKeywords.put ("static", new Integer (sym.T_STATIC));
		m_htKeywords.put ("structdef", new Integer (sym.T_STRUCTDEF));
		m_htKeywords.put ("this", new Integer (sym.T_THIS));
		m_htKeywords.put ("true", new Integer (sym.T_TRUE));
		m_htKeywords.put ("void", new Integer (sym.T_VOID));
		m_htKeywords.put ("while", new Integer (sym.T_WHILE));
	}


	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public int
	getLineNumber ()
	{
		if (m_stkInputs.isEmpty())
			return 0;

		LineNumberPushbackStream		
			input = m_stkInputs.peek ();
		return (input.getLineNumber());
	}


	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public String
	getEPFilename ()
	{
		if (m_stkInputs.isEmpty())
			return "";
		
		LineNumberPushbackStream		
			input = m_stkInputs.peek ();
		return (input.getName());
	}


	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	private boolean
	addAFile (String strFile, boolean bInclude)
	{
		//	First, see if the file exists
		LineNumberPushbackStream		stream;
                try
                {
                     	stream = new LineNumberPushbackStream (strFile);

			//	Ok, now see if it has been included already
			if (m_lstFiles.indexOf(strFile) > 0)
			{
				if (bInclude)
					m_errors.print ("multiple included file \"" +
							strFile + "\"");
				else
					System.out.println ("multiple read file \"" +
							strFile + "\"");
				return (false);
			}

			m_stkInputs.push (stream);
			m_lstFiles.addElement (strFile);
			
                } catch (FileNotFoundException e)
                {
			if (bInclude)
				m_errors.print ("bad include file \"" +
					strFile + "\"");
			else
				System.out.println ("Error, " + 
					e.getMessage());
			return	(false);
                }

		return	(true);
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	private char
	getChar ()
	{
		int		c = 0;
		LineNumberPushbackStream	
				input;

		while (!m_stkInputs.isEmpty() && c <= 0)
		{
			input = m_stkInputs.peek ();

			try {
				c = input.read ();
			} catch (java.io.IOException e) {
				e.printStackTrace();	
				c = 0;
			}
			
			//	If at end of file, you can get rid of the file
			if (c <= 0)
				m_stkInputs.pop();
		}

		if (c < 0)
			c = 0;

		return ((char)c);
	}


	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	private void
	ungetChar (char c)
	{
		if (m_stkInputs.isEmpty())
			return;

		LineNumberPushbackStream 	input = 
			m_stkInputs.peek();

		try {
			input.unread (c);
		} catch (java.io.IOException e) {
			e.printStackTrace();	
		}
	}


	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public void
	setErrorPrinter (ErrorPrinter ep)
	{
		m_errors = ep;
	}


//----------------------------------------------------------------
//	Instance variables
//----------------------------------------------------------------
	//	Some RC constants
	private int 			MAXIDLEN = 40;
	private int 			MAXNUMLEN = 10;
	private int 			MAXSTRLEN = 80;
	private int 			MAXCHARLEN = 3;
	private int 			MAXEXPLEN = 3;

	//	This error printer will format line numbers and file
	//	names and whatever else appropriately.
	private ErrorPrinter		m_errors;

	//	This is the list of RC keywords.
	private Hashtable<String, Integer>		m_htKeywords;

	//	This is the list of input files to read from.
	private Stack<LineNumberPushbackStream>			m_stkInputs;
	//	This is the list of files that we've read so far.  We must
	//	keep track of this to not read a file twice.
	private Vector<String>			m_lstFiles;
}
