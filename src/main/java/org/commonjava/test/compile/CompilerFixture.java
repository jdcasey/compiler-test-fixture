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

public class CompilerFixture
    extends ExternalResource
{

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

        List<File> sources = scan( directory, "**/*.java" );

        final JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        final StandardJavaFileManager fileManager = javac.getStandardFileManager( null, null, null );
        Iterable<? extends JavaFileObject> fileObjs = fileManager.getJavaFileObjectsFromFiles( sources );

        final DiagnosticCollector<JavaFileObject> diags = new DiagnosticCollector<>();

        final List<String> options = new ArrayList<>( Arrays.asList( "-g", "-d", target.getCanonicalPath() ) );

        options.addAll( config.getExtraOptions() );

        File generatedSourceDir = null;
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

            options.add( "-processor" );
            options.add( sb.toString() );

            generatedSourceDir = temp.newFolder( directory.getName() + "-generated-sources" );
            options.add( "-s" );
            options.add( generatedSourceDir.getCanonicalPath() );
        }

        CompilationTask task = javac.getTask( null, fileManager, diags, options, null, fileObjs );
        Boolean result = task.call();

        final Set<File> seenSources = new HashSet<>();
        if ( result && generatedSourceDir != null && generatedSourceDir.isDirectory() )
        {
            sources = scan( generatedSourceDir, "**/*.java" );
            sources.removeAll( seenSources );
            seenSources.addAll( sources );

            int pass = 1;
            while ( !sources.isEmpty() )
            {
                System.out.printf( "pass: %d Compiling/processing generated sources:\n  %s\n", pass,
                                   join( sources, "\n  " ) );
                fileObjs = fileManager.getJavaFileObjectsFromFiles( sources );
                task = javac.getTask( null, fileManager, diags, options, null, fileObjs );
                result = task.call();

                sources = scan( generatedSourceDir, "**/*.java" );
                sources.removeAll( seenSources );
                seenSources.addAll( sources );
                pass++;
            }
        }

        for ( final Diagnostic<? extends JavaFileObject> diag : diags.getDiagnostics() )
        {
            System.out.println( diag );
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
