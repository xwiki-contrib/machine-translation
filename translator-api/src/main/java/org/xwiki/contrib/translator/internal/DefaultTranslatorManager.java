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
package org.xwiki.contrib.translator.internal;

import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.translator.Translator;
import org.xwiki.contrib.translator.TranslatorConfiguration;
import org.xwiki.contrib.translator.TranslatorManager;

/**
 * Default manager for translators.
 *
 * @version $Id$
 */
@Component
@Singleton
public class DefaultTranslatorManager implements TranslatorManager
{
    /**
     * Translation configuration.
     */
    @Inject
    protected TranslatorConfiguration translatorConfiguration;

    /**
     * Logging helper.
     */
    @Inject
    protected Logger logger;

    /**
     * Component manager.
     */
    @Inject
    private ComponentManager componentManager;

    @Override
    public Translator getTranslator()
    {
        return getTranslator(this.translatorConfiguration.getTranslator());
    }

    @Override
    public Translator getTranslator(String hint)
    {
        try {
            return this.componentManager.getInstance(Translator.class, hint);
        } catch (ComponentLookupException e) {
            this.logger.error("Error while getting the Translator with hint [{}]", hint, e);
            return null;
        }
    }

    @Override
    public Set<String> getAvailableTranslators()
    {
        try {
            return this.componentManager.getInstanceMap(Translator.class).keySet();
        } catch (ComponentLookupException e) {
            this.logger.error("Error while getting the instance map of the Translators", e);
            return Collections.emptySet();
        }
    }
}
