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
import org.xwiki.contrib.translator.model.GlossaryLocalePairs;
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
// TODO: Introduce TranslatorException
// TODO: review TranslationSet
// TODO: use glossary on translation when available

@Role
public interface Translator
{
    void translate(EntityReference reference, Locale locale) throws TranslatorException;

    void translate(EntityReference reference, Locale[] toLocales) throws TranslatorException;

    DocumentReference computeTranslationReference(DocumentReference originalDocument, String translationTitle,
        Locale translationLocale) throws TranslatorException;

    String translate(String content, Locale from, Locale to, boolean html) throws TranslatorException;

    boolean isTranslatable(DocumentReference reference) throws TranslatorException;

    boolean canTranslate(DocumentReference reference, Locale toLocale) throws TranslatorException;

    boolean canTranslate(DocumentReference reference) throws TranslatorException;

    TranslationSet getTranslations(EntityReference reference) throws TranslatorException;

    String normalizeLocale(Locale locale);

    String getName();

    boolean isSameNameTranslationNamingStrategy(EntityReference reference) throws TranslatorException;

    /**
     * Get the glossary name for the specificed locales
     *
     * @param source locale for the source lang
     * @param target locale for the target lang
     * @return name of the glossary
     */
    String getGlossaryName(Locale source, Locale target);

    /**
     * @return the prefix name of the glossaries for this Wiki instance
     */
    String getGlossaryNamePrefix();

    /**
     * @return a list of all available language pair
     */
    List<GlossaryLocalePairs> getGlossaryLocalePairs() throws TranslatorException;

    /**
     * @return a list of glossaries available on translator service
     */
    List<GlossaryInfo> getGlossaries() throws TranslatorException;

    /**
     * @param id glossary id
     * @return a map with source lang, target lang
     */
    Map<String, String> getGlossaryEntryDetails(String id) throws TranslatorException;

    /**
     * Update the glossary entries on the translator provider
     *
     * @param entries the list of all glossaries with all entries to update
     */
    void updateGlossaries(List<GlossaryUpdateEntry> entries) throws TranslatorException;
}
