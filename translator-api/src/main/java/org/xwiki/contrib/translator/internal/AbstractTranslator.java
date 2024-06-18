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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.contrib.translator.TranslationSet;
import org.xwiki.contrib.translator.Translator;
import org.xwiki.contrib.translator.TranslatorConfiguration;
import org.xwiki.contrib.translator.TranslatorException;
import org.xwiki.contrib.translator.TranslatorManager;
import org.xwiki.contrib.translator.model.GlossaryInfo;
import org.xwiki.contrib.translator.model.LocalePairs;
import org.xwiki.localization.LocaleUtils;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.model.validation.EntityNameValidationManager;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.text.StringUtils;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
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
        new LocalDocumentReference(Arrays.asList("XWiki", "Translator", "Translation"), "TranslationClass");

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
    protected TranslatorConfiguration translatorConfiguration;

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
     * User reference resolver.
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

    @Override
    public void translate(EntityReference reference, Locale toLocale) throws TranslatorException
    {
        try {
            if (!authorizationManager.hasAccess(Right.VIEW, reference)) {
                throw new TranslatorException(String.format("Denied view access on [%s]", reference));
            }

            XWikiContext xcontext = xcontextProvider.get();
            XWiki xwiki = xcontext.getWiki();
            XWikiDocument doc = xwiki.getDocument(reference, xcontext).clone();

            Locale fromLocale = doc.getDefaultLocale();
            logger.info("Translating [{}] [{}] to locale [{}]", doc.getDocumentReference(), fromLocale, toLocale);

            String translationTitle = translate(doc.getTitle(), fromLocale, toLocale, false);
            // TODO: use reference resolver
            EntityReference translationReference =
                computeTranslationReference(doc.getDocumentReference(), translationTitle, toLocale);

            if (!this.authorizationManager.hasAccess(Right.EDIT, translationReference)) {
                throw new TranslatorException(String.format("Denied edit right on [%s]", translationReference));
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
                // translationObj.setStringValue("translator", "XWiki.Translator.DeepL");
            }

            setAuthors(translationPage);
            xwiki.saveDocument(translationPage, "Translation from " + fromLocale.getLanguage(), xcontext);
        } catch (XWikiException e) {
            logger.error("Translator error {[]}", e);
            throw new TranslatorException(e);
        }
    }

    private XWikiDocument getTranslationPage(XWikiDocument doc, EntityReference translationReference,
        Locale toLocale) throws XWikiException, TranslatorException
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
        throws TranslatorException
    {
        String content = original.getContent();
        for (EntityReference property : getTargetProperties()) {
            String propertyString = getModelScriptService().serialize(property);
            // TODO: move to static
            if (propertyString.equals(CONTENT_REFERENCE)) {
                String html = toHTML(content, Syntax.XWIKI_2_1);
                String translatedContent = translate(html, from, to, true);
                translatedContent = fromAnnotatedHTML(translatedContent, Syntax.XWIKI_2_1);
                translation.setContent(translatedContent);
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
    }

    @Override
    public DocumentReference computeTranslationReference(DocumentReference originalDocument, String translationTitle,
        Locale translationLocale) throws TranslatorException
    {
        LocalDocumentReference localReference = new LocalDocumentReference(originalDocument);
        String translationName = translationTitle;
        if (StringUtils.isNotEmpty(translationTitle)) {
            translationName = entityNameValidationManager.getEntityReferenceNameStrategy().transform(translationTitle);
        }
        if (!isSameNameTranslationNamingStrategy(originalDocument)) {
            SpaceReference localeSpaceReference =
                new SpaceReference(translationLocale.toString(), originalDocument.getWikiReference());

            EntityReference target = localReference.appendParent(localeSpaceReference);
            if (StringUtils.isEmpty(translationName)) {
                return new DocumentReference(target);
            }

            String defaultDocumentName =
                this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT).getName();

            if (!target.getName().equals(defaultDocumentName)) {
                return new DocumentReference(translationName, target.getParent(), null);
            } else {
                target = target.replaceParent(target.getParent(),
                    new SpaceReference(translationName, target.getParent().getParent()));
                return new DocumentReference(target);
            }
        } else {
            return originalDocument;
        }
    }

    @Override
    public void translate(EntityReference reference, Locale[] toLocales) throws TranslatorException
    {
        for (Locale toLocale : toLocales) {
            translate(reference, toLocale);
        }
    }

    @Override
    public TranslationSet getTranslations(EntityReference reference) throws TranslatorException
    {
        XWikiContext xcontext = xcontextProvider.get();
        XWiki xwiki = xcontext.getWiki();
        try {
            XWikiDocument doc = xwiki.getDocument(reference, xcontext);
            BaseObject translationObj = doc.getXObject(TRANSLATION_CLASS_REFERENCE);
            String originalPageName = null;
            StringBuilder hql = new StringBuilder();
            List<String[]> entries = new ArrayList<>();

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

            /** Either the current page is already a translation or it is the original document */
            if (translationObj != null) {
                // 1) First case: the current page is a translation
                originalPageName = translationObj.getStringValue(ORIGINAL_PAGE_PROPERTY);
                hql.append("select doc.fullName, doc.title, doc.language from XWikiDocument as doc, ");
                hql.append("BaseObject as obj where doc.fullName = :originalPage and obj.name = doc.fullName");
                hql.append(" and obj.className = :class order by doc.language");
                Query query = queryManager.createQuery(hql.toString(), Query.HQL);
                query.bindValue("class", entityReferenceSerializer.serialize(TRANSLATION_CLASS_REFERENCE));
                query.bindValue(ORIGINAL_PAGE_PROPERTY, originalPageName);
                entries = query.execute();
                DocumentReference originalPageReference = referenceResolver.resolve(originalPageName);
                if (authorizationManager.hasAccess(Right.VIEW, originalPageReference)) {
                    XWikiDocument originalPage = xwiki.getDocument(originalPageReference, xcontext);
                    entries.add(
                        new String[] { originalPageName, originalPage.getTitle(),
                            originalPage.getRealLocale().toString() });
                }
            } else {
                // 2) Second case: the current page is the original one
                originalPageName = entityReferenceSerializer.serialize(reference);
            }

            hql = new StringBuilder("select doc.fullName, doc.title, doc.defaultLanguage from XWikiDocument as doc, ");
            hql.append("BaseObject as obj, StringProperty as prop where obj.name = doc.fullName and obj.className = ");
            hql.append(":className and prop.id.id = obj.id and prop.id.name = :prop ");
            hql.append("and prop.value = :originalPage and doc.fullName <> :currentDoc");
            Query query = queryManager.createQuery(hql.toString(), Query.HQL);
            query.bindValue("className", entityReferenceSerializer.serialize(TRANSLATION_CLASS_REFERENCE));
            query.bindValue("prop", ORIGINAL_PAGE_PROPERTY);
            query.bindValue(ORIGINAL_PAGE_PROPERTY, originalPageName);
            query.bindValue("currentDoc", entityReferenceSerializer.serialize(reference));
            entries.addAll(query.execute());
            Map<Locale, List<Object>> map = new HashMap<>();
            for (Object[] obj : entries) {
                Locale locale = LocaleUtils.toLocale(obj[2].toString());
                DocumentReference pageReference = referenceResolver.resolve(obj[0].toString());
                if (xwiki.getAvailableLocales(xcontext).contains(locale) && authorizationManager.hasAccess(Right.VIEW,
                    pageReference))
                {
                    map.put(locale, Arrays.asList(pageReference, obj[1], locale));
                }
            }
            DocumentReference originalDocumentReference = referenceResolver.resolve(originalPageName);
            XWikiDocument originalPage = xwiki.getDocument(originalDocumentReference, xcontext);
            return new TranslationSet(originalDocumentReference, originalPage.getTitle(),
                originalPage.getDefaultLocale(), map);
        } catch (XWikiException | QueryException e) {
            throw new TranslatorException(String.format("Failed to get translations for [%s]", reference), e);
        }
    }

    @Override
    public boolean isTranslatable(DocumentReference reference) throws TranslatorException
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
            throw new TranslatorException(String.format("Failed to check if [%s] is translatable", reference), e);
        }
        return false;
    }

    @Override
    public boolean canTranslate(DocumentReference reference) throws TranslatorException
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
    public boolean canTranslate(DocumentReference reference, Locale toLocale) throws TranslatorException
    {
        if (!isTranslatable(reference)) {
            return false;
        }

        DocumentReference translationReference = computeTranslationReference(reference, null, toLocale);
        return authorizationManager.hasAccess(Right.EDIT, translationReference);
    }

    @Override
    public boolean isSameNameTranslationNamingStrategy(EntityReference reference) throws TranslatorException
    {
        if (translatorConfiguration.isSameNameTranslationNamingStrategy()) {
            return true;
        } else {
            String sameNameTranslationClasses = translatorConfiguration.getSameNameTranslationClasses();
            XWikiContext xcontext = xcontextProvider.get();
            XWiki xwiki = xcontext.getWiki();
            try {
                XWikiDocument doc = xwiki.getDocument(reference, xcontext);

                for (String xclass : toList(sameNameTranslationClasses)) {
                    if (StringUtils.isNotEmpty(xclass)) {
                        List<BaseObject> objects = doc.getXObjects(referenceResolver.resolve(xclass));
                        if (!objects.isEmpty()) {
                            return true;
                        }
                    }
                }
            } catch (XWikiException e) {
                throw new TranslatorException(String.format("Failed to load document [%s]", reference), e);
            }

            return false;
        }
    }

    @Override
    public String getGlossaryName(Locale source, Locale target)
    {
        String prefix = getGlossaryNamePrefix();
        return getGlossaryName(source, target, prefix);
    }

    @Override
    public String getGlossaryName(Locale source, Locale target, String prefix)
    {
        return prefix + "-" + source.toString() + "-" + target.toString();
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
            return glossariesPrefix + "-" + wikiPrefix;
        }
    }

    @Override
    public abstract List<LocalePairs> getGlossaryLocalePairs() throws TranslatorException;

    @Override
    public abstract List<GlossaryInfo> getGlossaries() throws TranslatorException;

    @Override
    public abstract Map<String, String> getGlossaryEntryDetails(String id) throws TranslatorException;

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
        return List.of(value.split(LIST_ITEM_SEPARATOR));
    }

    protected void setAuthors(XWikiDocument page)
    {
        XWikiContext xcontext = xcontextProvider.get();
        DocumentReference currentUserDocumentReference = xcontext.getUserReference();
        UserReference currentUserReference = userReferenceResolver.resolve(currentUserDocumentReference);

        page.getAuthors().setCreator(currentUserReference);
        page.getAuthors().setContentAuthor(currentUserReference);
        page.getAuthors().setEffectiveMetadataAuthor(currentUserReference);
        page.getAuthors().setOriginalMetadataAuthor(currentUserReference);
    }
}
