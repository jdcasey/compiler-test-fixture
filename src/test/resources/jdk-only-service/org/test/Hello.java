package org.test;

public class Hello
    implements IHello
{
    
    public void hello( String[] args )
    {
        System.out.println( "Hello, " + args[0] + "!" );
    }

}
