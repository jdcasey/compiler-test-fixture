package org.commonjava.test.compile;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;

public class CompilerResult
    implements Closeable
{
    private final File classes;

    private final File generatedSources;

    private final DiagnosticCollector<? extends JavaFileObject> diagnostics;

    private final Boolean result;

    private URLClassLoader classloader;

    public CompilerResult( final File classes, final File generatedSources,
                           final DiagnosticCollector<? extends JavaFileObject> diagnostics, final Boolean result )
    {
        this.classes = classes;
        this.generatedSources = generatedSources;
        this.diagnostics = diagnostics;
        this.result = result;
    }

    public File getClasses()
    {
        return classes;
    }

    public File getGeneratedSources()
    {
        return generatedSources;
    }

    public DiagnosticCollector<? extends JavaFileObject> getDiagnostics()
    {
        return diagnostics;
    }

    public Boolean getResult()
    {
        return result;
    }

    public URLClassLoader getClassLoader()
        throws MalformedURLException
    {
        if ( classloader == null )
        {
            classloader = new URLClassLoader( new URL[] { classes.toURI()
                                                                 .toURL() }, Thread.currentThread()
                                                                                   .getContextClassLoader() );
        }

        return classloader;
    }

    public URLClassLoader buildCustomClassLoader( final File... classpath )
        throws MalformedURLException
    {
        final URL[] urls = new URL[classpath.length + 1];
        int i = 0;
        for ( final File f : classpath )
        {
            urls[i] = f.toURI()
                       .toURL();
            i++;
        }
        urls[i] = classes.toURI()
                         .toURL();

        return new URLClassLoader( urls, Thread.currentThread()
                                               .getContextClassLoader() );
    }

    @Override
    public void close()
        throws IOException
    {
        if ( classloader != null )
        {
            classloader.close();
        }
    }

}
