package org.commonjava.test.compile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.annotation.processing.AbstractProcessor;

public class CompilerFixtureConfig
{

    private final List<Class<? extends AbstractProcessor>> annotationProcessors = new ArrayList<>();

    private final List<String> extraOptions = new ArrayList<>();

    public CompilerFixtureConfig withAnnotationProcessor( final Class<? extends AbstractProcessor> annotationProcessor )
    {
        annotationProcessors.add( annotationProcessor );
        return this;
    }

    public CompilerFixtureConfig withExtraOptions( final String... extraOptions )
    {
        this.extraOptions.addAll( Arrays.asList( extraOptions ) );
        return this;
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
