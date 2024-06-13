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

public class GlossaryUpdateEntry
{
    private Map<String, String> entry;

    private Locale sourceLanguage;

    private Locale targetLanguage;

    public GlossaryUpdateEntry(Map<String, String> entry, Locale sourceLanguage, Locale targetLanguage)
    {
        this.entry = entry;
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
    }

    public Map<String, String> getEntry()
    {
        return entry;
    }

    public Locale getSourceLanguage()
    {
        return sourceLanguage;
    }

    public Locale getTargetLanguage()
    {
        return targetLanguage;
    }
}
