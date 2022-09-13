package org.pentaho.di.core.plugins.registries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSelectInfo;
import org.apache.commons.vfs2.FileSelector;
import org.pentaho.di.core.exception.KettlePluginException;
import org.pentaho.di.core.plugins.PluginFolderInterface;
import org.pentaho.di.core.plugins.PluginTypeInterface;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.core.xml.XMLHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class XMLPluginFolderPluginRegistryExtension extends AbstractNodeBasedPluginRegistryExtension {

  private List<PluginFolderInterface> pluginFolders = new ArrayList();

  @Override
  public void searchForType( PluginTypeInterface pluginType ) {
    
    try {
      registerXmlPlugins( pluginType );
    } catch( Exception e ) {
      //TODO: Better logging
      e.printStackTrace();
    }
    
  }

  protected void registerXmlPlugins( PluginTypeInterface pluginType ) throws KettlePluginException {
    for ( PluginFolderInterface folder : pluginFolders ) {

      if ( folder.isPluginXmlFolder() ) {
        List<FileObject> pluginXmlFiles = findPluginXmlFiles( folder.getFolder() );
        for ( FileObject file : pluginXmlFiles ) {

          try {
            Document document = XMLHandler.loadXMLFile( file );
            Node pluginNode = XMLHandler.getSubNode( document, "plugin" );
            if ( pluginNode != null ) {
              registerPluginFromXmlNode( pluginNode, KettleVFS.getFilename( file.getParent() ), pluginType,
                false, file.getParent().getURL() );
            }
          } catch ( Exception e ) {
            // We want to report this plugin.xml error, perhaps an XML typo or something like that...
            //
            // log.logError( "Error found while reading step plugin.xml file: " + file.getName().toString(), e );
            //TODO: Fix this with a log
            e.printStackTrace();
          }
        }
      }
    }
  }

  protected List<FileObject> findPluginXmlFiles( String folder ) {

    return findPluginFiles( folder, ".*\\/plugin\\.xml$" );
  }

  protected List<FileObject> findPluginFiles( String folder, final String regex ) {

    List<FileObject> list = new ArrayList<>();
    try {
      FileObject folderObject = KettleVFS.getFileObject( folder );
      FileObject[] files = folderObject.findFiles( new FileSelector() {

        @Override
        public boolean traverseDescendents( FileSelectInfo fileSelectInfo ) throws Exception {
          return true;
        }

        @Override
        public boolean includeFile( FileSelectInfo fileSelectInfo ) throws Exception {
          return fileSelectInfo.getFile().toString().matches( regex );
        }
      } );
      if ( files != null ) {
        Collections.addAll( list, files );
      }
    } catch ( Exception e ) {
      // ignore this: unknown folder, insufficient permissions, etc
    }
    return list;
  }

}
