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
import org.xwiki.contrib.translator.model.GlossaryInfo;
import org.xwiki.contrib.translator.model.LocalePair;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

@Component
@Named("translator")
@Singleton
public class TranslatorScriptService implements ScriptService
{
    @Inject
    private TranslatorConfiguration translatorConfiguration;

    @Inject
    private TranslatorManager translatorManager;

    @Inject
    private Logger logger;

    @Inject
    private ContextualAuthorizationManager authorizationManager;

    public TranslationSet getTranslations(DocumentReference reference) throws TranslatorException
    {
        Translator translator = translatorManager.getTranslator();
        if (translator == null) {
            return null;
        }
        return translator.getTranslations(reference);
    }

    public boolean isTranslatable(DocumentReference reference) throws TranslatorException
    {
        Translator translator = translatorManager.getTranslator();
        if (translator == null) {
            return false;
        }
        return translatorManager.getTranslator().isTranslatable(reference);
    }

    public boolean canTranslate(DocumentReference reference) throws TranslatorException
    {
        Translator translator = translatorManager.getTranslator();
        if (translator == null) {
            return false;
        }
        return translatorManager.getTranslator().canTranslate(reference);
    }

    public boolean canTranslate(DocumentReference reference, Locale toLocale) throws TranslatorException
    {
        Translator translator = translatorManager.getTranslator();
        if (translator == null) {
            return false;
        }
        return translatorManager.getTranslator().canTranslate(reference, toLocale);
    }

    public boolean isSameNameTranslationNamingStrategy(EntityReference reference) throws TranslatorException
    {
        Translator translator = translatorManager.getTranslator();
        if (translator == null) {
            return false;
        }
        return translator.isSameNameTranslationNamingStrategy(reference);
    }

    public void translate(EntityReference reference, Locale toLocale) throws Exception
    {
        translatorManager.getTranslator().translate(reference, toLocale);
    }

    public String translate(String content, Locale from, Locale to, boolean html) throws Exception
    {
        Translator translator = translatorManager.getTranslator();
        return translator.translate(content, from, to, html);
    }

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

    public Set<String> getAvailableTranslators()
    {
        return translatorManager.getAvailableTranslators();
    }

    public String getName()
    {
        Translator translator = translatorManager.getTranslator();
        if (translator == null) {
            return "";
        }
        return translator.getName();
    }

    public DocumentReference computeTranslationReference(DocumentReference originalDocument, String translationTitle,
        Locale translationLocale) throws TranslatorException
    {
        Translator translator = translatorManager.getTranslator();
        if (translator == null) {
            return originalDocument;
        }
        return translator.computeTranslationReference(originalDocument, translationTitle, translationLocale);
    }

    public List<GlossaryInfo> getGlossaries() throws TranslatorException
    {
        Translator translator = translatorManager.getTranslator();
        if (this.authorizationManager.hasAccess(Right.PROGRAM)) {
            return translator.getGlossaries();
        } else {
            return new ArrayList<>(0);
        }
    }

    public Map<String, String> getGlossaryEntryDetails(String id) throws TranslatorException
    {
        Translator translator = translatorManager.getTranslator();
        if (this.authorizationManager.hasAccess(Right.PROGRAM)) {
            return translator.getGlossaryEntryDetails(id);
        } else {
            return new HashMap<>(0);
        }
    }

    public List<LocalePair> getGlossaryLanguagePairs() throws TranslatorException
    {
        Translator translator = translatorManager.getTranslator();
        if (this.authorizationManager.hasAccess(Right.PROGRAM)) {
            return translator.getGlossaryLocalePairs();
        } else {
            return new ArrayList<>(0);
        }
    }
}
