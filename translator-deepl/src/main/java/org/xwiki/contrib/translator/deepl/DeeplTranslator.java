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
import org.xwiki.contrib.translator.Usage;
import org.xwiki.contrib.translator.internal.AbstractTranslator;
import org.xwiki.contrib.translator.internal.DefaultUsage;
import org.xwiki.contrib.translator.model.Glossary;
import org.xwiki.contrib.translator.model.GlossaryInfo;
import org.xwiki.contrib.translator.model.LocalePair;
import org.xwiki.localization.LocaleUtils;
import org.xwiki.text.StringUtils;

import com.deepl.api.DeepLException;
import com.deepl.api.GlossaryEntries;
import com.deepl.api.TextTranslationOptions;
import com.deepl.api.Translator;

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
        throws TranslatorException
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
            /**
             * Deepl bug with HTML comments
             * Example:
             * <div>
             *   reveals his deception and he is freed. He then eats an enormous amount of food at a
             *   <!--startwikilink:true|-|url|-|https://en.wikipedia.org/wiki/Cafeteria-->
             *   <span class="wikiexternallink">
             * <a href="https://en.wikipedia.org/wiki/Cafeteria">cafeteria</a></span><!--stopwikilink-->
             *   without paying to get arrested, and once again encounters Ellen in a
             * </div>
             *
             * outputs:
             * <div>
             *   révèle sa supercherie et il est libéré. Il mange ensuite une énorme quantité de nourriture dans une<span
             *         class="wikiexternallink"><a href="https://en.wikipedia.org/wiki/Cafeteria">cafétéria</a></span>
             *   <!--startwikilink:true|-|url|-|https://en.wikipedia.org/wiki/Cafeteria--><!--stopwikilink--> sans payer pour se faire
             *   arrêter, et rencontre à nouveau Ellen dans une salle d'attente.
             * </div>
             */
            content = content.replaceAll("<!--", "<script class=\"notranslate\"><!--");
            content = content.replaceAll("-->", "--></script>");
        }
        Optional<com.deepl.api.GlossaryInfo> glossaryId = getGlossaryForLocales(from, to);
        if (glossaryId.isPresent() && glossaryId.get().isReady()) {
            options.setGlossaryId(glossaryId.get().getGlossaryId());
        }
        String result = null;
        try {
            result = translator.translateText(content,
                normalizeLocale(from, NormalisationType.SOURCE_LANG),
                normalizeLocale(to, NormalisationType.TARGET_LANG),
                options).getText();
            result = result.replaceAll("<script class=\"notranslate\">", "");
            result = result.replaceAll("</script>", "");
        } catch (InterruptedException e) {
            String abbr = StringUtils.abbreviate(content, 100);
            logger.debug("Error when translating [{}]", abbr);
            throw new TranslatorException(String.format("Interrupt exception when translating [%s]", abbr), e);
        } catch (DeepLException e) {
            String abbr = StringUtils.abbreviate(content, 100);
            logger.debug("Error when translating [{}]", abbr);
            throw new TranslatorException(String.format("DeepL exception when translating [%s]", abbr), e);
        }
        return result;
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
                case SOURCE_LANG_GLOSSARY:
                case TARGET_LANG_GLOSSARY:
                    return locale.getLanguage();
                case TARGET_LANG:
                    String name = locale.toString();
                    if (name.equals("en") || name.equals("en_GB")) {
                        return "en-GB";
                    } else if (name.equals("en_US")) {
                        return "en-US";
                    } else if (name.equals("pt") || name.equals("pt_BR")) {
                        return "pt-BR";
                    } else if (name.equals("pt_PT")) {
                        return "pt-PT";
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

    public Translator getTranslator() throws TranslatorException
    {
        String apiKey = translatorConfiguration.getApiKey();
        try {
            if (StringUtils.isNotEmpty(apiKey)) {
                return new Translator(apiKey);
            } else {
                throw new TranslatorException("Invalid API key");
            }
        } catch (Exception e) {
            throw new TranslatorException("Invalid API key");
        }
    }

    public Usage getUsage() throws TranslatorException
    {
        //TODO: check programming rights
        Translator translator = getTranslator();
        try {
            if (translator != null && translator.getUsage() != null && translator.getUsage().getCharacter() != null) {
                com.deepl.api.Usage.Detail character = translator.getUsage().getCharacter();
                return new DefaultUsage(character.getCount(), character.getLimit());
            }
        } catch (DeepLException | InterruptedException e) {
            logger.error("Error while retrieving translator usage", e);
            throw new TranslatorException("Error while retrieving translator usage", e);
        }
        return null;
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
                logger.info("Updating glossary: [{}]",   glossaryName);

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
                        NormalisationType.SOURCE_LANG_GLOSSARY),
                    normalizeLocale(entry.getGlossaryInfo().getTargetLocale(),
                        NormalisationType.TARGET_LANG_GLOSSARY),
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
