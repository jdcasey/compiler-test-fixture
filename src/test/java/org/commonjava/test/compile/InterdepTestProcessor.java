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

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/* @formatter:off */
@SupportedAnnotationTypes( "org.commonjava.test.compile.Doc" )
@SupportedSourceVersion( SourceVersion.RELEASE_7 )
/* @formatter:on */
public class InterdepTestProcessor
    extends AbstractProcessor
{

    @Override
    public boolean process( final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv )
    {
        final Filer filer = processingEnv.getFiler();

        for ( final Element elem : roundEnv.getElementsAnnotatedWith( Doc.class ) )
        {
            final String comment = processingEnv.getElementUtils()
                                                .getDocComment( elem );

            Element pe = elem;
            do
            {
                pe = pe.getEnclosingElement();
            }
            while ( pe != null && pe.getKind() != ElementKind.PACKAGE );

            final String pkg = ( (PackageElement) pe ).getQualifiedName()
                                                      .toString();

            System.out.printf( "Processing: %s\n", elem );
            final Doc doc = elem.getAnnotation( Doc.class );

            final String className = elem.getSimpleName() + "Doc";
            final String resName = pkg.replace( '.', '/' ) + "/" + className + ".java";
            System.out.println( resName + "\n  " + doc.value() );

            Writer writer = null;
            try
            {
                final FileObject file =
                    filer.createResource( StandardLocation.SOURCE_OUTPUT, "", resName, (Element[]) null );
                writer = file.openWriter();
                writer.write( "package " + pkg + ";\n\npublic class " + className
                    + "{\n    public static void main( String[] args ){\n        System.out.println(" + pkg + "."
                    + elem.getSimpleName() + ".class.getSimpleName() + \": " + doc.value()
                    + " " + comment.replaceAll( "\\n", "\\\\n" ) + "\");\n    }\n}" );
            }
            catch ( final IOException e )
            {
                processingEnv.getMessager()
                             .printMessage( Kind.ERROR,
                                            "While generating documentation class: '" + resName + "', error: "
                                                + e.getMessage() );
            }
            finally
            {
                if ( writer != null )
                {
                    try
                    {
                        writer.close();
                    }
                    catch ( final IOException e )
                    {
                    }
                }
            }
        }

        return true;
    }

}
