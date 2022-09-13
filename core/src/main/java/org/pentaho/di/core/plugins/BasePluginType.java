/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2019 by Hitachi Vantara : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.pentaho.di.core.plugins;

import java.io.File;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.vfs2.FileObject;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettlePluginException;
import org.pentaho.di.core.logging.DefaultLogLevel;
import org.pentaho.di.core.logging.LogChannel;
import org.pentaho.di.core.logging.LogLevel;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.i18n.GlobalMessageUtil;
import org.scannotation.AnnotationDB;

public abstract class BasePluginType implements PluginTypeInterface {
  protected static final Class<?> PKG = BasePluginType.class; // for i18n purposes, needed by Translator2!!

  protected String id;
  protected String name;
  protected List<PluginFolderInterface> pluginFolders;

  protected PluginRegistry registry;

  protected LogChannel log;

  protected Map<Class<?>, String> objectTypes = new HashMap<>();

  protected boolean searchLibDir;

  Class<? extends Annotation> pluginClass;

  public BasePluginType( Class<? extends Annotation> pluginClass ) {
    this.pluginFolders = new ArrayList<>();
    this.log = new LogChannel( "Plugin type" );

    registry = PluginRegistry.getInstance();
    this.pluginClass = pluginClass;
  }

  /**
   * @param id
   *          The plugin type ID
   * @param name
   *          the name of the plugin
   */
  public BasePluginType( Class<? extends Annotation> pluginClass, String id, String name ) {
    this( pluginClass );
    this.id = id;
    this.name = name;
  }

  /**
   * this is a utility method for subclasses so they can easily register which folders contain plugins
   *
   * @param xmlSubfolder
   *          the sub-folder where xml plugin definitions can be found
   */
  protected void populateFolders( String xmlSubfolder ) {
    pluginFolders.addAll( PluginFolder.populateFolders( xmlSubfolder ) );
  }

  public Map<Class<?>, String> getAdditionalRuntimeObjectTypes() {
    return objectTypes;
  }

  @Override
  public void addObjectType( Class<?> clz, String xmlNodeName ) {
    objectTypes.put( clz, xmlNodeName );
  }

  @Override
  public String toString() {
    return name + "(" + id + ")";
  }

  /**
   * Let's put in code here to search for the step plugins..
   */
  @Override
  public void searchPlugins() throws KettlePluginException {
    //registerNatives();
    //registerPluginJars();
  }
 

  /**
   * @return the id
   */
  @Override
  public String getId() {
    return id;
  }

  /**
   * @param id
   *          the id to set
   */
  public void setId( String id ) {
    this.id = id;
  }

  
  public Class<? extends Annotation> getAnnotationClass() {
    return pluginClass;
  }
  
  /**
   * @return the name
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * @param name
   *          the name to set
   */
  public void setName( String name ) {
    this.name = name;
  }

  /**
   * @return the pluginFolders
   */
  @Override
  public List<PluginFolderInterface> getPluginFolders() {
    return pluginFolders;
  }

  /**
   * @param pluginFolders
   *          the pluginFolders to set
   */
  public void setPluginFolders( List<PluginFolderInterface> pluginFolders ) {
    this.pluginFolders = pluginFolders;
  }

  protected static String getCodedTranslation( String codedString ) {
    if ( codedString == null ) {
      return null;
    }

    if ( codedString.startsWith( "i18n:" ) ) {
      String[] parts = codedString.split( ":" );
      if ( parts.length != 3 ) {
        return codedString;
      } else {
        return BaseMessages.getString( parts[1], parts[2] );
      }
    } else {
      return codedString;
    }
  }

  protected static String getTranslation( String string, String packageName, String altPackageName,
    Class<?> resourceClass ) {
    if ( string == null ) {
      return null;
    }

    if ( string.startsWith( "i18n:" ) ) {
      String[] parts = string.split( ":" );
      if ( parts.length != 3 ) {
        return string;
      } else {
        return BaseMessages.getString( parts[1], parts[2] );
      }
    } else {
      // Try the default package name
      //
      String translation;
      if ( !Utils.isEmpty( packageName ) ) {
        LogLevel oldLogLevel = DefaultLogLevel.getLogLevel();

        // avoid i18n messages for missing locale
        //
        DefaultLogLevel.setLogLevel( LogLevel.BASIC );

        translation = BaseMessages.getString( packageName, string, resourceClass );
        if ( translation.startsWith( "!" ) && translation.endsWith( "!" ) ) {
          translation = BaseMessages.getString( PKG, string, resourceClass );
        }

        // restore loglevel, when the last alternative fails, log it when loglevel is detailed
        //
        DefaultLogLevel.setLogLevel( oldLogLevel );
        if ( !Utils.isEmpty( altPackageName ) && translation.startsWith( "!" ) && translation.endsWith( "!" ) ) {
          translation = BaseMessages.getString( altPackageName, string, resourceClass );
        }
      } else {
        // Translations are not supported, simply keep the original text.
        //
        translation = string;
      }

      return translation;
    }
  }

  /*
  protected List<JarFileAnnotationPlugin> findAnnotatedClassFiles( String annotationClassName ) {
    JarFileCache jarFileCache = JarFileCache.getInstance();
    List<JarFileAnnotationPlugin> classFiles = new ArrayList<>();

    // We want to scan the plugins folder for plugin.xml files...
    //
    for ( PluginFolderInterface pluginFolder : getPluginFolders() ) {

      if ( pluginFolder.isPluginAnnotationsFolder() ) {

        FileObject[] fileObjects = null;
        try {
          // Get all the jar files in the plugin folder...
          //
          fileObjects = jarFileCache.getFileObjects( pluginFolder );
        } catch ( Exception e ) {
          log.logError( e.getMessage(), e );
        }

        if ( fileObjects != null ) {
          for ( FileObject fileObject : fileObjects ) {
            // These are the jar files : find annotations in it...
            //
            try {
              AnnotationDB annotationDB = jarFileCache.getAnnotationDB( fileObject );
              Set<String> impls = annotationDB.getAnnotationIndex().get( annotationClassName );
              if ( impls != null ) {

                for ( String fil : impls ) {
                  classFiles.add( new JarFileAnnotationPlugin( fil, fileObject.getURL(), fileObject
                    .getParent().getURL() ) );
                }
              }
            } catch ( Exception jarPluginLoadError ) {
              LogChannel.GENERAL.logError( "Error while finding annotations for jar plugin: '"
                + fileObject + "'" );
              LogChannel.GENERAL.logDebug( "Error while finding annotations for jar plugin: '"
                + fileObject + "'", jarPluginLoadError );
            }
          }
        }
      }
    }
    return classFiles;
  }
  
  */

  /**
   * This method allows for custom registration of plugins that are on the main classpath. This was originally created
   * so that test environments could register test plugins programmatically.
   *
   * @param clazz
   *          the plugin implementation to register
   * @param cat
   *          the category of the plugin
   * @param id
   *          the id for the plugin
   * @param name
   *          the name for the plugin
   * @param desc
   *          the description for the plugin
   * @param image
   *          the image for the plugin
   * @throws KettlePluginException
   */
  public void registerCustom( Class<?> clazz, String cat, String id, String name, String desc, String image ) throws KettlePluginException {
    Class<? extends PluginTypeInterface> pluginType = getClass();
    Map<Class<?>, String> classMap = new HashMap<>();
    PluginMainClassType mainClassTypesAnnotation = pluginType.getAnnotation( PluginMainClassType.class );
    classMap.put( mainClassTypesAnnotation.value(), clazz.getName() );
    PluginInterface stepPlugin =
      new Plugin(
        new String[] { id }, pluginType, mainClassTypesAnnotation.value(), cat, name, desc, image, false,
        false, classMap, new ArrayList<String>(), null, null, null, null, null );
    registry.registerPlugin( pluginType, stepPlugin );
  }

  /**
   *
   * @param input
   * @param localizedMap
   * @return
   */
  public static String getAlternativeTranslation( String input, Map<String, String> localizedMap ) {
    if ( Utils.isEmpty( input ) ) {
      return null;
    }

    if ( input.startsWith( "i18n" ) ) {
      return getCodedTranslation( input );
    } else {
      for ( final Locale locale : GlobalMessageUtil.getActiveLocales() ) {
        String alt = localizedMap.get( locale.toString().toLowerCase() );
        if ( !Utils.isEmpty( alt ) ) {
          return alt;
        }
      }
      // Nothing found? Return the original!
      return input;
    }
  }

 
  /**
   * Create a new URL class loader with the jar file specified. Also include all the jar files in the lib folder next to
   * that file.
   *
   * @param jarFileUrl
   *          The jar file to include
   * @param classLoader
   *          the parent class loader to use
   * @return The URL class loader
   */
  protected URLClassLoader createUrlClassLoader( URL jarFileUrl, ClassLoader classLoader ) {
    List<URL> urls = new ArrayList<>();

    // Also append all the files in the underlying lib folder if it exists...
    //
    try {
      String libFolderName = new File( URLDecoder.decode( jarFileUrl.getFile(), "UTF-8" ) ).getParent()
        + Const.FILE_SEPARATOR + "lib";
      if ( new File( libFolderName ).exists() ) {
        PluginFolder pluginFolder = new PluginFolder( libFolderName, false, true, searchLibDir );
        FileObject[] libFiles = pluginFolder.findJarFiles( true );
        for ( FileObject libFile : libFiles ) {
          urls.add( libFile.getURL() );
        }
      }
    } catch ( Exception e ) {
      LogChannel.GENERAL.logError( "Unexpected error searching for jar files in lib/ folder next to '"
        + jarFileUrl + "'", e );
    }

    urls.add( jarFileUrl );

    return new KettleURLClassLoader( urls.toArray( new URL[urls.size()] ), classLoader );
  }

  protected abstract String extractID( Annotation annotation );

  protected abstract String extractName( Annotation annotation );

  protected abstract String extractDesc( Annotation annotation );

  protected abstract String extractCategory( Annotation annotation );

  protected abstract String extractImageFile( Annotation annotation );

  protected abstract boolean extractSeparateClassLoader( Annotation annotation );

  protected abstract String extractI18nPackageName( Annotation annotation );

  protected abstract String extractDocumentationUrl( Annotation annotation );

  protected abstract String extractSuggestion( Annotation annotation );

  protected abstract String extractCasesUrl( Annotation annotation );

  protected abstract String extractForumUrl( Annotation annotation );

  @SuppressWarnings( "squid:S1172" )  //Overriding classes use the parameter
  protected String extractClassLoaderGroup( Annotation annotation ) {
    return null;
  }

  /**
   * When set to true the PluginFolder objects created by this type will be instructed to search for additional plugins
   * in the lib directory of plugin folders.
   *
   * @param transverseLibDirs
   */
  protected void setTransverseLibDirs( boolean transverseLibDirs ) {
    this.searchLibDir = transverseLibDirs;
  }

  /*
  protected void registerPluginJars() throws KettlePluginException {
    List<JarFileAnnotationPlugin> jarFilePlugins = findAnnotatedClassFiles( pluginClass.getName() );
    for ( JarFileAnnotationPlugin jarFilePlugin : jarFilePlugins ) {

      URLClassLoader urlClassLoader =
        createUrlClassLoader( jarFilePlugin.getJarFile(), getClass().getClassLoader() );

      try {
        Class<?> clazz = urlClassLoader.loadClass( jarFilePlugin.getClassName() );
        if ( clazz == null ) {
          throw new KettlePluginException( "Unable to load class: " + jarFilePlugin.getClassName() );
        }
        List<String> libraries = Arrays.stream( urlClassLoader.getURLs() )
          .map( URL::getFile )
          .collect( Collectors.toList() );
        Annotation annotation = clazz.getAnnotation( pluginClass );

        handlePluginAnnotation( clazz, annotation, libraries, false, jarFilePlugin.getPluginFolder() );
      } catch ( Exception e ) {
        // Ignore for now, don't know if it's even possible.
        LogChannel.GENERAL.logError(
          "Unexpected error registering jar plugin file: " + jarFilePlugin.getJarFile(), e );
      } finally {
        if ( urlClassLoader instanceof KettleURLClassLoader ) {
          ( (KettleURLClassLoader) urlClassLoader ).closeClassLoader();
        }
      }
    }
  }
  */

  /**
   * Handle an annotated plugin
   *
   * @param clazz
   *          The class to use
   * @param annotation
   *          The annotation to get information from
   * @param libraries
   *          The libraries to add
   * @param nativePluginType
   *          Is this a native plugin?
   * @param pluginFolder
   *          The plugin folder to use
   * @throws KettlePluginException
   */
  @Override
  public void handlePluginAnnotation( Class<?> clazz, Annotation annotation,
    List<String> libraries, boolean nativePluginType, URL pluginFolder ) throws KettlePluginException {

    String idList = extractID( annotation );
    if ( Utils.isEmpty( idList ) ) {
      throw new KettlePluginException( "No ID specified for plugin with class: " + clazz.getName() );
    }

    // Only one ID for now
    String[] ids = idList.split( "," );

    String packageName = extractI18nPackageName( annotation );
    String altPackageName = clazz.getPackage().getName();
    String pluginName = getTranslation( extractName( annotation ), packageName, altPackageName, clazz );
    String description = getTranslation( extractDesc( annotation ), packageName, altPackageName, clazz );
    String category = getTranslation( extractCategory( annotation ), packageName, altPackageName, clazz );
    String imageFile = extractImageFile( annotation );
    boolean separateClassLoader = extractSeparateClassLoader( annotation );
    String documentationUrl = extractDocumentationUrl( annotation );
    String casesUrl = extractCasesUrl( annotation );
    String forumUrl = extractForumUrl( annotation );
    String suggestion = getTranslation( extractSuggestion( annotation ), packageName, altPackageName, clazz );
    String classLoaderGroup = extractClassLoaderGroup( annotation );

    pluginName += addDeprecation( category );

    Map<Class<?>, String> classMap = new HashMap<>();

    PluginMainClassType mainType = getClass().getAnnotation( PluginMainClassType.class );

    classMap.put( mainType.value(), clazz.getName() );

    addExtraClasses( classMap, clazz, annotation );

    PluginInterface plugin =
      new Plugin(
        ids, this.getClass(), mainType.value(), category, pluginName, description, imageFile, separateClassLoader,
        classLoaderGroup, nativePluginType, classMap, libraries, null, pluginFolder, documentationUrl,
        casesUrl, forumUrl, suggestion );

    ParentFirst parentFirstAnnotation = clazz.getAnnotation( ParentFirst.class );
    if ( parentFirstAnnotation != null ) {
      registry.addParentClassLoaderPatterns( plugin, parentFirstAnnotation.patterns() );
    }
    registry.registerPlugin( this.getClass(), plugin );

    if ( libraries != null && !libraries.isEmpty() ) {
      LogChannel.GENERAL.logDetailed( "Plugin with id ["
        + ids[0] + "] has " + libraries.size() + " libaries in its private class path" );
    }
  }

  /**
   * Extract extra classes information from a plugin annotation.
   *
   * @param classMap
   * @param clazz
   * @param annotation
   */
  protected abstract void addExtraClasses( Map<Class<?>, String> classMap, Class<?> clazz, Annotation annotation );

  public static String addDeprecation( String category ) {
    String deprecated = BaseMessages.getString( PKG, "PluginRegistry.Category.Deprecated" );
    if ( deprecated.equals( category )  ) {
      return " (" + deprecated.toLowerCase() + ")";
    }
    return "";
  }
}
