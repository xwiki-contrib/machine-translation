## Translator

This following process should be tested with both option '`Translation documents location strategy`':

* Same location as original document
* In a space named after their language

### Basic document

Process:

1. Create a new page with some text in english.
2. Run the translator to these languages:
    * FR
    * FR_CA
    * ES
    * AR
    * ZH
    * DE
    * RU
3. Check the result on each pages

### Document with style and macros

1. Create a new page with some text in english, macros and images like this following:

```
{{info}}
Info macro
{{/info}}

this is an image

[[image:XWikiLogo.png]]

{{warning}}
Hello World
{{/warning}}

{{velocity}}
This in into the velocity macro so it shouldn't be traducted

#*
This is a comment
*#
$doc.title
{{velocity}}
```

2. Run the translator to these languages:
    * FR
    * FR_CA
    * ES
    * AR
    * ZH
    * DE
    * RU
3. Check the result on each pages

### Complex document with object

Process:

1. Create a new class named '`ItemFieldToTranslateClass`' into '`Sandbox`'.
2. Create a field into this class of type string named `myTransalatableField`.
3. Into the admin page, into the translator section, set '`Properties to be translated`' to '`Sandbox.ItemFieldToTranslateClass^myTransalatableField`'.
4. Create one page and add an object of type '`Sandbox.ItemFieldToTranslateClass`' and set some translatable text in english in the field '`myTransalatableField`'.
5. Run the translation of this page (to en_US, fr, fr_CA) and check that the translated page have the correct value on the field `myTransalatableField` of the object `ItemFieldToTranslateClass`.

### Filtered translatable document

The idea is to validate that the feature which select only documents with has at least one object of one of these classes will be translatable.

Process:

1. Create a new class named '`TranslationSelectorClass`'
1. Into the admin page, into the translator section, set '`Documents to be translated`' to '`TranslationSelectorClass`'.
1. Create one page with some text (without any object)
1. Create one page with some text with an object of type '`TranslationSelectorClass`'.
1. Run the translation (to en_US, fr, fr_CA).
1. Test that the translator apply only on the page with the '`TranslationSelectorClass`'.

### Test matrix

| Test                           | Location strategy: *Same location* | Location strategy: *specific space* |
| ------------------------------ |------------------------------------|-------------------------------------|
| Basic document                 |                                    |                                     |
| Complexe document with object  |                                    |                                     |
| Filtered translatable document |                                    |                                     |

### Edge case

#### Country specific translation

In case of DeepL, the translator could support the translation to some country specific language but not for all.

For DeepL by example, it supports the translation from 'FR' to 'EN_US' or 'FR' to 'EN_GB', so the translation of the {{glossaryReference glossaryId="Glossary" entryId="world"}}world{{/glossaryReference}} 'aspirateur' will be translated to 'vacuum cleaner' for 'EN_US' and 'hoover' for 'EN_GB'.
In other side for DeepL, it doesn't support the translation to country specific for fr like translating from 'en' to 'fr_CA' is not supported, only 'en' to 'fr' is supported.

## Translator glossary

### Single wiki

Process:

1. Add theses entries into the glossary:

|EN_US  |FR      |FR_CA     |DE             |ES         |
|-------|--------|----------|---------------|-----------|
|forhele|moinconu|moinconuca|unerkannteswort|palabrconoc|
|car    |carosse |camion    |Lastwagen      |camión     |
|city   |villefr |villefrca |Stadtenx       |villalx    |

2. Force the sychronisation.
2. Create a new page with will contain these worlds: forhele, car, city
3. Run the translation (to en_US, fr, fr_CA).
4. Check the resulted pages.

### Mulitple subwiki support

Do the same process than for [Single wiki](#single-wiki) but on the second wiki use this glossary translation table:

|EN_US  |FR           |FR_CA          |DE                  |ES              |
|-------|-------------|---------------|--------------------|----------------|
|forhele|wikibmoinconu|wikibmoinconuca|Wikibunerkannteswort|wikibpalabrconoc|
|car    |wikibcarosse |wikibcamion    |Wikiblastwagen      |wikibcamión     |
|city   |wikibvillefr |wikibvillefrca |Wikibstadtenx       |wikibvillalx    |

Then check again the result page with the new glossary.
