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
package org.xwiki.contrib.translator.model;

import java.util.Locale;

import org.xwiki.localization.LocaleUtils;

/**
 * Struct used to store a pair of Locale. Mostly used for the translator glossary.
 *
 * @version $Id$
 */
public class LocalePair
{
    private final Locale sourceLocale;

    private final Locale targetLocale;

    /**
     * Constructs a LocalePair.
     *
     * @param sourceLocale Source locale
     * @param destinationLocale Target locale
     */
    public LocalePair(String sourceLocale, String destinationLocale)
    {
        this(LocaleUtils.toLocale(sourceLocale), LocaleUtils.toLocale(destinationLocale));
    }

    /**
     * Constructs a LocalePair.
     *
     * @param sourceLocale Source locale
     * @param targetLocale Target locale
     */
    public LocalePair(Locale sourceLocale, Locale targetLocale)
    {
        this.sourceLocale = sourceLocale;
        this.targetLocale = targetLocale;
    }

    /**
     * @return the source locale
     */
    public Locale getSourceLocale()
    {
        return sourceLocale;
    }

    /**
     * @return the target locale
     */
    public Locale getTargetLocale()
    {
        return targetLocale;
    }
}
