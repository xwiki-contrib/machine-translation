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

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.translator.model.GlossaryInfo;
import org.xwiki.contrib.translator.model.LocalePairs;
import org.xwiki.contrib.translator.model.GlossaryUpdateEntry;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

// TODO: AdminGroup view TranslatorConfiguration but this is local group -> check
// TODO: check rights in Java
// OK: Check that current user has edit right in the target space
// TODO: Check what happens when language is not set (empty)
// TODO: check if creating a Translator is time consuming (-> do not recreate) or not
// TODO: maybe use same mechanism as shareinline to load the translate modal when the button is hit
// TODO: later manage country specific : override keys in FR_CH from FR to have specific translations only
// TODO: check what happens if the original page itself is translated natively (same name)
// TODO: review TranslationSet
// TODO: use glossary on translation when available

/**
 * Represents a translation service.
 *
 * @version $Id$
 */
@Role
public interface Translator
{
    /**
     * Translates a given page into given locale.
     * @param reference A page reference
     * @param locale Destination language
     * @throws TranslatorException in case an error occurs
     */
    void translate(EntityReference reference, Locale locale) throws TranslatorException;

    /**
     *
     * Translates a given page to a set of locales.
     * @param reference A page reference
     * @param toLocales Target lcoales
     * @throws TranslatorException in case an error occurs
     */
    void translate(EntityReference reference, Locale[] toLocales) throws TranslatorException;

    /**
     * Computes the location of a translation based on the location of the original one, its title and its locale.
     *
     * @param originalDocument A page refeence
     * @param translationTitle Title of the translation
     * @param translationLocale Locale of the translation
     * @return location of the translation page to be created or updated
     * @throws TranslatorException in case an error occurs
     */
    DocumentReference computeTranslationReference(DocumentReference originalDocument, String translationTitle,
        Locale translationLocale) throws TranslatorException;

    /**
     * Translated content from a locale to another one, taking into account html formatting or not.
     *
     * @param content A given string
     * @param from From locale
     * @param to To locale
     * @param html true if the string to be translated uses HTML
     * @return translated content
     * @throws TranslatorException in case an error occurs
     */
    String translate(String content, Locale from, Locale to, boolean html) throws TranslatorException;

    /**
     * Checks if a given page is translated according to the rules defined in the translator configuration.
     *
     * @param reference A page reference
     * @return true if the page is meant to be translated
     * @throws TranslatorException in case an error occurs
     */
    boolean isTranslatable(DocumentReference reference) throws TranslatorException;

    /**
     * Checks if the current user is allowed to create a translation of a given page in a given locale.
     *
     * @param reference A page reference
     * @param toLocale Target lcoale
     * @return true if the current user is allowed to create or update the page translation
     * @throws TranslatorException in case an error occurs
     */
    boolean canTranslate(DocumentReference reference, Locale toLocale) throws TranslatorException;

    /**
     * Checks if the current user is allowed to create a translation of a given page.
     *
     * @param reference A page reference
     * @return true if user is allowed
     * @throws TranslatorException in case an error occurs
     */
    boolean canTranslate(DocumentReference reference) throws TranslatorException;

    /**
     * Returns list of existing translations of a given page.
     *
     * @param reference A page reference
     * @return list of page translations
     * @throws TranslatorException in case an error occurs
     */
    TranslationSet getTranslations(EntityReference reference) throws TranslatorException;

    /**
     * Normalizes the string representation of a given locale. Useful for instance for some translators supporting
     * only language representation without the country, e.g "en" vs "en-GB".
     *
     * @param locale A given locale
     * @return normalized string representation
     */
    String normalizeLocale(Locale locale);

    /**
     * Translator name.
     * @return name
     */
    String getName();

    /**
     * Returns true if the current translation naming strategy is "same location as original document".
     *
     * @param reference A page reference
     * @return true if location is "same"
     * @throws TranslatorException in case an error occurs
     */
    boolean isSameNameTranslationNamingStrategy(EntityReference reference) throws TranslatorException;

    /**
     * Get the glossary name for the specificed locales.
     *
     * @param source locale for the source lang
     * @param target locale for the target lang
     * @return name of the glossary
     */
    String getGlossaryName(Locale source, Locale target);

    /**
     * Get the glossary name for the specificed locales.
     *
     * @param source locale for the source lang
     * @param target locale for the target lang
     * @param prefix prefix to use for name calculation. Note that this is mostly used for performance, to avoid to
     * retrieve many times the prefix in case of multiple call.
     * @return name of the glossary
     */
    String getGlossaryName(Locale source, Locale target, String prefix);

    /**
     * @return the prefix name of the glossaries for this Wiki instance.
     */
    String getGlossaryNamePrefix();

    /**
     * @return a list of all available language pair.
     */
    List<LocalePairs> getGlossaryLocalePairs() throws TranslatorException;

    /**
     * @return a list of glossaries available on translator service.
     */
    List<GlossaryInfo> getGlossaries() throws TranslatorException;

    /**
     * @param id glossary id
     * @return a map with source lang, target lang
     */
    Map<String, String> getGlossaryEntryDetails(String id) throws TranslatorException;

    /**
     * Update the glossary entries on the translator provider.
     *
     * @param entries the list of all glossaries with all entries to update
     */
    void updateGlossaries(List<GlossaryUpdateEntry> entries) throws TranslatorException;
}
