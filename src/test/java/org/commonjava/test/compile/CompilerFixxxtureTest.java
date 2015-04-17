/**
 * Copyright (C) 2014 John Casey (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.test.compile;

import static org.apache.commons.lang.StringUtils.join;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.List;
import java.util.ServiceLoader;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

public class CompilerFixxxtureTest
{

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Rule
    public CompilerFixture compiler = new CompilerFixture( temp );

    @Rule
    public TestName name = new TestName();

    @Test
    public void compileDependingOnlyOnJDK()
        throws Exception
    {
        testHelloWorld( "jdk-only", true );
    }

    @Test
    public void compileDependingOnlyOnJDK_NonCompilable()
        throws Exception
    {
        testHelloWorld( "jdk-only-fails", false );
    }

    @Test
    public void compileInterdependentClasses()
        throws Exception
    {
        testHelloWorld( "interdep", true );
    }

    @Test
    public void compileDependingOnlyOnJDK_UseServiceLoader()
        throws Exception
    {
        final CompilerResult result = compiler.compileSourceDirWithThisClass( "jdk-only-service", "org.test.Hello" );

        FileUtils.write( Paths.get( result.getClasses()
                                          .getAbsolutePath(), "META-INF", "services", "org.test.IHello" )
                              .toFile(), "org.test.Hello" );

        final URLClassLoader ucl = result.getClassLoader();
        final Class<?> cls = ucl.loadClass( "org.test.IHello" );
        final ServiceLoader<?> loader = ServiceLoader.load( cls, ucl );
        final Object object = loader.iterator()
                                    .next();

        final Method method = cls.getMethod( "hello", new Class[] { String[].class } );

        System.out.println( method );
        method.invoke( object, new Object[] { new String[] { "Tester" } } );
    }

    @Test
    public void compileDependingOnCallerClasspath()
        throws Exception
    {
        final CompilerResult result = compiler.compileSourceDirWithThisClass( "dep-on-calling-cp", "org.test.Hello" );
        final Class<?> cls = result.getClassLoader()
                                   .loadClass( "org.test.Hello" );

        final Object object = cls.newInstance();

        final Method method = MyInterface.class.getMethod( "sayHello", new Class[] { String.class } );

        System.out.println( method );
        method.invoke( object, new Object[] { "Tester" } );
    }

    @Test
    public void annotationProcessorGeneratedSourcesGetCompiled()
        throws Exception
    {
        final CompilerResult result =
            compiler.compileSourceDirWithThisClass( "anno-proc-gen-src",
                                                    "org.test.Hello",
                                                    new CompilerFixtureConfig().withAnnotationProcessor( TestProcessor.class )
                                                                               .withExtraOptions( "-verbose" ) );

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

    @Test
    public void annotationProcessorGeneratedSourcesGetCompiled_DependentOnExistingClass()
        throws Exception
    {
        final CompilerResult result =
            compiler.compileSourceDirWithThisClass( "anno-proc-gen-src",
                                                    "org.test.Hello",
                                                    new CompilerFixtureConfig().withAnnotationProcessor( InterdepTestProcessor.class ) );

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

    private void testHelloWorld( final String basedir, final boolean expectSuccess )
        throws Exception
    {
        final CompilerResult result = compiler.compileSourceDirWithThisClass( basedir, "org.test.Hello" );
        assertThat( result.getResult()
                          .booleanValue(), equalTo( expectSuccess ) );

        if ( !result.getResult() )
        {
            return;
        }

        final Class<?> cls = result.getClassLoader()
                                   .loadClass( "org.test.Hello" );
        final Method method = cls.getMethod( "main", new Class[] { String[].class } );

        System.out.println( method );
        method.invoke( null, new Object[] { new String[] { "Tester" } } );
    }

}
