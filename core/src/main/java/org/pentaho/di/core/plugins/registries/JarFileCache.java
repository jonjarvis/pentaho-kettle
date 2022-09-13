/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2017 by Hitachi Vantara : http://www.pentaho.com
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

package org.pentaho.di.core.plugins.registries;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSelectInfo;
import org.apache.commons.vfs2.FileSelector;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleFileException;
import org.pentaho.di.core.plugins.KettleURLClassLoader;
import org.pentaho.di.core.vfs.KettleVFS;
import org.scannotation.AnnotationDB;

public class JarFileCache {

  private final ConcurrentMap<String, FileObject[]> folderMap;

  private final ConcurrentMap<FileObject, AnnotationDB> annotationMap;

  private final ConcurrentMap<URL, URLClassLoader> classLoaderMap;

  public JarFileCache() {
    annotationMap = new ConcurrentHashMap<>();
    folderMap = new ConcurrentHashMap<>();
    classLoaderMap = new ConcurrentHashMap<>();
  }

  public AnnotationDB getAnnotationDB( FileObject fileObject ) throws IOException {

    return annotationMap.computeIfAbsent( fileObject, fo -> {
      try {
        AnnotationDB db = new AnnotationDB();
        db.scanArchives( fileObject.getURL() );
        return db;
      } catch ( IOException ioe ) {
        // TODO: Warn about this;
        return null;
      }
    } );

  }

  public FileObject[] getJarFiles( String folder ) throws KettleFileException {
    return folderMap.computeIfAbsent( folder, f -> {
      try {
        return findJarFiles( f, false );
      } catch ( KettleFileException e ) {
        // TODO: Warn about this
        e.printStackTrace();
        return null;
      }
    } );
  }

  public URLClassLoader getClassLoader( URL jarFile ) {

    return classLoaderMap.computeIfAbsent( jarFile, jf -> {
      return new KettleURLClassLoader( Stream.concat(
        Stream.of( jarFile ),
        Arrays.stream( findLibFiles( jarFile ) ).<URL> flatMap( fo -> {
          try {
            return Stream.of( fo.getURL() );
          } catch ( Exception e ) {
            return Stream.<URL> empty();
          }
        } ) ).toArray( URL[]::new ), getClass().getClassLoader() );
    } );
  }

  public Collection<FileObject[]> allJarFiles() {
    return folderMap.values();
  }

  public FileObject[] findLibFiles( URL jarFileUrl ) {
    try {
      String libFolderName =
          new File( URLDecoder.decode( jarFileUrl.getFile(), "UTF-8" ) ).getParent()
              + Const.FILE_SEPARATOR + "lib";
      if ( new File( libFolderName ).exists() ) {
        return findJarFiles( libFolderName, true );
      }
    } catch ( Exception e ) {
      e.printStackTrace();
      // TODO: CLeanup
    }
    return new FileObject[0];
  }

  private FileObject[] findJarFiles( String folderName, boolean includeLibJars )
    throws KettleFileException {

    try {
      // Find all the jar files in this folder...
      FileObject folderObject = KettleVFS.getFileObject( folderName );

      return folderObject.findFiles( new FileSelector() {
        @Override
        public boolean traverseDescendents( FileSelectInfo fileSelectInfo ) throws Exception {
          FileObject fileObject = fileSelectInfo.getFile();
          String folder = fileObject.getName().getBaseName();
          FileObject kettleIgnore = fileObject.getChild( ".kettle-ignore" );
          return includeLibJars || ( kettleIgnore == null && !"lib".equals( folder ) );
        }

        @Override
        public boolean includeFile( FileSelectInfo fileSelectInfo ) throws Exception {
          FileObject file = fileSelectInfo.getFile();
          return file.isFile() && file.toString().toLowerCase().endsWith( ".jar" );
        }
      } );
    } catch ( Exception e ) {
      throw new KettleFileException( "Unable to list jar files in plugin folder '" + toString() + "'", e );
    }
  }

  public void clear() {
    annotationMap.clear();
    folderMap.clear();
  }
}
