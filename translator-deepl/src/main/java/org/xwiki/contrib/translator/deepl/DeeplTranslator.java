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
package org.xwiki.contrib.translator.deepl;

import java.util.Locale;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.translator.TranslatorException;
import org.xwiki.contrib.translator.internal.AbstractTranslator;
import org.xwiki.text.StringUtils;

import com.deepl.api.DeepLException;
import com.deepl.api.TextResult;
import com.deepl.api.TextTranslationOptions;
import com.deepl.api.Translator;
import com.deepl.api.Usage;

@Component
@Named(DeeplTranslator.HINT)
@Singleton
public class DeeplTranslator extends AbstractTranslator
{
    static final String HINT = "deepl";

    static final String NAME = "DeepL";

    @Override
    public String translate(String content, Locale from, Locale to, boolean html) throws TranslatorException
    {
        if (StringUtils.isEmpty(content)) {
            return content;
        }
        Translator translator = getTranslator();
        logger.debug("Translator: [{}]", translator);
        TextTranslationOptions options = new TextTranslationOptions();
        if (html) {
            options.setTagHandling("html");
        }
        TextResult result = null;
        try {
            result = translator.translateText(content, from.toString(), normalizeLocale(to),
                options);
        } catch (InterruptedException e) {
            String abbr = StringUtils.abbreviate(content, 100);
            logger.debug("Error when translating [{}]", abbr);
            throw new TranslatorException(String.format("Interrupt exception when translating [%s]", abbr), e);
        } catch (DeepLException e) {
            String abbr = StringUtils.abbreviate(content, 100);
            logger.debug("Error when translating [{}]", abbr);
            throw new TranslatorException(String.format("DeepL exception when translating [%s]", abbr), e);
        }
        return result.getText();
    }

    /**
     * See https://developers.deepl.com/docs/resources/supported-languages
     *
     * @param locale
     * @return normalized locale
     */
    public String normalizeLocale(Locale locale)
    {
        if (locale != null) {
            String name = locale.toString();
            if (name.equals("en")) {
                return "en-GB";
            } else if (name.equals("pt")) {
                return "pt-BR";
            } else {
                return locale.toString();
            }
        } else {
            logger.error("Missing locale");
            return null;
        }
    }

    public Translator getTranslator()
    {
        String apiKey = translatorConfiguration.getApiKey();
        return new Translator(apiKey);
    }

    public Usage getUsage() throws DeepLException, InterruptedException
    {
        //TODO: check programming rights
        Translator translator = getTranslator();
        return translator.getUsage();
    }

    public String getName()
    {
        return NAME;
    }
}
