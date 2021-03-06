package io.gomint.server.registry;

import io.gomint.server.GoMintServer;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author geNAZt
 * @version 1.0
 */
public class Registry<R> {

    private static final Logger LOGGER = LoggerFactory.getLogger( Registry.class );

    private final GeneratorCallback<R> generatorCallback;
    private R[] generators;
    private R[] negativeGenerators;
    private final Object2IntMap<Class<?>> apiReferences = new Object2IntOpenHashMap<>();

    /**
     * Build a new generator registry
     *
     * @param server   which started this gomint
     * @param callback which is used to generate a generator for each found element
     */
    public Registry( GoMintServer server, GeneratorCallback<R> callback ) {
        this.generatorCallback = callback;
        this.generators = (R[]) new Object[16];
        this.negativeGenerators = (R[]) new Object[2];
    }

    /**
     * Register all classes which can be found in given path
     *
     * @param classPath which should be searched
     */
    public void register( String classPath ) {
        LOGGER.debug( "Going to scan: {}", classPath );

        /*for ( ClassPath.ClassInfo classInfo : this.server.getClassPath().getTopLevelClasses( classPath ) ) {
            register( classInfo.load() );
        }*/
    }

    private void register( Class<?> clazz ) {
        // We need register info
        if ( !clazz.isAnnotationPresent( RegisterInfo.class ) && !clazz.isAnnotationPresent( RegisterInfos.class ) ) {
            LOGGER.debug( "No register info annotation present" );
            return;
        }

        if ( clazz.isAnnotationPresent( RegisterInfo.class ) ) {
            int id = clazz.getAnnotation( RegisterInfo.class ).id();
            R generator = this.generatorCallback.generate( clazz );
            if ( generator != null ) {
                this.storeGeneratorForId( id, generator );

                // Check for API interfaces
                for ( Class<?> apiInter : clazz.getInterfaces() ) {
                    this.apiReferences.put( apiInter, id );
                }

                this.apiReferences.put( clazz, id );
            }
        } else {
            R generator = this.generatorCallback.generate( clazz );
            if ( generator != null ) {
                RegisterInfo[] infos = clazz.getAnnotation( RegisterInfos.class ).value();
                for ( RegisterInfo info : infos ) {
                    int id = info.id();
                    this.storeGeneratorForId( id, generator );

                    // Check for API interfaces
                    for ( Class<?> apiInter : clazz.getInterfaces() ) {
                        this.apiReferences.put( apiInter, id );
                    }

                    this.apiReferences.put( clazz, id );
                }
            }
        }
    }

    private void storeGeneratorForId( int id, R generator ) {
        boolean negative = false;
        if ( id < 0 ) {
            id = Math.abs( id );
            negative = true;
        }

        R[] array = this.ensureArraySize( negative, id );
        array[id] = generator;
    }

    private R[] ensureArraySize( boolean negative, int id ) {
        // Check if we need to grow the array
        R[] array = ( negative ) ? this.negativeGenerators : this.generators;
        if ( array.length < id + 16 ) {
            R[] temp = (R[]) new Object[id + 16];
            System.arraycopy( array, 0, temp, 0, array.length );
            if ( negative ) {
                this.negativeGenerators = temp;
            } else {
                this.generators = temp;
            }
        }

        return ( negative ) ? this.negativeGenerators : this.generators;
    }

    public R getGenerator( Class<?> clazz ) {
        // Get the internal ID
        int id = apiReferences.getOrDefault( clazz, -1 );
        if ( id == -1 ) {
            return null;
        }

        return getGenerator( id );
    }

    public final R getGenerator( int id ) {
        if ( id < 0 ) {
            id *= -1;
            return this.negativeGenerators.length <= id ? null : this.negativeGenerators[id];
        }

        return this.generators.length <= id ? null : this.generators[id];
    }

    public int getId( Class<?> clazz ) {
        return this.apiReferences.getOrDefault( clazz, -1 );
    }

    public void register( Class<?> clazz, R generator ) {
        // We need register info
        if ( !clazz.isAnnotationPresent( RegisterInfo.class ) && !clazz.isAnnotationPresent( RegisterInfos.class ) ) {
            LOGGER.debug( "No register info annotation present" );
            return;
        }

        if ( clazz.isAnnotationPresent( RegisterInfo.class ) ) {
            int id = clazz.getAnnotation( RegisterInfo.class ).id();
            this.storeGeneratorForId( id, generator );

            // Check for API interfaces
            for ( Class<?> apiInter : clazz.getInterfaces() ) {
                this.apiReferences.put( apiInter, id );
            }

            this.apiReferences.put( clazz, id );
        } else {
            RegisterInfos infos = clazz.getAnnotation( RegisterInfos.class );
            int lastId = -1;
            for ( RegisterInfo info : infos.value() ) {
                int id = info.id();
                this.storeGeneratorForId( id, generator );
                lastId = id;
            }

            if ( lastId > -1 ) {
                // Check for API interfaces
                for ( Class<?> apiInter : clazz.getInterfaces() ) {
                    this.apiReferences.put( apiInter, lastId );
                }

                this.apiReferences.put( clazz, lastId );
            }
        }
    }

}
