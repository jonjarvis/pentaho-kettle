/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2018 by Hitachi Vantara : http://www.pentaho.com
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

package org.pentaho.di.core.row.value;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.plugins.BasePluginType;
import org.pentaho.di.core.plugins.PluginAnnotationType;
import org.pentaho.di.core.plugins.PluginMainClassType;
import org.pentaho.di.core.plugins.PluginTypeInterface;
import org.pentaho.di.core.plugins.XMLPluginTypeInterface;
import org.pentaho.di.core.row.ValueMetaInterface;

/**
 * This class represents the value meta plugin type.
 *
 * @author matt
 *
 */

@PluginMainClassType( ValueMetaInterface.class )
@PluginAnnotationType( ValueMetaPlugin.class )
public class ValueMetaPluginType extends BasePluginType implements PluginTypeInterface, XMLPluginTypeInterface {

  private static ValueMetaPluginType valueMetaPluginType;

  private ValueMetaPluginType() {
    super( ValueMetaPlugin.class, "VALUEMETA", "ValueMeta" );
    populateFolders( "valuemeta" );
  }

  public static ValueMetaPluginType getInstance() {
    if ( valueMetaPluginType == null ) {
      valueMetaPluginType = new ValueMetaPluginType();
    }
    return valueMetaPluginType;
  }
  
  
  @Override
  public String getPopulateFoldersPath() {
    return "valuemeta";
  }

  @Override
  public String getPath() {
    return null;
  }

  @Override
  public boolean isReturn() {
    return false;
  }
  
  @Override
  public String getXmlPluginFile() {
    return Const.XML_FILE_KETTLE_VALUEMETA_PLUGINS;
  }

  @Override
  public String getAlternativePluginFile() {
    return Const.KETTLE_VALUEMETA_PLUGINS_FILE;
  }

  @Override
  public String getMainTag() {
    return "valuemeta-plugins";
  }

  @Override
  public String getSubTag() {
    return "valuemeta-plugin";
  }

  @Override
  protected String extractCategory( Annotation annotation ) {
    return null;
  }

  @Override
  protected String extractDesc( Annotation annotation ) {
    return ( (ValueMetaPlugin) annotation ).description();
  }

  @Override
  protected String extractID( Annotation annotation ) {
    return ( (ValueMetaPlugin) annotation ).id();
  }

  @Override
  protected String extractName( Annotation annotation ) {
    return ( (ValueMetaPlugin) annotation ).name();
  }

  @Override
  protected String extractImageFile( Annotation annotation ) {
    return null;
  }

  @Override
  protected boolean extractSeparateClassLoader( Annotation annotation ) {
    return ( (ValueMetaPlugin) annotation ).isSeparateClassLoaderNeeded();
  }

  @Override
  protected String extractI18nPackageName( Annotation annotation ) {
    return null;
  }

  @Override
  protected void addExtraClasses( Map<Class<?>, String> classMap, Class<?> clazz, Annotation annotation ) {
  }

  @Override
  protected String extractDocumentationUrl( Annotation annotation ) {
    return ( (ValueMetaPlugin) annotation ).documentationUrl();
  }

  @Override
  protected String extractSuggestion( Annotation annotation ) {
    return null;
  }

  @Override
  protected String extractCasesUrl( Annotation annotation ) {
    return ( (ValueMetaPlugin) annotation ).casesUrl();
  }

  @Override
  protected String extractForumUrl( Annotation annotation ) {
    return ( (ValueMetaPlugin) annotation ).forumUrl();
  }

  @Override
  protected String extractClassLoaderGroup( Annotation annotation ) {
    return ( (ValueMetaPlugin) annotation ).classLoaderGroup();
  }

}
