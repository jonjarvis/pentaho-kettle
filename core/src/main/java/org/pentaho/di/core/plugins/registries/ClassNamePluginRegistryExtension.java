package org.pentaho.di.core.plugins.registries;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.logging.KettleLogStore;
import org.pentaho.di.core.logging.LogChannel;
import org.pentaho.di.core.plugins.PluginAnnotationType;
import org.pentaho.di.core.plugins.PluginRegistry;
import org.pentaho.di.core.plugins.PluginRegistryExtension;
import org.pentaho.di.core.plugins.PluginTypeInterface;
import org.pentaho.di.core.util.EnvUtil;
import org.pentaho.di.core.util.Utils;

/**
 * Finds and registers plugins (using annotations) specified
 * in the KETTLE_PLUGIN_CLASSES environment variable
 * @author jjarvis
 *
 */
public class ClassNamePluginRegistryExtension implements PluginRegistryExtension {
  
  private Set<String> pluginClassNames = new HashSet<>();
  
  @Override
  public void init( PluginRegistry registry ) {
    // Scan for plugin classes to facilitate debugging etc.
    String pluginClasses = EnvUtil.getSystemProperty( Const.KETTLE_PLUGIN_CLASSES );
    if ( !Utils.isEmpty( pluginClasses ) ) {
      String[] classNames = pluginClasses.split( "," );
      Collections.addAll( pluginClassNames, classNames );
    }
  }

  @Override
  public void searchForType( PluginTypeInterface pluginType ) {
    
    for ( String className : pluginClassNames ) {
      try {
        // What annotation does the plugin type have?
        PluginAnnotationType annotationType = pluginType.getClass().getAnnotation( PluginAnnotationType.class );
        if ( annotationType != null ) {
          Class<? extends Annotation> annotationClass = annotationType.value();

          Class<?> clazz = Class.forName( className );
          Annotation annotation = clazz.getAnnotation( annotationClass );

          if ( annotation != null ) {
            // Register this one!
            //
            pluginType.handlePluginAnnotation( clazz, annotation, new ArrayList<>(), true, null );
            LogChannel.GENERAL.logBasic( "Plugin class "
                + className + " registered for plugin type '" + pluginType.getName() + "'" );
          } else {
            if ( KettleLogStore.isInitialized() && LogChannel.GENERAL.isDebug() ) {
              LogChannel.GENERAL.logDebug( "Plugin class "
                  + className + " doesn't contain annotation for plugin type '" + pluginType.getName() + "'" );
            }
          }
        } else {
          if ( KettleLogStore.isInitialized() && LogChannel.GENERAL.isDebug() ) {
            LogChannel.GENERAL.logDebug( "Plugin class "
                + className + " doesn't contain valid class for plugin type '" + pluginType.getName() + "'" );
          }
        }
      } catch ( Exception e ) {
        if ( KettleLogStore.isInitialized() ) {
          LogChannel.GENERAL.logError( "Error registring plugin class from KETTLE_PLUGIN_CLASSES: "
              + className + Const.CR + Const.getStackTracker( e ) );
        }
      }
    }
    
  }

  @Override
  public String getPluginId( Class<? extends PluginTypeInterface> pluginType, Object pluginClass ) {
    return null;
  }
  
  
  

}
