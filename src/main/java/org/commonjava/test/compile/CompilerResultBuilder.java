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
