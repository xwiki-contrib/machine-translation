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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.xwiki.contrib.machinetranslation.MachineTranslationConfiguration;
import org.xwiki.contrib.machinetranslation.MachineTranslationException;
import org.xwiki.contrib.machinetranslation.TranslationSet;
import org.xwiki.contrib.machinetranslation.Translator;
import org.xwiki.contrib.machinetranslation.TranslatorManager;
import org.xwiki.contrib.machinetranslation.model.LocalePair;
import org.xwiki.localization.LocaleUtils;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.model.validation.EntityNameValidationManager;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.ContentParser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.text.StringUtils;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wysiwyg.cleaner.HTMLCleaner;
import org.xwiki.wysiwyg.converter.HTMLConverter;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Translator superclass.
 *
 * @version $Id$
 */
public abstract class AbstractTranslator implements Translator
{
    /**
     * Translation class reference.
     */
    static final LocalDocumentReference TRANSLATION_CLASS_REFERENCE =
        new LocalDocumentReference(Arrays.asList("XWiki", "MachineTranslation"), "MachineTranslationClass");

    static final String ORIGINAL_PAGE_PROPERTY = "originalPage";

    static final String CONTENT_REFERENCE = "XWiki.Document^content";

    /**
     * Separator used to separate list items in strings.
     */
    static final String LIST_ITEM_SEPARATOR = ",";

    /**
     * Logging helper.
     */
    @Inject
    protected Logger logger;

    /**
     * Translator configuration.
     */
    @Inject
    protected MachineTranslationConfiguration translatorConfiguration;

    /**
     * Context provider.
     */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * Query manager.
     */
    @Inject
    private QueryManager queryManager;

    /**
     * Dcument resolver.
     */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> referenceResolver;

    /**
     * Reference serializer.
     */
    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    /**
     * UserReference resolver.
     */
    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    /**
     * Authorization manager.
     */
    @Inject
    private ContextualAuthorizationManager authorizationManager;

    /**
     * HTML Converter.
     */
    @Inject
    private HTMLConverter htmlConverter;

    /**
     * Model script service.
     */
    @Inject
    @Named("model")
    private ScriptService modelScriptService;

    /**
     * Entity name validation manager.
     */
    @Inject
    private EntityNameValidationManager entityNameValidationManager;

    /**
     * Entity reference provider.
     */
    @Inject
    private EntityReferenceProvider defaultEntityReferenceProvider;

    /**
     * Translator manager.
     */
    @Inject
    private TranslatorManager translatorManager;

    /**
     * Context provider.
     */
    @Inject
    private Provider<XWikiContext> xwikiContextProvider;

    @Inject
    private HTMLCleaner htmlCleaner;

    @Inject
    private ContentParser parser;

    @Inject
    @Named("annotatedxhtml/1.0")
    private BlockRenderer wikiBlockRenderer;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Override
    public void translate(EntityReference reference, Locale toLocale) throws MachineTranslationException
    {
        try {
            if (!authorizationManager.hasAccess(Right.VIEW, reference)) {
                throw new MachineTranslationException(String.format("Denied view access on [%s]", reference));
            }

            XWikiContext xcontext = xcontextProvider.get();
            XWiki xwiki = xcontext.getWiki();
            EntityReference originalDocumentReference = getOriginalDocumentReference(reference);
            XWikiDocument doc = xwiki.getDocument(originalDocumentReference, xcontext).clone();

            Locale fromLocale = doc.getDefaultLocale();
            logger.info("Translating [{}] [{}] to locale [{}]", doc.getDocumentReference(), fromLocale, toLocale);

            String translationTitle = translate(doc.getTitle(), fromLocale, toLocale, false);
            // TODO: use reference resolver
            EntityReference translationReference =
                computeTranslationReference(doc.getDocumentReference(), translationTitle, toLocale);

            if (!this.authorizationManager.hasAccess(Right.EDIT, translationReference)) {
                throw new MachineTranslationException(String.format("Denied edit right on [%s]", translationReference));
            }

            XWikiDocument translationPage = getTranslationPage(doc, translationReference, toLocale);
            translationPage.setTitle(translationTitle);
            translate(doc, translationPage, fromLocale, toLocale);

            if (!isSameNameTranslationNamingStrategy(reference)) {
                BaseObject translationObj = translationPage.getXObject(TRANSLATION_CLASS_REFERENCE);
                if (translationObj == null) {
                    translationObj = translationPage.newXObject(TRANSLATION_CLASS_REFERENCE, xcontext);
                }
                translationObj.setStringValue(ORIGINAL_PAGE_PROPERTY,
                    entityReferenceSerializer.serialize(doc.getDocumentReference()));
                translationObj.setDateValue("automatedTranslationDate", new Date());
                // TODO: fill in translator appropriately
                // translationObj.setStringValue("translator", "XWiki.MachineTranslation.DeepL");
            }

            setAuthors(translationPage);
            xwiki.saveDocument(translationPage, "Translation from " + fromLocale.getLanguage(), xcontext);
        } catch (XWikiException e) {
            logger.error("Translator error {[]}", e);
            throw new MachineTranslationException(e);
        }
    }

    private XWikiDocument getTranslationPage(XWikiDocument doc, EntityReference translationReference,
        Locale toLocale) throws XWikiException, MachineTranslationException
    {
        XWikiDocument translationPage = null;
        XWikiContext xcontext = xcontextProvider.get();
        XWiki xwiki = xcontext.getWiki();
        logger.info("Translation page: {}", translationReference);
        if (!isSameNameTranslationNamingStrategy(doc.getDocumentReference())) {
            translationPage = xwiki.getDocument(translationReference, xcontext).clone();
            translationPage.setDefaultLocale(toLocale);
            // TODO make the copy of attachments and objects configurable
            logger.info("Copying attachments...");
            translationPage.copyAttachments(doc);

            logger.info("Copying objects...");
            // NB: in case objects are not copied, we need to make sure in the code below that
            // the target object exist in the translated page before they get translated
            translationPage.duplicateXObjects(doc);
        } else {
            translationPage =
                xwiki.getDocument(new DocumentReference(translationReference, toLocale), xcontext).clone();
        }
        return translationPage;
    }

    private void translate(XWikiDocument original, XWikiDocument translation, Locale from, Locale to)
        throws MachineTranslationException
    {
        String content = original.getContent();
        try {
            for (EntityReference property : getTargetProperties()) {
                String propertyString = getModelScriptService().serialize(property);
                if (propertyString.equals(CONTENT_REFERENCE)) {
                    XDOM xdom = parser.parse(content, Syntax.XWIKI_2_1);
                    WikiPrinter printer = new DefaultWikiPrinter();
                    wikiBlockRenderer.render(xdom, printer);

                    Pattern pattern = Pattern.compile(
                        "<!--startmacro:glossaryReference\\|-\\|glossaryId=\".+?\" "
                            + "entryId=\".+?\"\\|-\\|(.+?)--><!--stopmacro-->");
                    StringBuffer builder = new StringBuffer();
                    Matcher matcher = pattern.matcher(printer.toString());
                    while (matcher.find()) {
                        matcher.appendReplacement(builder, matcher.group(1));
                    }
                    matcher.appendTail(builder);
                    String plainHtml = builder.toString();
                    String translatedContent = translate(plainHtml, from, to, true);
                    // TODO: We can convert directly without using fromAnnotatedHTML
                    String wikiSyntax = fromAnnotatedHTML(translatedContent, Syntax.XWIKI_2_1);
                    translation.setContent(wikiSyntax);
                } else if (!isSameNameTranslationNamingStrategy(original.getDocumentReference())) {
                    List<BaseObject> objects = original.getXObjects(property.getParent());
                    for (BaseObject obj : objects) {
                        logger.debug("Translating object property [{}] [{}]...", propertyString, obj.getNumber());
                        String value = obj.getLargeStringValue(property.getName());
                        if (StringUtils.isNotEmpty(value)) {
                            String html = toHTML(value, Syntax.XWIKI_2_1);
                            String translatedContent = translate(html, from, to, true);
                            translatedContent = fromAnnotatedHTML(translatedContent, Syntax.XWIKI_2_1);
                            BaseObject object =
                                translation.getXObject(property.getParent(), obj.getNumber());
                            object.setLargeStringValue(property.getName(), translatedContent);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("translate", e);
            throw new MachineTranslationException(e);
        }
    }

    @Override
    public DocumentReference computeTranslationReference(EntityReference reference, String translationTitle,
        Locale translationLocale) throws MachineTranslationException
    {
        try {
            EntityReference originalDocumentReference = getOriginalDocumentReference(reference);
            String translationPageName = translationTitle;
            if (StringUtils.isNotEmpty(translationTitle)) {
                translationPageName =
                    entityNameValidationManager.getEntityReferenceNameStrategy().transform(translationTitle);
            }
            if (!isSameNameTranslationNamingStrategy(originalDocumentReference)) {
                SpaceReference localeSpaceReference =
                    new SpaceReference(translationLocale.toString(), getCurrentWikiReference());
                LocalDocumentReference localOriginalDocumentReference =
                    new LocalDocumentReference(new DocumentReference(originalDocumentReference));

                EntityReference translationPageReference = new EntityReference(localOriginalDocumentReference);

                // In case the original document top level parent is the document language code,
                // remove this space when computing the translation reference. For example: "/en/my-page/" will
                // become "/fr/ma-page/", not "/fr/en/my-page/".
                EntityReference topLevelSpace = localOriginalDocumentReference.extractFirstReference(EntityType.SPACE);
                String topLevelSpaceName = entityReferenceSerializer.serialize(topLevelSpace);
                XWikiContext xcontext = xcontextProvider.get();
                XWiki xwiki = xcontext.getWiki();
                XWikiDocument originalDocument = xwiki.getDocument(originalDocumentReference, xcontext);
                Locale originalDocumentLocale = originalDocument.getDefaultLocale();
                if (originalDocumentLocale.getLanguage().equals(topLevelSpaceName)) {
                    translationPageReference = translationPageReference.removeParent(topLevelSpace);
                }

                // Append language code as root space of the translation
                translationPageReference = translationPageReference.appendParent(localeSpaceReference);

                if (StringUtils.isEmpty(translationPageName)) {
                    return new DocumentReference(translationPageReference).setWikiReference(getCurrentWikiReference());
                }

                String defaultDocumentName =
                    this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT).getName();

                if (!translationPageReference.getName().equals(defaultDocumentName)) {
                    return new DocumentReference(translationPageName, translationPageReference.getParent(), null);
                } else {
                    // TODO: make it work for terminal pages
                    translationPageReference =
                        translationPageReference.replaceParent(translationPageReference.getParent(),
                            new SpaceReference(translationPageName, translationPageReference.getParent().getParent()));
                    return new DocumentReference(translationPageReference).setWikiReference(getCurrentWikiReference());
                }
            } else {
                return new DocumentReference(originalDocumentReference).setWikiReference(getCurrentWikiReference());
            }
        } catch (XWikiException e) {
            logger.error("Error while computing a translation reference for [{}]", reference);
            throw new MachineTranslationException("Error when computing a translation reference", e);
        }
    }

    @Override
    public void translate(EntityReference reference, Locale[] toLocales) throws MachineTranslationException
    {
        for (Locale toLocale : toLocales) {
            translate(reference, toLocale);
        }
    }

    @Override
    public TranslationSet getTranslations(EntityReference reference) throws MachineTranslationException
    {
        XWikiContext xcontext = xcontextProvider.get();
        XWiki xwiki = xcontext.getWiki();
        try {
            XWikiDocument doc = xwiki.getDocument(reference, xcontext);

            if (isSameNameTranslationNamingStrategy(reference)) {
                List<Locale> locales = doc.getTranslationLocales(xcontext);
                Map<Locale, List<Object>> map = new HashMap<>();

                map.put(doc.getDefaultLocale(),
                    Arrays.asList(doc.getDocumentReference(), doc.getTitle(), doc.getDefaultLocale()));
                for (Locale locale : locales) {
                    if (xwiki.getAvailableLocales(xcontext).contains(locale)) {
                        XWikiDocument translatedDocument = doc.getTranslatedDocument(locale, xcontext);
                        map.put(locale,
                            Arrays.asList(translatedDocument.getDocumentReference(), translatedDocument.getTitle(),
                                locale));
                    }
                }
                return new TranslationSet(doc.getDocumentReference(), doc.getTitle(),
                    doc.getDefaultLocale(), map);
            }
            return retrieveTranslations(doc.getDocumentReference());
        } catch (XWikiException e) {
            throw new MachineTranslationException(String.format("Failed to get translations for [%s]", reference), e);
        }
    }

    private TranslationSet retrieveTranslations(DocumentReference reference) throws MachineTranslationException
    {
        XWikiContext xcontext = xcontextProvider.get();
        XWiki xwiki = xcontext.getWiki();

        List<String[]> entries = new ArrayList<>();
        TranslationSet translationSet = new TranslationSet();

        EntityReference originalDocumentReference = getOriginalDocumentReference(reference);
        translationSet.setOriginalDocumentReference(originalDocumentReference);

        try {
            if (!reference.equals(originalDocumentReference)) {
                String originalPageName = entityReferenceSerializer.serialize(originalDocumentReference);
                String hql = "select doc.fullName, doc.title, doc.language from XWikiDocument as doc, "
                    + "BaseObject as obj where doc.fullName = :originalPage and obj.name = doc.fullName"
                    + " and obj.className = :class order by doc.language";
                Query query = queryManager.createQuery(hql, Query.HQL)
                    .bindValue("class", entityReferenceSerializer.serialize(TRANSLATION_CLASS_REFERENCE))
                    .bindValue(ORIGINAL_PAGE_PROPERTY, originalPageName);
                entries = query.execute();
                if (authorizationManager.hasAccess(Right.VIEW, referenceResolver.resolve(originalPageName))) {
                    XWikiDocument originalPage =
                        xwiki.getDocument(referenceResolver.resolve(originalPageName), xcontext);
                    entries.add(new String[] { originalPageName, originalPage.getTitle(),
                        originalPage.getRealLocale().toString() });
                }
            }

            Query query = createQuery(reference, translationSet.getOriginalDocumentReference());
            entries.addAll(query.execute());
            List<Locale> availableLocales = xwiki.getAvailableLocales(xcontext);
            for (Object[] obj : entries) {
                Locale locale = LocaleUtils.toLocale(obj[2].toString());
                DocumentReference pageReference = referenceResolver.resolve(obj[0].toString());
                if (availableLocales.contains(locale) && authorizationManager.hasAccess(Right.VIEW,
                    pageReference))
                {
                    translationSet.getTranslations().put(locale, Arrays.asList(pageReference, obj[1], locale));
                }
            }
            XWikiDocument originalPage = xwiki.getDocument(translationSet.getOriginalDocumentReference(), xcontext);
            translationSet.setOriginalDocumentTitle(originalPage.getTitle());
            translationSet.setOriginalDocumentLocale(originalPage.getDefaultLocale());
            return translationSet;
        } catch (XWikiException | QueryException e) {
            logger.error("Error while retrieving translation pages of [{}]", reference, e);
            throw new MachineTranslationException("Error when retrieving translation pages", e);
        }
    }

    private EntityReference getOriginalDocumentReference(EntityReference reference) throws MachineTranslationException
    {
        try {
            /* Either the current page is already a translation or it is the original document */
            XWikiContext xcontext = xcontextProvider.get();
            XWiki xwiki = xcontext.getWiki();
            XWikiDocument doc = xwiki.getDocument(reference, xcontext);
            BaseObject translationObj = doc.getXObject(TRANSLATION_CLASS_REFERENCE);
            if (translationObj != null) {
                // 1) First case: the current page is a translation
                // -> retrieve original document and add it to the entry list
                String originalPageName = translationObj.getStringValue(ORIGINAL_PAGE_PROPERTY);
                return referenceResolver.resolve(originalPageName);
            } else {
                // 2) Second case: the current page is the original one
                return reference;
            }
        } catch (XWikiException e) {
            logger.error("Error while retrieving original document reference for [{}]", reference, e);
            throw new MachineTranslationException("Error while retrieving original document reference", e);
        }
    }

    /**
     * Create Query that will retrieve all translations of a given document, when they're located in different
     * locations.
     *
     * @param reference A given document reference
     * @param originalDocument Reference of the original document
     * @return Query to be used to retrieve all translation documents
     * @throws QueryException in case an error occurs
     */
    private Query createQuery(DocumentReference reference, EntityReference originalDocument)
        throws QueryException
    {
        String hql = "select doc.fullName, doc.title, doc.defaultLanguage from XWikiDocument as doc, "
            + "BaseObject as obj, StringProperty as prop where obj.name = doc.fullName and obj.className = "
            + ":className and prop.id.id = obj.id and prop.id.name = :prop "
            + "and prop.value = :originalPage";
        return queryManager.createQuery(hql, Query.HQL)
            .bindValue("className", entityReferenceSerializer.serialize(TRANSLATION_CLASS_REFERENCE))
            .bindValue("prop", ORIGINAL_PAGE_PROPERTY)
            .bindValue(ORIGINAL_PAGE_PROPERTY, entityReferenceSerializer.serialize(originalDocument));
    }

    @Override
    public boolean isTranslatable(EntityReference reference) throws MachineTranslationException
    {
        XWikiContext xcontext = xcontextProvider.get();
        XWiki xwiki = xcontext.getWiki();

        if (!xwiki.isMultiLingual(xcontext)) {
            return false;
        }

        try {
            XWikiDocument doc = xwiki.getDocument(reference, xcontext);
            if (doc.getRealLocale() == Locale.ROOT) {
                return false;
            }

            String targetClasses = translatorConfiguration.getTargetClasses();
            if (StringUtils.isEmpty(targetClasses)) {
                return true;
            }

            for (String xclass : toList(targetClasses)) {
                if (StringUtils.isNotEmpty(xclass)) {
                    DocumentReference classReference = referenceResolver.resolve(xclass);
                    List<BaseObject> objects = doc.getXObjects(classReference);
                    if (!objects.isEmpty()) {
                        return true;
                    }
                }
            }
        } catch (XWikiException e) {
            throw new MachineTranslationException(String.format("Failed to check if [%s] is translatable", reference),
                e);
        }
        return false;
    }

    @Override
    public boolean canTranslate(EntityReference reference) throws MachineTranslationException
    {
        if (!isTranslatable(reference)) {
            return false;
        }

        XWikiContext xcontext = xcontextProvider.get();
        XWiki xwiki = xcontext.getWiki();

        for (Locale locale : xwiki.getAvailableLocales(xcontext)) {
            if (canTranslate(reference, locale)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canTranslate(EntityReference reference, Locale toLocale) throws MachineTranslationException
    {
        if (!isTranslatable(reference)) {
            return false;
        }

        EntityReference translationReference = computeTranslationReference(reference, null, toLocale);
        return authorizationManager.hasAccess(Right.EDIT, translationReference);
    }

    @Override
    public boolean isSameNameTranslationNamingStrategy(EntityReference reference) throws MachineTranslationException
    {
        if (translatorConfiguration.isSameNameTranslationNamingStrategy()) {
            return true;
        } else {
            /* We use same name translation naming strategy for all pages which already have a translation
              at the same location (eg Main.WebHome), and for the ones matching the configuration
              parameter "sameNameTranslationClasses".
             */
            String sameNameTranslationClasses = translatorConfiguration.getSameNameTranslationClasses();
            XWikiContext xcontext = xcontextProvider.get();
            XWiki xwiki = xcontext.getWiki();
            try {
                XWikiDocument doc = xwiki.getDocument(reference, xcontext);

                if (CollectionUtils.isNotEmpty(doc.getTranslationLocales(xcontext))) {
                    return true;
                }

                for (String xclass : toList(sameNameTranslationClasses)) {
                    if (StringUtils.isNotEmpty(xclass)) {
                        List<BaseObject> objects = doc.getXObjects(referenceResolver.resolve(xclass));
                        if (!objects.isEmpty()) {
                            return true;
                        }
                    }
                }
            } catch (XWikiException e) {
                throw new MachineTranslationException(String.format("Failed to load document [%s]", reference), e);
            }

            return false;
        }
    }

    /**
     * Gets list of XClass properties to be translated.
     *
     * @return list of class properties if any, or empty list
     */
    public List<EntityReference> getTargetProperties()
    {
        String targetProperties = translatorConfiguration.getTargetProperties();
        List<String> properties = toList(targetProperties);
        List<EntityReference> references = new ArrayList<>();
        // TODO: return only properties of type String or LargeString
        for (String property : properties) {
            if (StringUtils.isNotEmpty(property)) {
                if (property.equals("doc.content")) {
                    references.add(getModelScriptService().resolveClassProperty(CONTENT_REFERENCE));
                } else {
                    references.add(getModelScriptService().resolveClassProperty(property));
                }
            }
        }
        return references;
    }

    /**
     * Gets ModelScriptService.
     *
     * @return ModelScriptService
     */
    private ModelScriptService getModelScriptService()
    {
        return (ModelScriptService) modelScriptService;
    }

    /**
     * This is to convert annotated HTML to wiki syntax.
     *
     * @param html HTML input
     * @param syntax Target syntax
     * @return Content in wiki syntax resulting from conversion
     */
    public String fromAnnotatedHTML(String html, Syntax syntax)
    {
        if (StringUtils.isNotEmpty(html)) {
            return this.htmlConverter.fromHTML(html, syntax.toIdString());
        }
        return "";
    }

    /**
     * Converts wiki syntax to HTML before translation.
     *
     * @param source Content
     * @param syntax Content syntax
     * @return converted content in HTML
     */
    public String toHTML(String source, Syntax syntax)
    {
        return StringUtils.isNotEmpty(source) ? htmlConverter.toHTML(source, syntax.toIdString()) : "";
    }

    /**
     * Converts string to list using LIST_ITEM_SEPARATOR.
     *
     * @param value string to be converted
     * @return string item list
     */
    public List<String> toList(String value)
    {
        if (value == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(value.split(LIST_ITEM_SEPARATOR));
    }

    protected void setAuthors(XWikiDocument page)
    {
        XWikiContext xcontext = xcontextProvider.get();
        DocumentReference userDocumentReference = xcontext.getUserReference();
        UserReference resolvedUserReference = this.userReferenceResolver.resolve(userDocumentReference);
        page.getAuthors().setCreator(resolvedUserReference);
        page.getAuthors().setContentAuthor(resolvedUserReference);
        page.getAuthors().setEffectiveMetadataAuthor(resolvedUserReference);
        page.getAuthors().setOriginalMetadataAuthor(resolvedUserReference);
    }

    /*
     * Glossary part
     */

    @Override
    public String getGlossaryName(Locale source, Locale target)
    {
        String prefix = getGlossaryNamePrefix();
        return getGlossaryName(source, target, prefix);
    }

    @Override
    public String getGlossaryName(Locale source, Locale target, String prefix)
    {
        return String.format("%s-%s-%s", prefix, source.toString(), target.toString());
    }

    @Override
    public String getGlossaryNamePrefix()
    {
        XWikiContext context = xwikiContextProvider.get();
        String glossariesPrefix = translatorConfiguration.getGlossaryNamePrefix();

        String wikiPrefix = context.getWikiId();

        if (org.apache.commons.lang3.StringUtils.isBlank(glossariesPrefix)) {
            return wikiPrefix;
        } else {
            return String.format("%s-%s", glossariesPrefix, wikiPrefix);
        }
    }

    @Override
    public Map<LocalePair, Boolean> getGlossaryLocalePairSupport() throws MachineTranslationException
    {
        XWikiContext context = xwikiContextProvider.get();
        List<LocalePair> translatorSupportedLocalePairs = getGlossaryLocalePairs();
        logger.debug("Fetched the list of supported glossary language combinations : [{}]",
            translatorSupportedLocalePairs);

        List<Locale> xwikiLanguages = xwikiContextProvider.get().getWiki().getAvailableLocales(context);
        Map<LocalePair, Boolean> pairSupport = new HashMap<>();

        for (Locale sourceLanguage : xwikiLanguages) {
            for (Locale targetLanguage : xwikiLanguages) {
                if (sourceLanguage.equals(targetLanguage)) {
                    continue;
                }
                String translatorSrcLang = normalizeLocale(sourceLanguage, NormalisationType.SOURCE_LANG_GLOSSARY);
                String translatorDstLang = normalizeLocale(targetLanguage, NormalisationType.TARGET_LANG_GLOSSARY);

                boolean foundMatchingLocalePairs = translatorSupportedLocalePairs.stream()
                    .anyMatch(entry ->
                        entry.getSourceLocale().toString().equals(translatorSrcLang)
                            && entry.getTargetLocale().toString().equals(translatorDstLang));
                pairSupport.put(new LocalePair(sourceLanguage, targetLanguage), foundMatchingLocalePairs);
            }
        }
        return pairSupport;
    }

    protected WikiReference getCurrentWikiReference()
    {
        return wikiDescriptorManager.getCurrentWikiReference();
    }
}
