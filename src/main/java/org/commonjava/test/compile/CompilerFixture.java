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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.apache.commons.io.IOUtils;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompilerFixture
    extends ExternalResource
{

    public class JoinLogString
    {

        private final Collection<?> objects;

        private final String joint;

        public JoinLogString( final Collection<?> objects, final String joint )
        {
            this.objects = objects;
            this.joint = joint;
        }

        @Override
        public String toString()
        {
            return join( objects, joint );
        }

    }

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final TemporaryFolder temp;

    private final Set<CompilerResult> results = new HashSet<>();

    public CompilerFixture( final TemporaryFolder temp )
    {
        this.temp = temp;
    }

    private File getResourceDirectory( final String dir, final String className )
    {
        final String path = Paths.get( dir, className.replace( '.', '/' ) + ".java" )
                                 .toString();

        final URL url = Thread.currentThread()
                              .getContextClassLoader()
                              .getResource( path );

        if ( url == null )
        {
            return null;
        }

        final String[] parts = className.split( "\\." );
        File f = new File( url.getPath() );
        for ( int i = 0; i < parts.length; i++ )
        {
            f = f.getParentFile();
        }

        return f;
    }

    public CompilerResult compileSourceDirWithThisClass( final String basedir, final String sampleClassName )
        throws IOException
    {
        return compileSourceDirWithThisClass( basedir, sampleClassName, new CompilerFixtureConfig() );
    }

    public CompilerResult compileSourceDirWithThisClass( final String basedir, final String sampleClassName,
                                                         final CompilerFixtureConfig config )
        throws IOException
    {
        final File dir = getResourceDirectory( basedir, sampleClassName );
        return compile( dir, config );
    }

    public CompilerResult compile( final File directory )
        throws IOException
    {
        return compile( directory, new CompilerFixtureConfig() );
    }

    public CompilerResult compile( final File directory, final CompilerFixtureConfig config )
        throws IOException
    {
        if ( directory == null || !directory.isDirectory() )
        {
            return null;
        }

        final File target = temp.newFolder( directory.getName() + "-classes" );

        final List<File> sources = scan( directory, "**/*.java" );

        final JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        final StandardJavaFileManager fileManager = javac.getStandardFileManager( null, null, null );
        final Set<JavaFileObject> objects = new HashSet<>();

        for ( final JavaFileObject jfo : fileManager.getJavaFileObjectsFromFiles( sources ) )
        {
            objects.add( jfo );
        }

        final DiagnosticCollector<JavaFileObject> diags = new DiagnosticCollector<>();

        final List<String> options = new ArrayList<>( Arrays.asList( "-g", "-d", target.getCanonicalPath() ) );

        options.addAll( config.getExtraOptions() );

        final StringBuilder sp = new StringBuilder();
        sp.append( directory.getCanonicalPath() )
          .append( ';' )
          .append( target.getCanonicalPath() );

        File generatedSourceDir = null;

        final List<String> procOptions = new ArrayList<>( options );
        procOptions.add( "-proc:only" );

        final Set<File> seenSources = new HashSet<>( sources );
        Boolean result = Boolean.TRUE;

        final List<Class<? extends AbstractProcessor>> annoProcessors = config.getAnnotationProcessors();
        if ( !annoProcessors.isEmpty() )
        {
            final StringBuilder sb = new StringBuilder();
            for ( final Class<? extends AbstractProcessor> annoProcessor : annoProcessors )
            {
                if ( sb.length() > 0 )
                {
                    sb.append( "," );
                }

                sb.append( annoProcessor.getCanonicalName() );
            }

            procOptions.add( "-processor" );
            procOptions.add( sb.toString() );

            generatedSourceDir = temp.newFolder( directory.getName() + "-generated-sources" );
            procOptions.add( "-s" );
            procOptions.add( generatedSourceDir.getCanonicalPath() );

            sp.append( ';' )
              .append( generatedSourceDir.getCanonicalPath() );

            procOptions.add( "-sourcepath" );
            procOptions.add( sp.toString() );

            int pass = 1;
            List<File> nextSources;
            do
            {
                logger.debug( "pass: {} Compiling/processing generated sources with: '{}':\n  {}\n", pass,
                                   new JoinLogString( procOptions, ", " ),
                                   new JoinLogString( sources, "\n  " ) );

                for ( final JavaFileObject jfo : fileManager.getJavaFileObjectsFromFiles( seenSources ) )
                {
                    objects.add( jfo );
                }

                final CompilationTask task = javac.getTask( null, fileManager, diags, procOptions, null, objects );
                result = task.call();

                nextSources = scan( generatedSourceDir, "**/*.java" );

                logger.debug( "\n\nNewly scanned sources:\n  {}\n\nPreviously seen sources:\n  {}\n\n",
                                   new JoinLogString( nextSources, "\n  " ), new JoinLogString( seenSources, "\n  " ) );
                nextSources.removeAll( seenSources );
                seenSources.addAll( nextSources );
                pass++;
            }
            while ( pass < config.getMaxAnnotationProcessorPasses() && !nextSources.isEmpty() );
        }

        if ( result )
        {
            options.add( "-sourcepath" );
            options.add( sp.toString() );

            options.add( "-proc:none" );

            for ( final JavaFileObject jfo : fileManager.getJavaFileObjectsFromFiles( seenSources ) )
            {
                objects.add( jfo );
            }

            final CompilationTask task = javac.getTask( null, fileManager, diags, options, null, objects );
            result = task.call();

            logger.debug( "Compiled classes:\n  {}\n\n", new JoinLogString( scan( target, "**/*.class" ), "\n  " ) );
        }
        else
        {
            logger.warn( "Annotation processing must have failed. Skipping compilation step." );
        }

        for ( final Diagnostic<? extends JavaFileObject> diag : diags.getDiagnostics() )
        {
            logger.error( String.valueOf( diag ) );
        }

        final CompilerResult cr = new CompilerResultBuilder().withClasses( target )
                                                             .withDiagnosticCollector( diags )
                                                             .withGeneratedSources( generatedSourceDir )
                                                             .withResult( result )
                                                             .build();

        results.add( cr );
        return cr;
    }

    public List<File> scan( final File directory, final String pattern )
        throws IOException
    {
        final PathMatcher matcher = FileSystems.getDefault()
                                               .getPathMatcher( "glob:" + directory.getCanonicalPath() + "/" + pattern );

        final List<File> sources = new ArrayList<>();
        Files.walkFileTree( directory.toPath(), new SimpleFileVisitor<Path>()
        {

            @Override
            public FileVisitResult visitFile( final Path file, final BasicFileAttributes attrs )
                throws IOException
            {
                if ( matcher.matches( file ) )
                {
                    sources.add( file.toFile() );
                }

                return FileVisitResult.CONTINUE;
            }

        } );

        return sources;
    }

    @Override
    protected void after()
    {
        for ( final CompilerResult result : results )
        {
            IOUtils.closeQuietly( result );
        }

        super.after();
    }

}
