/**
 * $Id$
 */
package com.untangle.uvm;

import java.util.List;
import java.util.Map;

import org.apache.commons.fileupload.FileItem;

/**
 * Allows the user to customize the language of the product.
 *
 */
public interface LanguageManager
{
    /**
     * Get the settings.
     *
     * @return the settings.
     */
    LanguageSettings getLanguageSettings();

    /**
     * Set the settings.
     *
     * @param langSettings the settings.
     */
    void setLanguageSettings(LanguageSettings langSettings);
    
    /**
     * Upload New Language Pack
     * 
     * @return true if the uploaded language pack was processed with no errors; otherwise returns false 
     */
    boolean uploadLanguagePack(FileItem item) throws UvmException;
    
    
    /**
     * Get list of available languages
     */
    List<LocaleInfo> getLanguagesList();
    
    /**
     * Return the map of translations for a module, for the current language
     * 
     * @param module  the name of the module
     * @return map of translations 
     */
    Map<String, String> getTranslations(String module);

    /**
     * Interval to keep cached translation maps
     */
    int CLEANER_LAST_ACCESS_MAX_TIME = 5 * 60 * 1000; /* Expire if unused for 5 minutes */

}
