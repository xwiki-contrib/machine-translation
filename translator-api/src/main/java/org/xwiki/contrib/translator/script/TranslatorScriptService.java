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
package org.xwiki.contrib.translator.script;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.translator.TranslationSet;
import org.xwiki.contrib.translator.Translator;
import org.xwiki.contrib.translator.TranslatorConfiguration;
import org.xwiki.contrib.translator.TranslatorException;
import org.xwiki.contrib.translator.TranslatorManager;
import org.xwiki.contrib.translator.model.GlossaryInfo;
import org.xwiki.contrib.translator.model.LocalePairs;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

/** Script service used to access the Translator service.
 *
 * @version $Id$
 */
@Component
@Named("translator")
@Singleton
public class TranslatorScriptService implements ScriptService
{
    /**
     * Translator configuration.
     */
    @Inject
    private TranslatorConfiguration translatorConfiguration;

    /**
     * Translator manager allowing to retrieve a specific translator implementation.
     */
    @Inject
    private TranslatorManager translatorManager;

    /**
     * Logging helper.
     */
    @Inject
    private Logger logger;

    /**
     * The authorization manager.
     */
    @Inject
    private ContextualAuthorizationManager authorizationManager;

    /**
     * Retrieves existing translation pages of a page with a given reference.
     *
     * @param reference Reference of a page
     * @return set of existing translations for that page
     * @throws TranslatorException in case an error occurs
     */
    public TranslationSet getTranslations(DocumentReference reference) throws TranslatorException
    {
        Translator translator = translatorManager.getTranslator();
        if (translator == null) {
            return null;
        }
        return translator.getTranslations(reference);
    }

    /**
     * Checks if a given reference can be translated according to the rules defined in the translator configuration.
     *
     * @param reference Reference of a page
     * @return true if the given reference can be translated, false otherwise
     * @throws TranslatorException in case an error occurs
     */
    public boolean isTranslatable(DocumentReference reference) throws TranslatorException
    {
        Translator translator = translatorManager.getTranslator();
        if (translator == null) {
            return false;
        }
        return translatorManager.getTranslator().isTranslatable(reference);
    }

    /**
     * Checks if the current user can translate a page.
     *
     * @param reference A given page reference
     * @return true if the current user can create page
     * @throws TranslatorException in case an error occurs
     */
    public boolean canTranslate(DocumentReference reference) throws TranslatorException
    {
        Translator translator = translatorManager.getTranslator();
        if (translator == null) {
            return false;
        }
        return translatorManager.getTranslator().canTranslate(reference);
    }

    /**
     * Checks if the current user can translate a page in a given locale.
     *
     * @param reference A given page reference
     * @param toLocale Locale to be translated to
     * @return true if user can translate page, false otherwise
     * @throws TranslatorException in case an error occurs
     */
    public boolean canTranslate(DocumentReference reference, Locale toLocale) throws TranslatorException
    {
        Translator translator = translatorManager.getTranslator();
        if (translator == null) {
            return false;
        }
        return translatorManager.getTranslator().canTranslate(reference, toLocale);
    }

    /**
     * Returns true in case the location of the translations of a given reference is the same as the original page.
     *
     * @param reference A given page reference
     * @return true if location strategy is "same location"
     * @throws TranslatorException in case an error occurs
     */
    public boolean isSameNameTranslationNamingStrategy(EntityReference reference) throws TranslatorException
    {
        Translator translator = translatorManager.getTranslator();
        if (translator == null) {
            return false;
        }
        return translator.isSameNameTranslationNamingStrategy(reference);
    }

    /**
     * Translates given page into iven locale.
     *
     * @param reference A page reference
     * @param toLocale A locale
     * @throws TranslatorException in case an error occurs
     */
    public void translate(EntityReference reference, Locale toLocale) throws TranslatorException
    {
        translatorManager.getTranslator().translate(reference, toLocale);
    }

    /**
     * Translates given content from given locale to another locale, optionally with html.
     *
     * @param content Given content
     * @param from Given original locale
     * @param to To target locale
     * @param html If translation should take html into account
     * @return translated content
     * @throws TranslatorException in case an error occurs
     */
    public String translate(String content, Locale from, Locale to, boolean html) throws TranslatorException
    {
        Translator translator = translatorManager.getTranslator();
        return translator.translate(content, from, to, html);
    }

    /**
     * Returns TranslatorManager.
     *
     * @return TranslatorManager
     */
    public TranslatorManager getManager()
    {
        if (this.authorizationManager.hasAccess(Right.PROGRAM)) {
            return this.translatorManager;
        } else {
            return null;
        }
    }

    /**
     * Return the translator configuration if the user has programming rights only.
     *
     * @return the configuration for translator or null.
     */
    public TranslatorConfiguration getConfiguration()
    {
        if (this.authorizationManager.hasAccess(Right.PROGRAM)) {
            return this.translatorConfiguration;
        } else {
            return null;
        }
    }

    /**
     * Returns available translators.
     *
     * @return available translators
     */
    public Set<String> getAvailableTranslators()
    {
        return translatorManager.getAvailableTranslators();
    }

    /**
     * Returns current translator name.
     *
     * @return current translator name
     */
    public String getName()
    {
        Translator translator = translatorManager.getTranslator();
        if (translator == null) {
            return "";
        }
        return translator.getName();
    }

    /**
     * Computes the location of a translation based on the original document and the translation title.
     * @param originalDocument Reference of the original document
     * @param translationTitle Title of the translation
     * @param translationLocale Translation locale
     * @return reference of the translation page
     * @throws TranslatorException in case an error occurs
     */
    public DocumentReference computeTranslationReference(DocumentReference originalDocument, String translationTitle,
        Locale translationLocale) throws TranslatorException
    {
        Translator translator = translatorManager.getTranslator();
        if (translator == null) {
            return originalDocument;
        }
        return translator.computeTranslationReference(originalDocument, translationTitle, translationLocale);
    }

    /**
     * Returns list of existing glossaries.
     * @return list of glossaries
     * @throws TranslatorException in case an error occurs
     */
    public List<GlossaryInfo> getGlossaries() throws TranslatorException
    {
        Translator translator = translatorManager.getTranslator();
        if (this.authorizationManager.hasAccess(Right.PROGRAM)) {
            return translator.getGlossaries();
        } else {
            return new ArrayList<>(0);
        }
    }

    /**
     *  Returns list of glossary entries for given glossary.
     * @param id A given glossary identifier
     * @return List of glossary entries
     * @throws TranslatorException in case an error occurs
     */
    public Map<String, String> getGlossaryEntryDetails(String id) throws TranslatorException
    {
        Translator translator = translatorManager.getTranslator();
        if (this.authorizationManager.hasAccess(Right.PROGRAM)) {
            return translator.getGlossaryEntryDetails(id);
        } else {
            return new HashMap<>(0);
        }
    }

    /**
     * Computes list of existing glossary locale pairs.
     * @return list of locale pairs
     * @throws TranslatorException in case an error occurs
     */
    public List<LocalePairs> getGlossaryLanguagePairs() throws TranslatorException
    {
        Translator translator = translatorManager.getTranslator();
        if (this.authorizationManager.hasAccess(Right.PROGRAM)) {
            return translator.getGlossaryLocalePairs();
        } else {
            return new ArrayList<>(0);
        }
    }
}
