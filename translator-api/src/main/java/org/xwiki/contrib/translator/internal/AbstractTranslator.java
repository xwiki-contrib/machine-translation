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

import java.io.InputStreamReader;
import java.io.OutputStream;
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
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.translator.TranslationSet;
import org.xwiki.contrib.translator.Translator;
import org.xwiki.contrib.translator.TranslatorConfiguration;
import org.xwiki.contrib.translator.TranslatorManager;
import org.xwiki.localization.LocaleUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
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

public abstract class AbstractTranslator implements Translator
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> referenceResolver;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    @Inject
    private ContextualAuthorizationManager authorizationManager;

    @Inject
    protected Logger logger;

    @Inject
    private HTMLConverter htmlConverter;

    @Inject
    @Named("model")
    private ScriptService modelScriptService;

    @Inject
    private EntityNameValidationManager entityNameValidationManager;

    @Inject
    private TranslatorManager translatorManager;

    static LocalDocumentReference TRANSLATION_CLASS_REFERENCE =
        new LocalDocumentReference(Arrays.asList("XWiki", "Translator", "Translation"), "TranslationClass");

    static final String LIST_ITEM_SEPARATOR = ",";

    @Inject
    protected TranslatorConfiguration translatorConfiguration;

    @Override
    public void translate(EntityReference reference, Locale toLocale) throws Exception
    {
        try {
            XWikiContext xcontext = xcontextProvider.get();
            XWiki xwiki = xcontext.getWiki();
            XWikiDocument doc = xwiki.getDocument(reference, xcontext).clone();

            Locale fromLocale = doc.getDefaultLocale();
            logger.info("Original page: {} {}", doc.getDocumentReference(), fromLocale);
            logger.info("Translation locale: {}", toLocale);

            String translationTitle = translate(doc.getTitle(), fromLocale, toLocale, true);
            // TODO: use reference resolver
            EntityReference translationReference =
                computeTranslationReference(doc.getDocumentReference(), translationTitle, toLocale);

            if (!this.authorizationManager.hasAccess(Right.EDIT, translationReference)) {
                throw new Exception("Edit right required");
            }

            XWikiDocument translationPage = null;
            logger.info("Translation page: {}", translationReference);
            if (!isSameNameTranslationNamingStrategy(reference)) {
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

            translationPage.setTitle(translationTitle);
            String content = doc.getContent();
            for (EntityReference property : getTargetProperties()) {
                String propertyString = getModelScriptService().serialize(property);
                // TODO: move to static
                if (propertyString.equals("XWiki.Document^content")) {
                    String html = toHTML(content, Syntax.XWIKI_2_1);
                    try {
                        String translation = translate(html, fromLocale, toLocale, true);
                        translation = fromAnnotatedHTML(translation, Syntax.XWIKI_2_1);
                        translationPage.setContent(translation);
                    } catch (Exception e) {
                        logger.error("Exception", e);
                    }
                } else if (!isSameNameTranslationNamingStrategy(reference)) {
                    List<BaseObject> objects = doc.getXObjects(property.getParent());
                    for (BaseObject obj : objects) {
                        logger.debug("Translating object property [{}] [{}]...", propertyString, obj.getNumber());
                        String value = obj.getLargeStringValue(property.getName());
                        if (StringUtils.isNotEmpty(value)) {
                            String html = toHTML(value, Syntax.XWIKI_2_1);
                            try {
                                String translation = translate(html, fromLocale, toLocale, true);
                                translation = fromAnnotatedHTML(translation, Syntax.XWIKI_2_1);
                                BaseObject object =
                                    translationPage.getXObject(property.getParent(), obj.getNumber());
                                object.setLargeStringValue(property.getName(), translation);
                            } catch (Exception e) {
                                logger.error("Exception", e);
                            }
                        }
                    }
                }
            }

            if (!isSameNameTranslationNamingStrategy(reference)) {
                BaseObject translationObj = translationPage.getXObject(TRANSLATION_CLASS_REFERENCE);
                if (translationObj == null) {
                    translationObj = translationPage.newXObject(TRANSLATION_CLASS_REFERENCE, xcontext);
                }
                translationObj.setStringValue("originalPage",
                    entityReferenceSerializer.serialize(doc.getDocumentReference()));
                translationObj.setDateValue("automatedTranslationDate", new Date());
                // TODO: fill in translator appropriately
                // translationObj.setStringValue("translator", "XWiki.Translator.DeepL");
            }

            DocumentReference currentUserDocumentReference = xcontext.getUserReference();
            UserReference currentUserReference = userReferenceResolver.resolve(currentUserDocumentReference);

            translationPage.getAuthors().setCreator(currentUserReference);
            translationPage.getAuthors().setContentAuthor(currentUserReference);
            translationPage.getAuthors().setEffectiveMetadataAuthor(currentUserReference);
            translationPage.getAuthors().setOriginalMetadataAuthor(currentUserReference);
            xwiki.saveDocument(translationPage, "Translation from " + fromLocale.getLanguage(), xcontext);
        } catch (Exception e) {
            logger.error("Translation error {[]}", e);
        }
    }

    @Override
    public DocumentReference computeTranslationReference(DocumentReference reference, String title, Locale locale)
        throws XWikiException
    {
        String translationName =
            entityNameValidationManager.getEntityReferenceNameStrategy().transform(title);
        if (!isSameNameTranslationNamingStrategy(reference)) {
            String[] array = new String[] { locale.toString(), translationName };
            // TODO: use resolver
            // TODO: do not hardcode WebHome
            return getModelScriptService().createDocumentReference(reference.getWikiReference().getName(),
                Arrays.asList(array), "WebHome");
        } else {
            return reference;
        }
    }

    @Override
    public void translate(EntityReference reference, Locale[] toLocales, boolean html) throws Exception
    {
        XWikiContext xcontext = xcontextProvider.get();
        XWiki xwiki = xcontext.getWiki();
        try {
            XWikiDocument document = xwiki.getDocument(reference, xcontext).clone();

            // If page is already a translation, get original document
            BaseObject translationObj = document.getXObject(TRANSLATION_CLASS_REFERENCE);
            if (translationObj != null) {
                String originalDocumentName = translationObj.getStringValue("originalPage");
                document = xwiki.getDocument(referenceResolver.resolve(originalDocumentName), xcontext).clone();
            }

            for (Locale toLocale : toLocales) {
                translate(reference, toLocale);
            }
        } catch (XWikiException e) {
            logger.error("Exception", e);
        }
    }

    @Override
    public OutputStream translate(InputStreamReader reader, Locale from, Locale to, boolean html)
    {
        return null;
    }

    /**
     * Gets all existing translations of passed reference as a {@link TranslationSet}
     */
    @Override
    public TranslationSet getTranslations(EntityReference reference) throws XWikiException, QueryException
    {
        XWikiContext xcontext = xcontextProvider.get();
        XWiki xwiki = xcontext.getWiki();
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
                XWikiDocument translatedDocument = doc.getTranslatedDocument(locale, xcontext);
                map.put(locale,
                    Arrays.asList(translatedDocument.getDocumentReference(), translatedDocument.getTitle(),
                        locale));
            }
            return new TranslationSet(doc.getDocumentReference(), doc.getTitle(),
                doc.getDefaultLocale(), map);
        }

        /** Either the current page is already a translation or it is the original document */
        if (translationObj != null) {
            // 1) First case: the current page is a translation
            originalPageName = translationObj.getStringValue("originalPage");
            hql.append("select doc.fullName, doc.title, doc.language from XWikiDocument as doc, ");
            hql.append("BaseObject as obj where doc.fullName = :originalPage and obj.name = doc.fullName");
            hql.append(" and obj.className = :class order by doc.language");
            Query query = queryManager.createQuery(hql.toString(), Query.HQL);
            query.bindValue("class", entityReferenceSerializer.serialize(TRANSLATION_CLASS_REFERENCE));
            query.bindValue("originalPage", originalPageName);
            entries = query.execute();
            XWikiDocument originalPage =
                xwiki.getDocument(referenceResolver.resolve(originalPageName), xcontext);
            entries.add(
                new String[] { originalPageName, originalPage.getTitle(), originalPage.getRealLocale().toString() });
        } else {
            // 2) Second case: the current page is the original one
            originalPageName = entityReferenceSerializer.serialize(reference);
        }

        hql = new StringBuilder("select doc.fullName, doc.title, doc.defaultLanguage from XWikiDocument as doc, ");
        hql.append("BaseObject as obj, StringProperty as prop where obj.name = doc.fullName and obj.className = ");
        hql.append(":class and prop.id.id = obj.id and prop.id.name = :prop and prop.value = :originalPage and ");
        hql.append("doc.fullName <> :currentDoc");
        Query query = queryManager.createQuery(hql.toString(), Query.HQL);
        query.bindValue("class", entityReferenceSerializer.serialize(TRANSLATION_CLASS_REFERENCE));
        query.bindValue("originalPage", originalPageName);
        query.bindValue("prop", "originalPage");
        query.bindValue("currentDoc", entityReferenceSerializer.serialize(reference));
        entries.addAll(query.execute());
        Map<Locale, List<Object>> map = new HashMap<>();
        for (Object[] obj : entries) {
            map.put(LocaleUtils.toLocale(obj[2].toString()),
                Arrays.asList(referenceResolver.resolve(obj[0].toString()), obj[1],
                    LocaleUtils.toLocale(obj[2].toString())));
        }
        DocumentReference originalDocumentReference = referenceResolver.resolve(originalPageName);
        XWikiDocument originalPage = xwiki.getDocument(originalDocumentReference, xcontext);
        return new TranslationSet(originalDocumentReference, originalPage.getTitle(),
            originalPage.getDefaultLocale(), map);
    }

    @Override
    public boolean isTranslatable(EntityReference reference)
    {
        String targetClasses = translatorConfiguration.getTargetClasses();
        if (StringUtils.isEmpty(targetClasses)) {
            return true;
        }
        List<String> xclasses = List.of(targetClasses.split(","));
        XWikiContext xcontext = xcontextProvider.get();
        XWiki xwiki = xcontext.getWiki();
        try {
            XWikiDocument doc = xwiki.getDocument(reference, xcontext);
            for (String xclass : xclasses) {
                if (StringUtils.isNotEmpty(xclass)) {
                    DocumentReference classReference = referenceResolver.resolve(xclass);
                    List<BaseObject> objects = doc.getXObjects(classReference);
                    if (!objects.isEmpty()) {
                        return true;
                    }
                }
            }
        } catch (XWikiException e) {
            logger.error("Exception", e);
        }
        return false;
    }

    /**
     * Gets list of XClass properties to be translated
     *
     * @return list of class properties if any, or empty list
     */
    public List<EntityReference> getTargetProperties()
    {
        String targetProperties = translatorConfiguration.getTargetProperties();
        List<String> properties = List.of(targetProperties.split(","));
        List<EntityReference> references = new ArrayList<>();
        // TODO: return only properties of type String or LargeString
        for (String property : properties) {
            if (StringUtils.isNotEmpty(property)) {
                if (property.equals("doc.content")) {
                    property = "XWiki.Document^content";
                }
                references.add(getModelScriptService().resolveClassProperty(property));
            }
        }
        return references;
    }

    private ModelScriptService getModelScriptService()
    {
        return (ModelScriptService) modelScriptService;
    }

    /**
     * This is to convert annotated HTML to wiki syntax.
     */
    public String fromAnnotatedHTML(String html, Syntax syntax)
    {
        if (StringUtils.isNotEmpty(html)) {
            return this.htmlConverter.fromHTML(html, syntax.toIdString());
        }
        return "";
    }

    public String toHTML(String source, Syntax syntax)
    {
        return StringUtils.isNotEmpty(source) ? htmlConverter.toHTML(source, syntax.toIdString()) : "";
    }

    public List<String> toList(String value)
    {
        if (value == null) {
            return Collections.emptyList();
        }
        return List.of(value.split(LIST_ITEM_SEPARATOR));
    }

    @Override
    public boolean isSameNameTranslationNamingStrategy(EntityReference reference) throws XWikiException
    {
        if (translatorConfiguration.isSameNameTranslationNamingStrategy()) {
            return true;
        } else {
            String sameNameTranslationClasses = translatorConfiguration.getSameNameTranslationClasses();
            XWikiContext xcontext = xcontextProvider.get();
            XWiki xwiki = xcontext.getWiki();
            XWikiDocument doc = xwiki.getDocument(reference, xcontext);

            for (String xclass : toList(sameNameTranslationClasses)) {
                if (StringUtils.isNotEmpty(xclass)) {
                    List<BaseObject> objects = doc.getXObjects(referenceResolver.resolve(xclass));
                    if (!objects.isEmpty()) {
                        return true;
                    }
                }
            }

            return false;
        }
    }
}
