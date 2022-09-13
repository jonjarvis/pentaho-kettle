package org.pentaho.di.core.plugins.registries;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.pentaho.di.core.exception.KettlePluginException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.plugins.PluginTypeInterface;
import org.pentaho.di.core.plugins.XMLPluginTypeInterface;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.core.xml.XMLHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.google.common.annotations.VisibleForTesting;

public class NativePluginRegistryExtension extends AbstractNodeBasedPluginRegistryExtension {

  @Override
  public void searchForType( PluginTypeInterface pluginType ) {
    
    try {
      if( pluginType instanceof XMLPluginTypeInterface ) {
        registerNatives( (XMLPluginTypeInterface ) pluginType );
      }
    } catch( Exception e ) {
      //Since the PluginRegistryExtension doesn't have KettlePluginException, we'll throw a runtime exception
      throw new RuntimeException( e );
    }
    
  }

  protected void registerNatives( XMLPluginTypeInterface xmlPluginType ) throws KettlePluginException {
    // Scan the native steps...
    String xmlFile = xmlPluginType.getXmlPluginFile();
    String alternative = null;
    if ( !Utils.isEmpty( xmlPluginType.getAlternativePluginFile() ) ) {
      alternative = getPropertyExternal( xmlPluginType.getAlternativePluginFile(), null );
      if ( !Utils.isEmpty( alternative ) ) {
        xmlFile = alternative;
      }
    }

    // Load the plugins for this file...
    //
    InputStream inputStream = null;
    try {
      inputStream = getResAsStreamExternal( xmlFile );
      if ( inputStream == null ) {
        inputStream = getResAsStreamExternal( "/" + xmlFile );
      }

      if ( !Utils.isEmpty( xmlPluginType.getAlternativePluginFile() ) && inputStream == null && !Utils.isEmpty( alternative ) ) {
        // Retry to load a regular file...
        try {
          inputStream = getFileInputStreamExternal( xmlFile );
        } catch ( Exception e ) {
          throw new KettlePluginException( "Unable to load native plugins '" + xmlFile + "'", e );
        }
      }

      if ( inputStream == null ) {
        if ( xmlPluginType.isReturn() ) {
          return;
        } else {
          throw new KettlePluginException( "Unable to find native plugins definition file: " + xmlFile );
        }
      }

      registerPlugins( inputStream, xmlPluginType );

    } catch ( KettleXMLException e ) {
      throw new KettlePluginException( "Unable to read the kettle XML config file: " + xmlFile, e );
    } finally {
      IOUtils.closeQuietly( inputStream );
    }
  }
  
  @VisibleForTesting
  protected String getPropertyExternal( String key, String def ) {
    return System.getProperty( key, def );
  }

  @VisibleForTesting
  protected InputStream getResAsStreamExternal( String name ) {
    return getClass().getResourceAsStream( name );
  }

  @VisibleForTesting
  protected InputStream getFileInputStreamExternal( String name ) throws FileNotFoundException {
    return new FileInputStream( name );
  }

  /**
   * This method registers plugins from the InputStream with the XML Resource
   *
   * @param inputStream
   * @throws KettlePluginException
   * @throws KettleXMLException
   */
  protected void registerPlugins( InputStream inputStream, XMLPluginTypeInterface xmlPluginType ) throws KettlePluginException, KettleXMLException {
    Document document = XMLHandler.loadXMLFile( inputStream, null, true, false );

    Node repsNode = XMLHandler.getSubNode( document, xmlPluginType.getMainTag() );
    List<Node> repsNodes = XMLHandler.getNodes( repsNode, xmlPluginType.getSubTag() );

    for ( Node repNode : repsNodes ) {
      registerPluginFromXmlNode( repNode, xmlPluginType.getPath(), (PluginTypeInterface) xmlPluginType, true, null );
    }
  }

}
