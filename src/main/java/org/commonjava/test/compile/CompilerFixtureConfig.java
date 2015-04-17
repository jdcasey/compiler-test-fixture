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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.annotation.processing.AbstractProcessor;

public class CompilerFixtureConfig
{

    private final List<Class<? extends AbstractProcessor>> annotationProcessors = new ArrayList<>();

    private int maxAnnotationProcessorPasses = 1;

    private final List<String> extraOptions = new ArrayList<>();

    public CompilerFixtureConfig withAnnotationProcessor( final Class<? extends AbstractProcessor> annotationProcessor )
    {
        annotationProcessors.add( annotationProcessor );
        return this;
    }

    public CompilerFixtureConfig withMaxAnnotationProcessorPasses( final int max )
    {
        this.maxAnnotationProcessorPasses = max;
        return this;
    }

    public CompilerFixtureConfig withExtraOptions( final String... extraOptions )
    {
        this.extraOptions.addAll( Arrays.asList( extraOptions ) );
        return this;
    }

    public int getMaxAnnotationProcessorPasses()
    {
        return maxAnnotationProcessorPasses;
    }

    public List<Class<? extends AbstractProcessor>> getAnnotationProcessors()
    {
        return annotationProcessors;
    }

    public Collection<? extends String> getExtraOptions()
    {
        return extraOptions;
    }

}
