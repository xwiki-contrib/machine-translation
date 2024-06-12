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

import org.xwiki.model.reference.DocumentReference;

/**
 * Inner class used to represent all translations of a given reference. The class contains: a document reference, the
 * title of that reference in its default locale, its default locale, a map of locales and title / references.
 */
public class TranslationSet
{
    protected DocumentReference originalDocumentReference;

    protected String originalDocumentTitle;

    protected Locale originalDocumentLocale;

    protected Map<Locale, List<Object>> translations;

    public TranslationSet(DocumentReference originalDocumentReference, String originalDocumentTitle,
        Locale originalDocumentLocale, Map<Locale, List<Object>> translations)
    {
        this.originalDocumentReference = originalDocumentReference;
        this.originalDocumentTitle = originalDocumentTitle;
        this.originalDocumentLocale = originalDocumentLocale;
        this.translations = translations;
    }

    public DocumentReference getOriginalDocumentReference()
    {
        return originalDocumentReference;
    }

    public String getOriginalDocumentTitle()
    {
        return originalDocumentTitle;
    }

    public Locale getOriginalDocumentLocale()
    {
        return originalDocumentLocale;
    }

    public Map<Locale, List<Object>> getTranslations()
    {
        return translations;
    }
}
