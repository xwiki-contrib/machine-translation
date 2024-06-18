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
import java.util.Map;

/**
 * Structure representing a Glossary.
 *
 * @version $Id$
 */
public class Glossary
{
    private Map<String, String> entries;

    private GlossaryInfo glossaryInfo;

    private Locale sourceLocale;

    private Locale targetLocale;

    /**
     * Constructs a Glossary with its entries and GlossaryInfo.
     *
     * @param entries Glossary entries
     * @param glossaryInfo Glossary metadata
     */
    public Glossary(Map<String, String> entries, GlossaryInfo glossaryInfo)
    {
        this.entries = entries;
        this.glossaryInfo = glossaryInfo;
    }

    /**
     * @return all entry of this glossary.
     */
    public Map<String, String> getEntries()
    {
        return entries;
    }

    /**
     * @return the glossary info of this glossary.
     */
    public GlossaryInfo getGlossaryInfo()
    {
        return glossaryInfo;
    }
}
