package org.pentaho.di.core.plugins;

public interface XMLPluginTypeInterface {

  /**
   * This method returns the path part for populating folders of *-plugin.xml files
   * 
   * @return Path part of the plugin xml
   */
  public String getPopulateFoldersPath();
  
  /**
   * This method return parameter for registerNatives() method
   *
   * @return XML plugin file
   */
  public String getXmlPluginFile();

  /**
   * This method return parameter for registerNatives() method
   *
   * @return Alternative XML plugin file
   */
  public String getAlternativePluginFile();

  /**
   * This method return parameter for registerPlugins() method
   *
   * @return Main XML tag
   */
  public String getMainTag();

  /**
   * This method return parameter for registerPlugins() method
   *
   * @return Subordinate XML tag
   */
  public String getSubTag();

  /**
   * This method return parameter for registerPlugins() method
   *
   * @return Path
   */
  public String getPath();

  /**
   * This method return parameter for registerNatives() method
   *
   * @return Flag ("return;" or "throw exception")
   */
  public boolean isReturn();
  
  
}
