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

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Locale;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.query.QueryException;

import com.xpn.xwiki.XWikiException;

// TODO: AdminGroup view TranslatorConfiguration but this is local group -> check
// TODO: not hasEdit but hasEdit on target space
// OK: Check that current user has edit right in the target space
// TODO: Check what happens when language is not set (empty)
// TODO: check if creating a Translator is time consuming (-> do not recreate) or not
// TODO: maybe use same mechanism as shareinline to load the translate modal when the button is hit
// TODO: later manage country specific : override keys in FR_CH from FR to have specific translations only
// TODO: check what happens if the original page itself is translated natively (same name)
// TODO: Introduce TranslatorException
// TODO: review TranslationSet
// TODO: compute full reference

@Role
public interface Translator
{
    void translate(EntityReference reference, Locale locale) throws Exception;

    DocumentReference computeTranslationReference(DocumentReference reference, String title, Locale locale)
        throws XWikiException;

    void translate(EntityReference reference, Locale[] toLocales, boolean html) throws Exception;

    String translate(String content, Locale from, Locale to, boolean html) throws Exception;

    OutputStream translate(InputStreamReader reader, Locale from, Locale to, boolean html);

    boolean isTranslatable(EntityReference reference);

    TranslationSet getTranslations(EntityReference reference) throws XWikiException, QueryException;

    String normalizeLocale(Locale locale);

    String getName();

    boolean isSameNameTranslationNamingStrategy(EntityReference reference) throws XWikiException;
}
