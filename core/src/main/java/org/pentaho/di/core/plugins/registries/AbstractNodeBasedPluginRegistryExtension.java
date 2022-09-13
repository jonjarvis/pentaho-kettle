package org.pentaho.di.core.plugins.registries;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettlePluginException;
import org.pentaho.di.core.plugins.BasePluginType;
import org.pentaho.di.core.plugins.Plugin;
import org.pentaho.di.core.plugins.PluginExtraClassTypes;
import org.pentaho.di.core.plugins.PluginInterface;
import org.pentaho.di.core.plugins.PluginMainClassType;
import org.pentaho.di.core.plugins.PluginRegistry;
import org.pentaho.di.core.plugins.PluginRegistryExtension;
import org.pentaho.di.core.plugins.PluginTypeInterface;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.w3c.dom.Node;

public abstract class AbstractNodeBasedPluginRegistryExtension implements PluginRegistryExtension {

  protected PluginRegistry registry;
  
  @Override
  public void init( PluginRegistry registry ) {
    this.registry = registry;
  }

  @Override
  public String getPluginId( Class<? extends PluginTypeInterface> pluginType, Object pluginClass ) {
    return null;
  }

  protected PluginInterface registerPluginFromXmlNode( Node pluginNode, String path,
      PluginTypeInterface pluginType, boolean nativePlugin, URL pluginFolder )
    throws KettlePluginException {
    try {

      String idAttr = XMLHandler.getTagAttribute( pluginNode, "id" );
      String description = getTagOrAttribute( pluginNode, "description" );
      String iconfile = getTagOrAttribute( pluginNode, "iconfile" );
      String tooltip = getTagOrAttribute( pluginNode, "tooltip" );
      String category = getTagOrAttribute( pluginNode, "category" );
      String classname = getTagOrAttribute( pluginNode, "classname" );
      String errorHelpfile = getTagOrAttribute( pluginNode, "errorhelpfile" );
      String documentationUrl = getTagOrAttribute( pluginNode, "documentation_url" );
      String casesUrl = getTagOrAttribute( pluginNode, "cases_url" );
      String forumUrl = getTagOrAttribute( pluginNode, "forum_url" );
      String suggestion = getTagOrAttribute( pluginNode, "suggestion" );

      Node libsnode = XMLHandler.getSubNode( pluginNode, "libraries" );
      int nrlibs = XMLHandler.countNodes( libsnode, "library" );

      List<String> jarFiles = new ArrayList<>();
      if ( path != null ) {
        for ( int j = 0; j < nrlibs; j++ ) {
          Node libnode = XMLHandler.getSubNodeByNr( libsnode, "library", j );
          String jarfile = XMLHandler.getTagAttribute( libnode, "name" );
          jarFiles.add( new File( path + Const.FILE_SEPARATOR + jarfile ).getAbsolutePath() );
        }
      }

      // Localized categories, descriptions and tool tips
      //
      Map<String, String> localizedCategories = readPluginLocale( pluginNode, "localized_category", "category" );
      category = BasePluginType.getAlternativeTranslation( category, localizedCategories );

      Map<String, String> localDescriptions =
          readPluginLocale( pluginNode, "localized_description", "description" );
      description = BasePluginType.getAlternativeTranslation( description, localDescriptions );
      description += BasePluginType.addDeprecation( category );

      suggestion = BasePluginType.getAlternativeTranslation( suggestion, localDescriptions );

      Map<String, String> localizedTooltips = readPluginLocale( pluginNode, "localized_tooltip", "tooltip" );
      tooltip = BasePluginType.getAlternativeTranslation( tooltip, localizedTooltips );

      String iconFilename = ( path == null ) ? iconfile : path + Const.FILE_SEPARATOR + iconfile;
      String errorHelpFileFull = errorHelpfile;
      if ( !Utils.isEmpty( errorHelpfile ) ) {
        errorHelpFileFull = ( path == null ) ? errorHelpfile : path + Const.FILE_SEPARATOR + errorHelpfile;
      }

      Map<Class<?>, String> classMap = new HashMap<>();

      PluginMainClassType mainClassTypesAnnotation = pluginType.getClass().getAnnotation( PluginMainClassType.class );
      classMap.put( mainClassTypesAnnotation.value(), classname );

      // process annotated extra types
      PluginExtraClassTypes classTypesAnnotation = pluginType.getClass().getAnnotation( PluginExtraClassTypes.class );
      if ( classTypesAnnotation != null ) {
        for ( int i = 0; i < classTypesAnnotation.classTypes().length; i++ ) {
          Class<?> classType = classTypesAnnotation.classTypes()[i];
          String className = getTagOrAttribute( pluginNode, classTypesAnnotation.xmlNodeNames()[i] );

          classMap.put( classType, className );
        }
      }

      // process extra types added at runtime
      Map<Class<?>, String> objectMap = pluginType.getAdditionalRuntimeObjectTypes();
      for ( Map.Entry<Class<?>, String> entry : objectMap.entrySet() ) {
        String clzName = getTagOrAttribute( pluginNode, entry.getValue() );
        classMap.put( entry.getKey(), clzName );
      }

      PluginInterface pluginInterface =
          new Plugin(
              idAttr.split( "," ), pluginType.getClass(), mainClassTypesAnnotation.value(), category, description, tooltip,
              iconFilename, false, nativePlugin, classMap, jarFiles, errorHelpFileFull, pluginFolder,
              documentationUrl, casesUrl, forumUrl, suggestion );
      registry.registerPlugin( pluginType.getClass(), pluginInterface );

      return pluginInterface;
    } catch ( Exception e ) {
      throw new KettlePluginException( BaseMessages.getString(
        BasePluginType.class, "BasePluginType.RuntimeError.UnableToReadPluginXML.PLUGIN0001" ), e );
    }
  }
  
  protected Map<String, String> readPluginLocale( Node pluginNode, String localizedTag, String translationTag ) {
    Map<String, String> map = new HashMap<>();

    Node locTipsNode = XMLHandler.getSubNode( pluginNode, localizedTag );
    int nrLocTips = XMLHandler.countNodes( locTipsNode, translationTag );
    for ( int j = 0; j < nrLocTips; j++ ) {
      Node locTipNode = XMLHandler.getSubNodeByNr( locTipsNode, translationTag, j );
      if ( locTipNode != null ) {
        String locale = XMLHandler.getTagAttribute( locTipNode, "locale" );
        String locTip = XMLHandler.getNodeValue( locTipNode );

        if ( !Utils.isEmpty( locale ) && !Utils.isEmpty( locTip ) ) {
          map.put( locale.toLowerCase(), locTip );
        }
      }
    }

    return map;
  }
  
  protected String getTagOrAttribute( Node pluginNode, String tag ) {
    String string = XMLHandler.getTagValue( pluginNode, tag );
    if ( string == null ) {
      string = XMLHandler.getTagAttribute( pluginNode, tag );
    }
    return string;
  }

}
