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

/**
 * Struct used to describe a glossary for a translator.
 *
 * @version $Id$
 */
public class GlossaryInfo
{
    private final String glossaryId;

    private final String name;

    private final boolean ready;

    private final Locale sourceLocale;

    private final Locale targetLocale;

    private final long entryCount;

    /**
     * Constructs a GlossaryInfo.
     *
     * @param glossaryId Glossary identifier
     * @param name Glossary name
     * @param ready Indicates whether the glossary is ready to be used
     * @param sourceLocale Source locale
     * @param targetLocale Target locale
     * @param entryCount Number of glossary entries
     */
    public GlossaryInfo(String glossaryId, String name, boolean ready, Locale sourceLocale, Locale targetLocale,
        long entryCount)
    {
        this.glossaryId = glossaryId;
        this.name = name;
        this.ready = ready;
        this.sourceLocale = sourceLocale;
        this.targetLocale = targetLocale;
        this.entryCount = entryCount;
    }

    /**
     * @return the internal ID of the glossary.
     */
    public String getGlossaryId()
    {
        return glossaryId;
    }

    /**
     * @return the name of the glossary.
     */
    public String getName()
    {
        return name;
    }

    /**
     * @return the state of the translator glossary. Return 'true' if the glossary is ready to use into the translator.
     *     else it returns false.
     */
    public boolean isReady()
    {
        return ready;
    }

    /**
     * @return the source lang of the glossary.
     */
    public Locale getSourceLocale()
    {
        return sourceLocale;
    }

    /**
     * @return the target lang of the glossary.
     */
    public Locale getTargetLocale()
    {
        return targetLocale;
    }

    /**
     * @return the number of item into this glossary.
     */
    public long getEntryCount()
    {
        return entryCount;
    }
}
