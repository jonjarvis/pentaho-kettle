package org.pentaho.di.core.plugins.registries;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import org.apache.commons.vfs2.FileObject;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleFileException;
import org.pentaho.di.core.exception.KettlePluginException;
import org.pentaho.di.core.logging.LogChannel;
import org.pentaho.di.core.plugins.PluginRegistry;
import org.pentaho.di.core.plugins.PluginRegistryExtension;
import org.pentaho.di.core.plugins.PluginTypeInterface;
import org.pentaho.di.core.util.EnvUtil;
import org.scannotation.AnnotationDB;

public class AnnotationSearchingPluginRegistryExtension implements PluginRegistryExtension {

  private PluginRegistry registry;

  private JarFileCache jarFileCache;

  private Executor executor;
  
  @Override
  public void init( PluginRegistry registry ) {
    this.registry = registry;
    
    //this.executor = Executors.newCachedThreadPool();

    if ( this.jarFileCache == null ) {
      this.jarFileCache = new JarFileCache();
    }

    String folderPaths = EnvUtil.getSystemProperty( Const.PLUGIN_BASE_FOLDERS_PROP );
    if ( folderPaths == null ) {
      folderPaths = Const.DEFAULT_PLUGIN_BASE_FOLDERS;
    }
    String[] folders = folderPaths.split( "," );

    for ( String folder : folders ) {
      folder = folder.trim();

      long startTime = System.currentTimeMillis();

      try {
        jarFileCache.getJarFiles( folder );
      } catch ( KettleFileException e ) {
        throw new RuntimeException( e );
      }

      System.out.println( String.format( "Traversing '%s' folder's plugin jars completed in %d ms", folder,
        System.currentTimeMillis() - startTime ) );

    }

  }

  @Override
  public void searchForType( PluginTypeInterface pluginType ) {

    List<CompletableFuture<Void>> allFutures = new LinkedList<>();
    
    // Loop over all source plugin base folders
    for ( FileObject[] jarFiles : jarFileCache.allJarFiles() ) {

      if ( jarFiles != null ) {
        
        for ( FileObject fileObject : jarFiles ) {
          
          allFutures.add( CompletableFuture.runAsync( () -> {

            List<JarFileAnnotationPlugin> classFiles = new ArrayList<>( 1 );

            // These are the jar files : find annotations in them
            try {
              AnnotationDB annotationDB = jarFileCache.getAnnotationDB( fileObject );
              Set<String> impls = annotationDB.getAnnotationIndex().get( pluginType.getAnnotationClass().getName() );
              if ( impls != null ) {
                for ( String annotatedClass : impls ) {
                  classFiles.add( new JarFileAnnotationPlugin( annotatedClass, fileObject.getURL(), fileObject
                      .getParent().getURL() ) );
                }
              }
            } catch ( Exception jarPluginLoadError ) {
              LogChannel.GENERAL.logError( "Error while finding annotations for jar plugin: '"
                  + fileObject + "'" );
              LogChannel.GENERAL.logDebug( "Error while finding annotations for jar plugin: '"
                  + fileObject + "'",
                jarPluginLoadError );
            }

            for ( JarFileAnnotationPlugin jarFilePlugin : classFiles ) {

              URLClassLoader urlClassLoader = jarFileCache.getClassLoader( jarFilePlugin.getJarFile() );
              
              try {
                Class<?> clazz = urlClassLoader.loadClass( jarFilePlugin.getClassName() );
                if ( clazz == null ) {
                  throw new KettlePluginException( "Unable to load class: " + jarFilePlugin.getClassName() );
                }
                List<String> libraries =
                    Arrays.stream( urlClassLoader.getURLs() )
                        .map( URL::getFile )
                        .collect( Collectors.toList() );
                Annotation annotation = clazz.getAnnotation( pluginType.getAnnotationClass() );

                pluginType.handlePluginAnnotation( clazz, annotation, libraries, false,
                  jarFilePlugin.getPluginFolder() );
              } catch ( Exception e ) {
                // Ignore for now, don't know if it's even possible.
                LogChannel.GENERAL.logError(
                  "Unexpected error registering jar plugin file: " + jarFilePlugin.getJarFile(), e );
              } finally {
               /*
                if ( urlClassLoader instanceof KettleURLClassLoader ) {
                  ( (KettleURLClassLoader) urlClassLoader ).closeClassLoader();
                } */
              }
            }

          }, this.executor ) );

        }
      }
       
    }
    
    //Wait for all of the futures that process individual jar files to finish
    CompletableFuture.allOf( allFutures.toArray( new CompletableFuture[ allFutures.size() ] ) ).join();

  }

  @Override
  public String getPluginId( Class<? extends PluginTypeInterface> pluginType, Object pluginClass ) {
    return null;
  }

}
