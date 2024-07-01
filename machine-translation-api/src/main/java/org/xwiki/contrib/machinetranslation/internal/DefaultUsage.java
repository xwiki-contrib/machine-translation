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
package org.xwiki.contrib.machinetranslation.internal;

import org.xwiki.contrib.machinetranslation.Usage;

/**
 * Default Usage implementation.
 *
 * @version $Id$
 */
public class DefaultUsage implements Usage
{
    private long count;

    private long limit;

    /**
     * Default constructor.
     * @param count Number of characters translated in current period
     * @param limit Maximum number of characters that can be translated in current period
     */
    public DefaultUsage(long count, long limit)
    {
        this.count = count;
        this.limit = limit;
    }

    @Override
    public long getCount()
    {
        return this.count;
    }

    @Override
    public long getLimit()
    {
        return this.limit;
    }
}
