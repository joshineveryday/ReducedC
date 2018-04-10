//---------------------------------------------------------------------
// CSE 131 Reduced-C Compiler Project
// Copyright (C) 2008-2015 Garo Bournoutian and Rick Ord
// University of California, San Diego
//---------------------------------------------------------------------

import java.util.*;
import java.io.*;

/////////////////////////////////////////////////////////////////
//
// Provides string substitution within strings via templating mechanism.
//
// NOTE: Does not actually check types or format numbers.  The patterns are
// just suggestive documentation.
//
/////////////////////////////////////////////////////////////////

class Formatter
{
    static String patterns[] = { "%S", "%D", "%R", "%L", "%O", "%T", "%F" };

    /////////////////////////////////////////////////////////////////
    // finds indexOf the first occurring pattern.
    /////////////////////////////////////////////////////////////////
    private static int nextIndex(String template) 
    {
        int patPos = -1;
        int patNum;

        for (patNum = 0; patNum < patterns.length; patNum++) 
        {
            int pos = template.indexOf(patterns[patNum]);
            // least match so far
            if (pos != -1 && (patPos == -1 || pos < patPos))  
                    patPos = pos;
            // System.out.println(template + " " + patNum + " " 
            //	+ pos + " " + patPos);
        }

        return patPos;
    }

    /////////////////////////////////////////////////////////////////
    // many arguments 
    /////////////////////////////////////////////////////////////////
    public static String toString(String template, Iterator<String> strings)
    {
        // System.out.println(template + " " + strings);
        if (!strings.hasNext()) return template;

        String result;
        int patPos = nextIndex(template);

        result = template.substring(0, patPos);
        result += strings.next();
        result += toString(template.substring(patPos+2), strings);
        return result;
    }

    public static String toString(String template, Vector<String> strings)
    {
        return toString(template, strings.iterator());
    }

    /////////////////////////////////////////////////////////////////
    // one argument
    /////////////////////////////////////////////////////////////////
    public static String toString(String template, String s1) 
    {
        Vector<String> strings = new Vector<String>();
        strings.add(s1);
        return toString(template, strings.iterator());
    }

    public static String toString(String template, int s1) 
    {
        return toString(template, Integer.toString(s1));
    }

    /////////////////////////////////////////////////////////////////
    // two arguments
    /////////////////////////////////////////////////////////////////
    public static String toString(String template, String s1, String s2) 
    {
        Vector<String> strings = new Vector<String>();
        strings.add(s1);
        strings.add(s2);
        return toString(template, strings.iterator());
    }

    public static String toString(String template, int s1, String s2) 
    {
        return toString(template, Integer.toString(s1), s2);
    }

    public static String toString(String template, String s1, int s2) 
    {
        return toString(template, s1, Integer.toString(s2));
    }

    public static String toString(String template, int s1, int s2) 
    {
        return toString(template, Integer.toString(s1), Integer.toString(s2));
    }

    /////////////////////////////// three argument ///////////////////////////////

    /////////////////////////////////////////////////////////////////
    // three arguments
    /////////////////////////////////////////////////////////////////
    public static String toString(String template, String s1, String s2, String s3) 
    {
        Vector<String> strings = new Vector<String>();
        strings.add(s1);
        strings.add(s2);
        strings.add(s3);
        return toString(template, strings.iterator());
    }

    public static String toString(String template, int s1, String s2, String s3)
    {
        return toString(template, Integer.toString(s1), s2, s3);
    }

    public static String toString(String template, String s1, int s2, String s3) 
    {
        return toString(template, s1, Integer.toString(s2), s3);
    }

    public static String toString(String template, String s1, String s2, int s3) 
    {
        return toString(template, s1, s2, Integer.toString(s3));
    }

    public static String toString(String template, int s1, int s2, String s3) 
    {
      return toString(template, Integer.toString(s1), Integer.toString(s2), s3);
    }

    public static String toString(String template, int s1, String s2, int s3) 
    {
        return toString(template, Integer.toString(s1), s2, 
            Integer.toString(s3));
    }

    public static String toString(String template, String s1, int s2, int s3) 
    {
        return toString(template, s1, Integer.toString(s2), Integer.toString(s3));
    }

    public static String toString(String template, int s1, int s2, int s3) 
    {
        return toString(template, Integer.toString(s1), 
            Integer.toString(s2), Integer.toString(s3));
    }

    /////////////////////////////////////////////////////////////////
    // 	Test driver: test all/most methods (number, type).
    /////////////////////////////////////////////////////////////////
    public static void main(String args[]) throws IOException 
    {
        System.out.println(Formatter.toString("Hel %D lo", 100));
        System.out.println(Formatter.toString("Hel %S lo", "lll"));
        
        System.out.println(Formatter.toString("=%D=%S=", 100, "Hello"));
        System.out.println(Formatter.toString("=%S=%D=", "Hello", 100));
        System.out.println(Formatter.toString("=%S=%S=", "Hello", "World"));
        System.out.println(Formatter.toString("=%D=%D=", 100, 200));
        
        System.out.println(Formatter.toString("=%D=%S=%D=", 100, "Hello", 200));
        System.out.println(Formatter.toString("=%D=%S=%S=", 100, 	
                "Hello", "World"));
        System.out.println(Formatter.toString("=%S=%S=%D=", "Hello", 
                "World", 100));
        System.out.println(Formatter.toString("=%S=%D=%S=", "Hello", 
                100, "World"));
        System.out.println(Formatter.toString("=%D=%D=%D=", 100, 200, 300));
        System.out.println(Formatter.toString("=%S=%S=%S=", "Hello", 
                "Cruel", "World"));
        
        System.out.println(Formatter.toString("=%S=%S=%S=", System.in.toString(), 
                System.out.toString(), System.err.toString()));
        
        Vector<String> 	strings = new Vector<String>();
        strings.add("Hello");
        strings.add("Goodbye");
        strings.add("Cruel");
        strings.add("World");
        System.out.println(Formatter.toString("=%S=%S=%S=%S=", strings));
    }
}
