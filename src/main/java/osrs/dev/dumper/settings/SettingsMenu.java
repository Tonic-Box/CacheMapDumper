package osrs.dev.dumper.settings;

import net.runelite.cache.definitions.EnumDefinition;
import net.runelite.cache.definitions.StructDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The in-game settings menu as the cache describes it: its categories in tab order and each category's rows.
 */
final class SettingsMenu
{
    static final int KIND_TOGGLE = 0;
    static final int KIND_SLIDER = 1;
    static final int KIND_DROPDOWN = 2;
    static final int KIND_KEY_BINDING = 3;
    static final int KIND_NUMBER = 4;
    static final int KIND_HEADER = 5;
    static final int KIND_ACTION = 6;
    static final int KIND_BUTTON_ROW = 7;
    static final int KIND_INFO = 8;
    static final int KIND_COLOUR = 9;
    static final int KIND_LIST_EDITOR = 10;

    private static final int CATEGORIES_ENUM = 422;
    private static final int CATEGORY_NAME = 744;
    private static final int CATEGORY_ENTRIES = 745;
    private static final int MOBILE_ONLY = 740;
    private static final int SETTING_ID = 1077;
    private static final int KIND = 1078;
    private static final int NAME = 1086;
    private static final int KEYWORDS = 1088;
    private static final int OPTIONS = 1091;
    private static final int DESCRIPTION = 1096;
    private static final Pattern TAG = Pattern.compile("<[^>]*>");

    private final List<Category> categories = new ArrayList<>();

    private SettingsMenu()
    {
    }

    /**
     * Reads the menu from a cache's configs.
     * @param configs the configs
     * @return the menu
     */
    static SettingsMenu read(ConfigDefinitions configs)
    {
        SettingsMenu menu = new SettingsMenu();
        EnumDefinition categoryEnum = configs.enumDefinition(CATEGORIES_ENUM);
        if (categoryEnum == null)
        {
            return menu;
        }

        Set<Integer> listed = new HashSet<>();
        for (int position = 0; position < categoryEnum.getSize(); position++)
        {
            StructDefinition categoryStruct = configs.struct(ConfigDefinitions.enumInt(categoryEnum, position));
            if (categoryStruct == null)
            {
                continue;
            }

            Category category = new Category(strip(configs.stringParam(categoryStruct, CATEGORY_NAME)));
            EnumDefinition entries = configs.enumDefinition(configs.intParam(categoryStruct, CATEGORY_ENTRIES));
            if (entries != null)
            {
                Entry section = null;
                for (int key = 0; key < entries.getSize(); key++)
                {
                    int structId = ConfigDefinitions.enumInt(entries, key);
                    StructDefinition struct = structId == -1 ? null : configs.struct(structId);
                    if (struct == null || !listed.add(structId))
                    {
                        continue;
                    }

                    int kind = configs.intParam(struct, KIND);
                    if (kind < KIND_TOGGLE || kind > KIND_LIST_EDITOR)
                    {
                        continue;
                    }

                    Entry entry = new Entry(configs, struct, kind, section);
                    if (kind == KIND_HEADER)
                    {
                        section = entry;
                    }
                    category.entries.add(entry);
                }
            }
            menu.categories.add(category);
        }
        return menu;
    }

    /**
     * @return the categories in tab order
     */
    List<Category> categories()
    {
        return Collections.unmodifiableList(categories);
    }

    private static String strip(String text)
    {
        return text == null ? null : TAG.matcher(text).replaceAll("");
    }

    /**
     * A tab of the menu.
     */
    static final class Category
    {
        private final String name;
        private final List<Entry> entries = new ArrayList<>();

        private Category(String name)
        {
            this.name = name;
        }

        /**
         * @return the tab name
         */
        String name()
        {
            return name;
        }

        /**
         * @return the rows in menu order
         */
        List<Entry> entries()
        {
            return Collections.unmodifiableList(entries);
        }
    }

    /**
     * A row of the menu.
     */
    static final class Entry
    {
        private final int structId;
        private final int kind;
        private final int settingId;
        private final String name;
        private final String description;
        private final List<String> keywords = new ArrayList<>();
        private final boolean mobileOnly;
        private final int optionEnum;
        private final List<String> options = new ArrayList<>();
        private final Entry section;

        private Entry(ConfigDefinitions configs, StructDefinition struct, int kind, Entry section)
        {
            this.structId = struct.getId();
            this.kind = kind;
            this.section = section;
            this.settingId = configs.intParam(struct, SETTING_ID);
            this.name = ConfigDefinitions.hasParam(struct, NAME) ? strip(configs.stringParam(struct, NAME)) : null;
            String rawDescription = strip(configs.stringParam(struct, DESCRIPTION));
            this.description = rawDescription == null ? "" : rawDescription;
            this.mobileOnly = configs.intParam(struct, MOBILE_ONLY) == 1;

            String rawKeywords = configs.stringParam(struct, KEYWORDS);
            if (rawKeywords != null)
            {
                for (String keyword : rawKeywords.split(","))
                {
                    if (!keyword.trim().isEmpty())
                    {
                        keywords.add(keyword.trim());
                    }
                }
            }

            boolean hasOptions = kind == KIND_DROPDOWN || kind == KIND_KEY_BINDING;
            this.optionEnum = hasOptions ? configs.intParam(struct, OPTIONS) : -1;
            EnumDefinition optionValues = optionEnum == -1 ? null : configs.enumDefinition(optionEnum);
            if (optionValues != null)
            {
                for (int value = 0; value < optionValues.getSize(); value++)
                {
                    options.add(strip(ConfigDefinitions.enumString(optionValues, value)));
                }
            }
        }

        /**
         * @return the struct id the row is keyed by
         */
        int structId()
        {
            return structId;
        }

        /**
         * @return the kind code
         */
        int kind()
        {
            return kind;
        }

        /**
         * @return the setting id, or 0 for rows without one
         */
        int settingId()
        {
            return settingId;
        }

        /**
         * @return the name, or the first keyword when the row has none
         */
        String label()
        {
            if (name != null)
            {
                return name;
            }
            return keywords.isEmpty() ? null : keywords.get(0);
        }

        /**
         * @return the description, empty when there is none
         */
        String description()
        {
            return description;
        }

        /**
         * @return whether the row is only shown on mobile
         */
        boolean mobileOnly()
        {
            return mobileOnly;
        }

        /**
         * @return whether the row only lays out the menu
         */
        boolean layout()
        {
            return kind == KIND_HEADER || kind == KIND_BUTTON_ROW || kind == KIND_INFO;
        }

        /**
         * @return the option enum id, or -1 when the row has no options
         */
        int optionEnum()
        {
            return optionEnum;
        }

        /**
         * @return the option labels, each option's value being its index
         */
        List<String> options()
        {
            return Collections.unmodifiableList(options);
        }

        /**
         * @return the header row this row is listed under, or null
         */
        Entry section()
        {
            return section;
        }
    }
}
