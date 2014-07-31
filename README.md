compiler-test-fixture
=====================

JUnit test fixture (@Rule) an accompanying classes for compiling sources during a test using javax.tools.JavaCompiler

Usage Example
--------------

    @Test
    public void annotationProcessorGeneratedSourcesGetCompiled()
        throws Exception
    {
        final CompilerResult result =
            compiler.compileSourceDirWithThisClass( "anno-proc-gen-src",
                                                    "org.test.Hello",
                                                    new CompilerFixtureConfig().withAnnotationProcessor( TestProcessor.class ) );
        
        final List<File> classfiles = compiler.scan( result.getClasses(), "**/*.class" );
        System.out.printf( "%d classes generated in: %s\n%s\n", classfiles.size(), result.getClasses(),
                           join( classfiles, "\n" ) );
        
        final List<File> gensrcfiles = compiler.scan( result.getGeneratedSources(), "**/*.java" );
        System.out.printf( "%d generated source files in: %s\n%s\n", gensrcfiles.size(), result.getGeneratedSources(),
                           join( gensrcfiles, "\n" ) );
        
        final Class<?> cls = result.getClassLoader()
                                   .loadClass( "org.test.HelloDoc" );
        
        final Method method = cls.getMethod( "main", new Class[] { String[].class } );
        
        System.out.println( method );
        method.invoke( null, new Object[] { new String[] {} } );
    }
