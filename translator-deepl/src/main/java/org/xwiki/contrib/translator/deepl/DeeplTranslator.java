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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.translator.TranslatorException;
import org.xwiki.contrib.translator.internal.AbstractTranslator;
import org.xwiki.contrib.translator.model.Glossary;
import org.xwiki.contrib.translator.model.GlossaryInfo;
import org.xwiki.contrib.translator.model.LocalePair;
import org.xwiki.localization.LocaleUtils;
import org.xwiki.text.StringUtils;

import com.deepl.api.DeepLException;
import com.deepl.api.GlossaryEntries;
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

    private static List<String> getGlossariesByName(List<com.deepl.api.GlossaryInfo> deeplGlossaries,
        String glossaryName)
    {
        List<String> glossaryIds = new ArrayList<>();
        for (com.deepl.api.GlossaryInfo glossary : deeplGlossaries) {
            if (glossary.getName().equals(glossaryName)) {
                glossaryIds.add(glossary.getGlossaryId());
            }
        }
        return glossaryIds;
    }

    private Optional<com.deepl.api.GlossaryInfo> getGlossaryForLocales(Locale source, Locale destination)
    {
        Translator translator = getTranslator();
        try {
            String glossaryName = getGlossaryName(source, destination);
            return translator.listGlossaries().stream()
                .filter(entry -> entry.getName().equals(glossaryName))
                .findFirst();
        } catch (Exception e) {
            logger.error("Got unexpected error while synchronizing glossaries : [{}]", e.getMessage(), e);
            return Optional.empty();
        }
    }

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
        Optional<com.deepl.api.GlossaryInfo> glossaryId = getGlossaryForLocales(from, to);
        if (glossaryId.isPresent() && glossaryId.get().isReady()) {
            options.setGlossaryId(glossaryId.get().getGlossaryId());
        }
        TextResult result = null;
        try {
            result = translator.translateText(content,
                normalizeLocale(from, NormalisationType.SOURCE_LANG),
                normalizeLocale(to, NormalisationType.TARGET_LANG),
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
    public String normalizeLocale(Locale locale, NormalisationType type) throws TranslatorException
    {
        if (locale != null) {

            switch (type) {
                case SOURCE_LANG:
                case GLOSSARY:
                    return locale.getLanguage();
                case TARGET_LANG:
                    String name = locale.toString();
                    if (name.equals("en")) {
                        return "en-GB";
                    } else if (name.equals("pt")) {
                        return "pt-BR";
                    } else {
                        return locale.getLanguage();
                    }
                default:
                    logger.error("Undefined NormalisationType: [{}]", type);
                    throw new TranslatorException("Undefined NormalisationType: " + type);
            }
        } else {
            logger.error("Missing locale");
            throw new TranslatorException("locale is null");
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

    /*
     * Glossary part
     */

    @Override
    public List<LocalePair> getGlossaryLocalePairs() throws TranslatorException
    {
        Translator translator = getTranslator();
        try {
            return translator.getGlossaryLanguages().stream()
                .map(item -> new LocalePair(item.getSourceLanguage(), item.getTargetLanguage()))
                .collect(Collectors.toList());
        } catch (InterruptedException e) {
            logger.debug("Error when getting glossary languages [{}]", e.getMessage(), e);
            throw new TranslatorException("Interrupt exception getting glossary languages", e);
        } catch (DeepLException e) {
            logger.debug("Error when getting glossary languages [{}]", e.getMessage(), e);
            throw new TranslatorException("DeepL exception when getting glossary languages", e);
        }
    }

    @Override
    public List<GlossaryInfo> getGlossaries() throws TranslatorException
    {
        Translator translator = getTranslator();
        try {
            String glossaryNamePrefix = getGlossaryNamePrefix();
            return translator.listGlossaries()
                .stream()
                .filter(entry -> entry.getName().startsWith(glossaryNamePrefix))
                .map(item -> new GlossaryInfo(item.getGlossaryId(), item.getName(), item.isReady(),
                    LocaleUtils.toLocale(item.getSourceLang()),
                    LocaleUtils.toLocale(item.getTargetLang()),
                    item.getEntryCount()))
                .collect(Collectors.toList());
        } catch (InterruptedException e) {
            logger.debug("Error when getting glossaries [{}]", e.getMessage(), e);
            throw new TranslatorException("Interrupt exception when getting glossaries", e);
        } catch (DeepLException e) {
            logger.debug("Error when getting glossaries [{}]", e.getMessage(), e);
            throw new TranslatorException("DeepL exception when getting glossaries", e);
        }
    }

    @Override
    public Map<String, String> getGlossaryEntries(String id) throws TranslatorException
    {
        Translator translator = getTranslator();
        try {
            return translator.getGlossaryEntries(id);
        } catch (InterruptedException e) {
            logger.debug("Error when getting glossaries details [{}]", e.getMessage(), e);
            throw new TranslatorException("Interrupt exception when getting glossaries details", e);
        } catch (DeepLException e) {
            logger.debug("Error when getting glossaries details [{}]", e.getMessage(), e);
            throw new TranslatorException("DeepL exception when getting glossaries details", e);
        }
    }

    @Override
    public void updateGlossaries(List<Glossary> entries) throws TranslatorException
    {
        Translator translator = getTranslator();
        try {
            List<com.deepl.api.GlossaryInfo> deeplGlossaries = translator.listGlossaries();
            String glossaryNamePrefix = getGlossaryNamePrefix();

            for (Glossary entry : entries) {
                String glossaryName = getGlossaryName(entry.getGlossaryInfo().getSourceLocale(),
                    entry.getGlossaryInfo().getTargetLocale(),
                    glossaryNamePrefix);

                // Check if the glossary exists. If it's the case, we need to delete it to re-create it
                for (String glossaryId : getGlossariesByName(deeplGlossaries, glossaryName)) {
                    logger.debug("Deleting glossary [{}] with ID [{}]", glossaryName, glossaryId);
                    translator.deleteGlossary(glossaryId);
                }

                // Create (or re-create the glossary)
                logger.debug("Creating glossary [{}]", glossaryName);
                translator.createGlossary(
                    glossaryName,
                    normalizeLocale(entry.getGlossaryInfo().getSourceLocale(),
                        org.xwiki.contrib.translator.Translator.NormalisationType.GLOSSARY),
                    normalizeLocale(entry.getGlossaryInfo().getTargetLocale(),
                        org.xwiki.contrib.translator.Translator.NormalisationType.GLOSSARY),
                    new GlossaryEntries(entry.getEntries()));
            }
        } catch (InterruptedException e) {
            logger.debug("Error when synchronizing glossaries [{}]", e.getMessage(), e);
            throw new TranslatorException("Interrupt exception when synchronizing glossaries", e);
        } catch (DeepLException e) {
            logger.debug("Error when synchronizing glossaries [{}]", e.getMessage(), e);
            throw new TranslatorException("DeepL exception when synchronizing glossaries", e);
        }
    }

    public String getName()
    {
        return NAME;
    }
}
