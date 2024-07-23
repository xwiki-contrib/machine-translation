The tests below have to be executed with all possible strategies available in Translator configuration option `Translation documents location strategy`.

## Basic document translation

1. Set wiki to multilingual with supported languages:FR, FR_CA, ES, AR, ZH, DE, RU
1. Create English page with some text
1. Run translator on all wiki languages

### Expected result

* Each page is translated
* The original page and its translations contain the other available translations in the `Translations` content menu
* In page history, the last author of each translation is the user who performed the translations

## Document with macros and images

1. Create English page with text, macros and images like this following:

```
{{info}}
This is an information message.
{{/info}}

[[Image with caption>>image:XWikiLogo.png]]

{{warning}}
Hello World - Warning
{{/warning}}

{{velocity}}
This text is a Velocity block hence it is not translated.

#*
This is a comment
*#
Document title: $doc.title
{{/velocity}}
```

2. Run translator on all wiki languages

### Expected result

The page is correctly translated in all languages

## Document with object and attachments

1. Make sure the Help extension is installed
1. Edit class `MoviesClass` and add TextArea field named `summary`
1. In administration translator section, set `Properties to be translated` to `XWiki.Movie.MovieClass^summary`'.
1. In object mode, edit Movie `Modern Times` and fill in summary from some text from the [movie plot section on Wikipedia](https://en.wikipedia.org/wiki/Modern_Times_(film))
1. Translate page to available wiki languages

### Expected result

When editing the translated pages in object mode, the field `summary` is translated correctly, and all attachments have been copied to the translations (only in the case of distinct translation location strategy) 

## Translatable pages

The idea is to validate that the feature which select only documents with has at least one object of one of these classes will be translatable.

Process:

1. Create a new class named '`TranslationSelectorClass`'
1. Into the admin page, into the translator section, set '`Documents to be translated`' to '`TranslationSelectorClass`'.
1. Create one page with some text (without any object)
1. Create one page with some text with an object of type '`TranslationSelectorClass`'.
1. Run the translation (to en_US, fr, fr_CA).
1. Test that the translator apply only on the page with the '`TranslationSelectorClass`'.

## Country specific translation

In case of DeepL, the translator could support the translation to some country specific language but not for all.

For DeepL by example, it supports the translation from 'FR' to 'EN_US' or 'FR' to 'EN_GB', so the translation of the {{glossaryReference glossaryId="Glossary" entryId="world"}}world{{/glossaryReference}} 'aspirateur' will be translated to 'vacuum cleaner' for 'EN_US' and 'hoover' for 'EN_GB'.
In other side for DeepL, it doesn't support the translation to country specific for fr like translating from 'en' to 'fr_CA' is not supported, only 'en' to 'fr' is supported.

## Translation of documents containing Glossary entries 

1. Configure wiki with the following locales: locales EN_US, FR, FR_CA, DE and ES
1. Install Glossary Machine Translation app
1. Add the entries below to the Glossary, with their translations
1. In Glossary Machine Translation app, go to page `Translation glossary explorer` and hit `Launch glossary synchronization`
1. Create page containing Glossary entries
1. Translate page to all available wiki locales

Glossary entries:

| EN_US   | FR         | FR_CA  | DE         | ES         |
|---------|------------|--------|------------|------------|
| bicycle | bicyclette | biclou | Veloziped  | velocípedo |


### Expected result

The translated documents contain the word translations set in the local Glossary, not the ones provided by the automated translator


## Glossary translations in subwikis

1. Run the same scenario as above in a subwiki, with the following Glossary entries and translations, and using also the word "bicycle".

Glossary entries:

|EN_US  | FR            | FR_CA | DE        | ES    |
|-------|---------------|-------|-----------|-------|
|car    | carosse       | auto  | Automobil | carro |


### Expected result

The glossary entries should be translated as defined in the glossary, instead of the translation provided by the translator. The entries present in the other subwiki glossary should have a standard translation instead of the custom ones defined in the other subwiki. 

