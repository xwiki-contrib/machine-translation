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
package org.xwiki.contrib.machinetranslation;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.machinetranslation.model.Glossary;
import org.xwiki.contrib.machinetranslation.model.GlossaryInfo;
import org.xwiki.contrib.machinetranslation.model.LocalePair;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Represents a translation service.
 *
 * @version $Id$
 * @since 1.0
 */
@Role
@Unstable
public interface Translator
{
    /**
     * Used to specify which type normalisation.
     */
    enum NormalisationType
    {
        /**
         * Source language of the translator.
         */
        SOURCE_LANG,
        /**
         * Target language of the translator.
         */
        TARGET_LANG,
        /**
         * Source language for the glossary.
         */
        SOURCE_LANG_GLOSSARY,
        /**
         * Target language for the glossary.
         */
        TARGET_LANG_GLOSSARY,
    }

    /**
     * Translates a given page into given locale.
     *
     * @param reference A page reference
     * @param locale Destination language
     * @return Reference to the translated page
     * @throws MachineTranslationException in case an error occurs
     */
    EntityReference translate(EntityReference reference, Locale locale) throws MachineTranslationException;

    /**
     * Translates a given page to a set of locales.
     *
     * @param reference A page reference
     * @param toLocales Target lcoales
     * @throws MachineTranslationException in case an error occurs
     */
    void translate(EntityReference reference, Locale[] toLocales) throws MachineTranslationException;

    /**
     * Computes the location of a translation based on the location of the original one, its title and its locale.
     *
     * @param originalDocument A page refeence
     * @param translationTitle Title of the translation
     * @param translationLocale Locale of the translation
     * @return location of the translation page to be created or updated
     * @throws MachineTranslationException in case an error occurs
     */
    EntityReference computeTranslationReference(EntityReference originalDocument, String translationTitle,
        Locale translationLocale) throws MachineTranslationException;

    /**
     * Translated content from a locale to another one, taking into account html formatting or not.
     *
     * @param content A given string
     * @param from From locale
     * @param to To locale
     * @param html true if the string to be translated uses HTML
     * @return translated content
     * @throws MachineTranslationException in case an error occurs
     */
    String translate(String content, Locale from, Locale to, boolean html) throws MachineTranslationException;

    /**
     * Checks if a given page is translated according to the rules defined in the translator configuration.
     *
     * @param reference A page reference
     * @return true if the page is meant to be translated
     * @throws MachineTranslationException in case an error occurs
     */
    boolean isTranslatable(EntityReference reference) throws MachineTranslationException;

    /**
     * Checks if the current user is allowed to create a translation of a given page in a given locale.
     *
     * @param reference A page reference
     * @param toLocale Target lcoale
     * @return true if the current user is allowed to create or update the page translation
     * @throws MachineTranslationException in case an error occurs
     */
    boolean canTranslate(EntityReference reference, Locale toLocale) throws MachineTranslationException;

    /**
     * Checks if the current user is allowed to create a translation of a given page.
     *
     * @param reference A page reference
     * @return true if user is allowed
     * @throws MachineTranslationException in case an error occurs
     */
    boolean canTranslate(EntityReference reference) throws MachineTranslationException;

    /**
     * Returns list of existing translations of a given page.
     *
     * @param reference a document reference (containing the document locale, if any)
     * @return list of page translations
     * @throws MachineTranslationException in case an error occurs
     */
    List<MachineTranslation> getTranslations(DocumentReference reference) throws MachineTranslationException;

    /**
     * @param reference a page reference
     * @param locale a locale
     * @return reference to a translation with given locale if any, null otherwise
     * @throws MachineTranslationException in case an error occurs
     */
    MachineTranslation getTranslation(DocumentReference reference, Locale locale) throws MachineTranslationException;

    /**
     * @param reference an entity reference
     * @return DocumentReference of the original document, with its locale
     * @throws MachineTranslationException in case an error occurs
     */
    DocumentReference getOriginalDocumentReference(EntityReference reference) throws MachineTranslationException;

    /**
     * @param reference an entity reference
     * @return the original document for that reference
     * @throws MachineTranslationException in case an error occurs
     */
    XWikiDocument getOriginalDocument(EntityReference reference) throws MachineTranslationException;

    /**
     * @param reference an entity reference
     * @return Locale of the original document
     * @throws MachineTranslationException in case an error occurs
     */
    Locale getOriginalDocumentRealLocale(EntityReference reference) throws MachineTranslationException;

    /**
     * Normalizes the string representation of a given locale. Useful for instance for some translators supporting only
     * language representation without the country, e.g "en" vs "en-GB".
     *
     * @param locale A given locale
     * @param type the normalisation type to use.
     * @return normalized string representation
     */
    String normalizeLocale(Locale locale, NormalisationType type) throws MachineTranslationException;

    /**
     * Translator name.
     *
     * @return name
     */
    String getName();

    /**
     * Returns true if the current translation naming strategy is "same location as original document".
     *
     * @param reference A page reference
     * @return true if location is "same"
     * @throws MachineTranslationException in case an error occurs
     */
    boolean isSameNameTranslationNamingStrategy(EntityReference reference) throws MachineTranslationException;

    /**
     * Checks if a given document is the original document.
     *
     * @param document a document
     * @return true if the document is the original one
     * @throws MachineTranslationException in case an error occurs
     */
    boolean isOriginalDocument(Document document) throws MachineTranslationException;

    /**
     *
     * @param translation a given MachineTranslation
     * @return true if the passed MachineTranslation is the current document
     * @throws MachineTranslationException in case an error occurs
     */
    boolean isCurrentDocument(MachineTranslation translation) throws MachineTranslationException;

    /**
     * Returns the current translator usage statistics.
     *
     * @return String representing the translator usage statistics
     * @throws MachineTranslationException in case an error occurs
     */
    Usage getUsage() throws MachineTranslationException;

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
     *     retrieve many times the prefix in case of multiple call.
     * @return name of the glossary
     */
    String getGlossaryName(Locale source, Locale target, String prefix);

    /**
     * @return the prefix name of the glossaries for this Wiki instance.
     */
    String getGlossaryNamePrefix();

    /**
     * @return list the glossary locale pairs supported by the translator.
     * @throws MachineTranslationException in case an error occurs
     */
    Map<LocalePair, Boolean> getGlossaryLocalePairSupport() throws MachineTranslationException;

    /**
     * @return a list of all available language pair.
     * @throws MachineTranslationException in case an error occurs
     */
    List<LocalePair> getGlossaryLocalePairs() throws MachineTranslationException;

    /**
     * @return a list of glossaries available on translator service.
     * @throws MachineTranslationException in case an error occurs
     */
    List<GlossaryInfo> getGlossaries() throws MachineTranslationException;

    /**
     * @param id glossary id
     * @return a map with source lang, target lang
     * @throws MachineTranslationException in case an error occurs
     */
    Map<String, String> getGlossaryEntries(String id) throws MachineTranslationException;

    /**
     * Update the glossary entries on the translator provider.
     *
     * @param entries the list of all glossaries with all entries to update
     * @throws MachineTranslationException in case an error occurs
     */
    void updateGlossaries(List<Glossary> entries) throws MachineTranslationException;
}
