//$HeadURL$
/*----------------------------------------------------------------------------
 This file is part of deegree, http://deegree.org/
 Copyright (C) 2001-2009 by:
 - Department of Geography, University of Bonn -
 and
 - lat/lon GmbH -

 This library is free software; you can redistribute it and/or modify it under
 the terms of the GNU Lesser General Public License as published by the Free
 Software Foundation; either version 2.1 of the License, or (at your option)
 any later version.
 This library is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 details.
 You should have received a copy of the GNU Lesser General Public License
 along with this library; if not, write to the Free Software Foundation, Inc.,
 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

 Contact information:

 lat/lon GmbH
 Aennchenstr. 19, 53177 Bonn
 Germany
 http://lat-lon.de/

 Department of Geography, University of Bonn
 Prof. Dr. Klaus Greve
 Postfach 1147, 53001 Bonn
 Germany
 http://www.geographie.uni-bonn.de/deegree/

 e-mail: info@deegree.org
 ----------------------------------------------------------------------------*/
package org.deegree.commons.utils.log;

import static java.lang.Integer.parseInt;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import org.slf4j.Logger;

/**
 * <code>LoggingAnnotationProcessor</code>
 * 
 * @author <a href="mailto:schmitz@lat-lon.de">Andreas Schmitz</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date$
 */

@SupportedAnnotationTypes(value = { "org.deegree.commons.utils.log.PackageLoggingNotes",
                                   "org.deegree.commons.utils.log.LoggingNotes" })
@SupportedSourceVersion(SourceVersion.RELEASE_6)
@SupportedOptions( { "log4j.outputfile", "width" })
public class LoggingAnnotationProcessor extends AbstractProcessor {

    private static final Logger LOG = getLogger( LoggingAnnotationProcessor.class );

    private String outFile;

    private int width;

    @Override
    public void init( ProcessingEnvironment env ) {
        super.init( env );
        outFile = env.getOptions().get( "log4j.outputfile" );
        String w = env.getOptions().get( "width" );
        width = w == null ? 80 : parseInt( w );
        if ( outFile == null ) {
            outFile = System.getProperty( "java.io.tmpdir" ) + "/log4j.snippet";
            LOG.info( "Outputting log4j snippet to '{}'.", outFile );
        }
    }

    // breaks the lines at max width
    private String format( String str ) {
        StringBuilder res = new StringBuilder();
        outer: while ( str.length() > ( width - 3 ) ) {
            int len = 3;
            res.append( "## " );
            while ( len < width && str.length() > 0 ) {
                int idx = str.indexOf( " " );
                if ( idx == -1 ) {
                    if ( len > 3 ) {
                        res.append( "\n## " );
                    }
                    res.append( str );
                    str = "";
                } else {
                    if ( len + idx + 1 > width ) {
                        res.append( "\n" );
                        continue outer;
                    }
                    res.append( str.substring( 0, idx + 1 ) );
                    str = str.substring( idx + 1 );
                    len += idx + 1;
                }
            }
            if ( !str.isEmpty() ) {
                res.append( "\n" );
            }
        }
        if ( !str.isEmpty() ) {
            res.append( "## " + str );
        }
        return res.toString();
    }

    @Override
    public boolean process( Set<? extends TypeElement> annotations, RoundEnvironment roundEnv ) {
        try {
            PrintWriter out = new PrintWriter( new OutputStreamWriter( new FileOutputStream( outFile, true ), "UTF-8" ) );

            // the #toString apparently yields the qname, is there another way?
            TreeMap<String, Element> sorted = new TreeMap<String, Element>();
            for ( Element e : roundEnv.getElementsAnnotatedWith( PackageLoggingNotes.class ) ) {
                sorted.put( e.toString(), e );
            }

            for ( Element e : roundEnv.getElementsAnnotatedWith( LoggingNotes.class ) ) {
                sorted.put( e.toString(), e );
            }

            for ( Entry<String, Element> e : sorted.entrySet() ) {
                LoggingNotes notes = e.getValue().getAnnotation( LoggingNotes.class );

                if ( notes == null ) {
                    PackageLoggingNotes pnotes = e.getValue().getAnnotation( PackageLoggingNotes.class );
                    String title = pnotes.title();

                    boolean isSubsystem = e.getKey().replaceAll( "[^\\.]", "" ).length() == 2;

                    if ( !title.isEmpty() ) {
                        if ( isSubsystem ) {
                            out.print( "# " );
                            for ( int i = 0; i < width - 2; ++i ) {
                                out.print( "=" );
                            }
                            out.println();
                        }

                        int odd = title.length() % 2;
                        int len = ( width - title.length() - 4 ) / 2;
                        out.print( "# " );
                        for ( int i = 0; i < len; ++i ) {
                            out.print( "=" );
                        }
                        out.print( " " + title + " " );
                        for ( int i = 0; i < len + odd; ++i ) {
                            out.print( "=" );
                        }
                        out.println();

                        if ( isSubsystem ) {
                            out.print( "# " );
                            for ( int i = 0; i < width - 2; ++i ) {
                                out.print( "=" );
                            }
                            out.println();
                        }

                        out.println();
                    }

                    String qname = e.getKey();

                    if ( !pnotes.error().isEmpty() ) {
                        out.println( format( pnotes.error() ) );
                        out.println( "#log4j.logger." + qname + " = ERROR" );
                        out.println();
                    }
                    if ( !pnotes.warn().isEmpty() ) {
                        out.println( format( pnotes.warn() ) );
                        out.println( "#log4j.logger." + qname + " = WARN" );
                        out.println();
                    }
                    if ( !pnotes.info().isEmpty() ) {
                        out.println( format( pnotes.info() ) );
                        out.println( "#log4j.logger." + qname + " = INFO" );
                        out.println();
                    }
                    if ( !pnotes.debug().isEmpty() ) {
                        out.println( format( pnotes.debug() ) );
                        out.println( "#log4j.logger." + qname + " = DEBUG" );
                        out.println();
                    }
                    if ( !pnotes.trace().isEmpty() ) {
                        out.println( format( pnotes.trace() ) );
                        out.println( "#log4j.logger." + qname + " = TRACE" );
                        out.println();
                    }
                } else {
                    String qname = e.getKey();

                    if ( !notes.error().isEmpty() ) {
                        out.println( format( notes.error() ) );
                        out.println( "#log4j.logger." + qname + " = ERROR" );
                        out.println();
                    }
                    if ( !notes.warn().isEmpty() ) {
                        out.println( format( notes.warn() ) );
                        out.println( "#log4j.logger." + qname + " = WARN" );
                        out.println();
                    }
                    if ( !notes.info().isEmpty() ) {
                        out.println( format( notes.info() ) );
                        out.println( "#log4j.logger." + qname + " = INFO" );
                        out.println();
                    }
                    if ( !notes.debug().isEmpty() ) {
                        out.println( format( notes.debug() ) );
                        out.println( "#log4j.logger." + qname + " = DEBUG" );
                        out.println();
                    }
                    if ( !notes.trace().isEmpty() ) {
                        out.println( format( notes.trace() ) );
                        out.println( "#log4j.logger." + qname + " = TRACE" );
                        out.println();
                    }
                }
            }

            out.close();
            return true;
        } catch ( UnsupportedEncodingException e ) {
            e.printStackTrace();
        } catch ( FileNotFoundException e ) {
            e.printStackTrace();
        }
        return false;
    }
}
