package org.test;

public class Hello
    implements org.commonjava.test.compile.MyInterface
{
    
    public void sayHello( String name )
    {
        System.out.println( "Hello, " + name + "!" );
    }

}
