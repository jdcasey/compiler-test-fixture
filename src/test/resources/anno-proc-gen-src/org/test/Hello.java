package org.test;

import org.commonjava.test.compile.Doc;

/**
 * Class that tests the use of an annotation processor to generate new classes based on these docs.
 * @author jdcasey
 */
@Doc( "Hello, this is documentation!" )
public class Hello
{
    
    public static void main( String[] args )
    {
        System.out.println( "Hello, " + args[0] + "!" );
    }

}
