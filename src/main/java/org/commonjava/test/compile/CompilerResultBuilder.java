package org.commonjava.test.compile;

import java.io.File;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;

final class CompilerResultBuilder
{

    private File classes;

    private File generatedSources;

    private DiagnosticCollector<? extends JavaFileObject> diagnostics;

    private Boolean result;

    CompilerResultBuilder withClasses( final File classes )
    {
        this.classes = classes;
        return this;
    }

    CompilerResultBuilder withResult( final Boolean result )
    {
        this.result = result;
        return this;
    }

    CompilerResultBuilder withGeneratedSources( final File generatedSources )
    {
        this.generatedSources = generatedSources;
        return this;
    }

    CompilerResultBuilder withDiagnosticCollector( final DiagnosticCollector<? extends JavaFileObject> collector )
    {
        this.diagnostics = collector;
        return this;
    }

    CompilerResult build()
    {
        if ( result == null || classes == null || !classes.isDirectory() )
        {
            throw new IllegalStateException( "result or classes directory is missing!" );
        }
        return new CompilerResult( classes, generatedSources, diagnostics, result );
    }

}
