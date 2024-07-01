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
package org.xwiki.contrib.machinetranslation;

/** Exception which can occur during translation operations.
 *
 * @version $Id$
 */
public class MachineTranslationException extends Exception
{
    /**
     * Serialization identifier.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new TranslatorException.
     * @param source Source exception
     */
    public MachineTranslationException(Throwable source)
    {
        super(source);
    }

    /**
     * Constructs a new TranslatorException with given message.
     * @param message Exception message
     */
    public MachineTranslationException(String message)
    {
        super(message);
    }

    /**
     * Constructs a new TranslatorException with message and source.
     * @param message Exception message
     * @param source Source exception
     */
    public MachineTranslationException(String message, Throwable source)
    {
        super(message, source);
    }
}
