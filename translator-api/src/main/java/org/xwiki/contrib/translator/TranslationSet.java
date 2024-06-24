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

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

/**
 * Inner class used to represent all translations of a given reference. The class contains: a document reference, the
 * title of that reference in its default locale, its default locale, a map of locales and title / references.
 *
 * @version $Id$
 */
public class TranslationSet
{
    /**
     * Original page reference.
     */
    protected EntityReference originalDocumentReference;

    /**
     * Original page title.
     */
    protected String originalDocumentTitle;

    /**
     * Original page locale.
     */
    protected Locale originalDocumentLocale;

    /**
     * Map whose keys are translation locales, and the values the translation reference and title.
     */
    protected Map<Locale, List<Object>> translations;

    /**
     * Constructs a TranslationSet.
     *
     * @param originalDocumentReference Original document reference
     * @param originalDocumentTitle Original document title
     * @param originalDocumentLocale Original document locale
     * @param translations Map of translations
     */
    public TranslationSet(EntityReference originalDocumentReference, String originalDocumentTitle,
        Locale originalDocumentLocale, Map<Locale, List<Object>> translations)
    {
        this.originalDocumentReference = originalDocumentReference;
        this.originalDocumentTitle = originalDocumentTitle;
        this.originalDocumentLocale = originalDocumentLocale;
        this.translations = translations;
    }

    /**
     * Constructs a TranslationSet with empty map of translations.
     */
    public TranslationSet()
    {
        this.translations = new HashMap<>();
    }

    /**
     * @return original dpage reference
     */
    public EntityReference getOriginalDocumentReference()
    {
        return originalDocumentReference;
    }

    /**
     * @return original page title
     */
    public String getOriginalDocumentTitle()
    {
        return originalDocumentTitle;
    }

    /**
     * @return original page locale
     */
    public Locale getOriginalDocumentLocale()
    {
        return originalDocumentLocale;
    }

    /**
     * Map whose keys are existing translation locales, and values translation pages reference and title.
     *
     * @return map of existing translations
     */
    public Map<Locale, List<Object>> getTranslations()
    {
        return translations;
    }

    /**
     * Sets original document reference.
     *
     * @param originalDocumentReference Original document reference
     */
    public void setOriginalDocumentReference(EntityReference originalDocumentReference)
    {
        this.originalDocumentReference = originalDocumentReference;
    }

    /**
     * Sets original document title.
     *
     * @param originalDocumentTitle Original document title
     */
    public void setOriginalDocumentTitle(String originalDocumentTitle)
    {
        this.originalDocumentTitle = originalDocumentTitle;
    }

    /**
     * Sets original document locale.
     *
     * @param originalDocumentLocale Original document locale
     */
    public void setOriginalDocumentLocale(Locale originalDocumentLocale)
    {
        this.originalDocumentLocale = originalDocumentLocale;
    }
}
