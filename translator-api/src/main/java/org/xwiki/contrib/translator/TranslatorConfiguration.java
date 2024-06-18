/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.translator;

import org.xwiki.component.annotation.Role;

/**
 * Define the configuration of the translator to be used.
 *
 * @version $Id$
 */
@Role
public interface TranslatorConfiguration
{
    /**
     * Retrieves name of the current Translator.
     * @return the name of the current {@link Translator} to be used.
     */
    String getTranslator();

    /**
     *
     * @return true if the current strategy is "translation location is the same as the original document"
     */
    boolean isSameNameTranslationNamingStrategy();

    /**
     *
     * @return list of XClasses that can be translated as separated by a comma
     */
    String getTargetClasses();

    /**
     *
     * @return list of XProperties that should be translated, separated by a comma
     */
    String getTargetProperties();

    /**
     *
     * @return API key to be used when cmmmunicating with the remote API.
     */
    String getApiKey();

    /**
     * Prefix to be used when creating or retrieving glossaries.
     * @return prefix to be used
     */
    String getGlossaryNamePrefix();

    /**
     * @return list of XClasses for which the translations should be at same location as the original document,
     * whatever the translation name strategy.
     */
    String getSameNameTranslationClasses();
}
