package com.dhananjaysaini.musicplayerapp.repository

import android.graphics.Color
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.model.AppTheme
import com.dhananjaysaini.musicplayerapp.model.ThemeType

object ThemeRepository {

    fun getColorThemes(): List<AppTheme> = listOf(
        AppTheme("color_1", "Midnight Neon", ThemeType.GRADIENT, "Color",
            drawableRes = R.drawable.gradient_theme_1, swatchRes = R.drawable.swatch_theme_1,
            startColor = Color.parseColor("#0B1B3D"), centerColor = Color.parseColor("#1D2A68"), endColor = Color.parseColor("#2C1F5C")),

        AppTheme("color_2", "Cyber Magenta", ThemeType.GRADIENT, "Color",
            drawableRes = R.drawable.gradient_theme_2, swatchRes = R.drawable.swatch_theme_2,
            startColor = Color.parseColor("#22002E"), centerColor = Color.parseColor("#4D0958"), endColor = Color.parseColor("#6E0D5A")),

        AppTheme("color_3", "Royal Navy", ThemeType.GRADIENT, "Color",
            drawableRes = R.drawable.gradient_theme_3, swatchRes = R.drawable.swatch_theme_3,
            startColor = Color.parseColor("#0B132B"), centerColor = Color.parseColor("#1C2541"), endColor = Color.parseColor("#3A506B"), isPro = true),

        AppTheme("color_4", "Deep Purple", ThemeType.GRADIENT, "Color",
            drawableRes = R.drawable.gradient_theme_4, swatchRes = R.drawable.swatch_theme_4,
            startColor = Color.parseColor("#12072B"), centerColor = Color.parseColor("#2B1055"), endColor = Color.parseColor("#591A8A"), isPro = true),

        AppTheme("color_5", "Espresso Mocha", ThemeType.GRADIENT, "Color",
            drawableRes = R.drawable.gradient_theme_5, swatchRes = R.drawable.swatch_theme_5,
            startColor = Color.parseColor("#1A1008"), centerColor = Color.parseColor("#2E1B0E"), endColor = Color.parseColor("#5C381E"), isPro = true),

        AppTheme("color_6", "Blush Pearl", ThemeType.GRADIENT, "Color",
            drawableRes = R.drawable.gradient_theme_6, swatchRes = R.drawable.swatch_theme_6,
            startColor = Color.parseColor("#FFF0F5"), centerColor = Color.parseColor("#FADADD"), endColor = Color.parseColor("#F2C4CE"), isDark = false),

        AppTheme("color_7", "Slate Graphite", ThemeType.GRADIENT, "Color",
            drawableRes = R.drawable.gradient_theme_7, swatchRes = R.drawable.swatch_theme_7,
            startColor = Color.parseColor("#1A1D20"), centerColor = Color.parseColor("#2C3238"), endColor = Color.parseColor("#3E464E")),

        AppTheme("color_8", "Obsidian Onyx", ThemeType.GRADIENT, "Color",
            drawableRes = R.drawable.gradient_theme_8, swatchRes = R.drawable.swatch_theme_8,
            startColor = Color.parseColor("#080808"), centerColor = Color.parseColor("#141414"), endColor = Color.parseColor("#242424")),

        AppTheme("color_9", "Electric Cobalt", ThemeType.GRADIENT, "Color",
            drawableRes = R.drawable.gradient_theme_9, swatchRes = R.drawable.swatch_theme_9,
            startColor = Color.parseColor("#0A1931"), centerColor = Color.parseColor("#153E90"), endColor = Color.parseColor("#005082")),

        AppTheme("color_10", "Sky Blue", ThemeType.GRADIENT, "Color",
            drawableRes = R.drawable.gradient_theme_10, swatchRes = R.drawable.swatch_theme_10,
            startColor = Color.parseColor("#034375"), centerColor = Color.parseColor("#006CB5"), endColor = Color.parseColor("#0096C7")),

        AppTheme("color_11", "Lime Forest", ThemeType.GRADIENT, "Color",
            drawableRes = R.drawable.gradient_theme_11, swatchRes = R.drawable.swatch_theme_11,
            startColor = Color.parseColor("#142814"), centerColor = Color.parseColor("#2B5524"), endColor = Color.parseColor("#588B35")),

        AppTheme("color_12", "Emerald Jade", ThemeType.GRADIENT, "Color",
            drawableRes = R.drawable.gradient_theme_12, swatchRes = R.drawable.swatch_theme_12,
            startColor = Color.parseColor("#062817"), centerColor = Color.parseColor("#0E4729"), endColor = Color.parseColor("#1B6A3E")),

        AppTheme("color_13", "Aqua Marine", ThemeType.GRADIENT, "Color",
            drawableRes = R.drawable.gradient_theme_13, swatchRes = R.drawable.swatch_theme_13,
            startColor = Color.parseColor("#032535"), centerColor = Color.parseColor("#0B525B"), endColor = Color.parseColor("#1B7A82")),

        AppTheme("color_14", "Sunset Orange", ThemeType.GRADIENT, "Color",
            drawableRes = R.drawable.gradient_theme_14, swatchRes = R.drawable.swatch_theme_14,
            startColor = Color.parseColor("#361304"), centerColor = Color.parseColor("#662400"), endColor = Color.parseColor("#A83E00")),

        AppTheme("color_15", "Crimson Ruby", ThemeType.GRADIENT, "Color",
            drawableRes = R.drawable.gradient_theme_15, swatchRes = R.drawable.swatch_theme_15,
            startColor = Color.parseColor("#2C0609"), centerColor = Color.parseColor("#5C0D12"), endColor = Color.parseColor("#8C1820")),

        AppTheme("color_16", "Arctic Aurora", ThemeType.GRADIENT, "Color",
            drawableRes = R.drawable.gradient_theme_16, swatchRes = R.drawable.swatch_theme_16,
            startColor = Color.parseColor("#031A1E"), centerColor = Color.parseColor("#053C48"), endColor = Color.parseColor("#026E77")),

        AppTheme("color_17", "Sunset Dusk", ThemeType.GRADIENT, "Color",
            drawableRes = R.drawable.gradient_theme_17, swatchRes = R.drawable.swatch_theme_17,
            startColor = Color.parseColor("#2B0938"), centerColor = Color.parseColor("#6B1D52"), endColor = Color.parseColor("#B84343")),

        AppTheme("color_18", "Cherry Plum", ThemeType.GRADIENT, "Color",
            drawableRes = R.drawable.gradient_theme_18, swatchRes = R.drawable.swatch_theme_18,
            startColor = Color.parseColor("#26051D"), centerColor = Color.parseColor("#520C3E"), endColor = Color.parseColor("#871665")),

        AppTheme("color_19", "Amber Gold", ThemeType.GRADIENT, "Color",
            drawableRes = R.drawable.gradient_theme_19, swatchRes = R.drawable.swatch_theme_19,
            startColor = Color.parseColor("#2E1C05"), centerColor = Color.parseColor("#543209"), endColor = Color.parseColor("#8C5814")),

        AppTheme("color_20", "Mystic Lavender", ThemeType.GRADIENT, "Color",
            drawableRes = R.drawable.gradient_theme_20, swatchRes = R.drawable.swatch_theme_20,
            startColor = Color.parseColor("#1E0F38"), centerColor = Color.parseColor("#3D1B68"), endColor = Color.parseColor("#6B32A3")),

        AppTheme("color_21", "Cyberpunk Neon", ThemeType.GRADIENT, "Color",
            drawableRes = R.drawable.gradient_theme_21, swatchRes = R.drawable.swatch_theme_21,
            startColor = Color.parseColor("#260033"), centerColor = Color.parseColor("#52004D"), endColor = Color.parseColor("#004866"))
    )

    fun getNatureThemes(): List<AppTheme> = listOf(
        AppTheme("nature_1", "Mountain Sunrise", ThemeType.IMAGE, "Nature", drawableRes = R.drawable.theme_nature_1, startColor = Color.parseColor("#0B1B3D"), endColor = Color.parseColor("#3D1B4D")),
        AppTheme("nature_2", "Pine Forest Mist", ThemeType.IMAGE, "Nature", drawableRes = R.drawable.theme_nature_2, startColor = Color.parseColor("#071E1A"), endColor = Color.parseColor("#134E4A")),
        AppTheme("nature_3", "Ocean Twilight", ThemeType.IMAGE, "Nature", drawableRes = R.drawable.theme_nature_3, startColor = Color.parseColor("#03071E"), endColor = Color.parseColor("#0A2540")),
        AppTheme("nature_4", "Sakura Blossom", ThemeType.IMAGE, "Nature", drawableRes = R.drawable.theme_nature_4, startColor = Color.parseColor("#2B0927"), endColor = Color.parseColor("#5A1846")),
        AppTheme("nature_5", "Desert Mirage", ThemeType.IMAGE, "Nature", drawableRes = R.drawable.theme_nature_5, startColor = Color.parseColor("#240B04"), endColor = Color.parseColor("#6B2D0C")),
        AppTheme("nature_6", "Aurora Borealis", ThemeType.IMAGE, "Nature", drawableRes = R.drawable.theme_nature_6, startColor = Color.parseColor("#02121A"), endColor = Color.parseColor("#0B424C")),
        AppTheme("nature_7", "Waterfall Glen", ThemeType.IMAGE, "Nature", drawableRes = R.drawable.theme_nature_7, startColor = Color.parseColor("#051923"), endColor = Color.parseColor("#006466")),
        AppTheme("nature_8", "Autumn Woodlands", ThemeType.IMAGE, "Nature", drawableRes = R.drawable.theme_nature_8, startColor = Color.parseColor("#1F0802"), endColor = Color.parseColor("#5E2204")),
        AppTheme("nature_9", "Snowy Peaks", ThemeType.IMAGE, "Nature", drawableRes = R.drawable.theme_nature_9, startColor = Color.parseColor("#0D1B2A"), endColor = Color.parseColor("#1B263B")),
        AppTheme("nature_10", "Starlit Lake", ThemeType.IMAGE, "Nature", drawableRes = R.drawable.theme_nature_10, startColor = Color.parseColor("#020813"), endColor = Color.parseColor("#0B192C")),
        AppTheme("nature_11", "Tropical Palms", ThemeType.IMAGE, "Nature", drawableRes = R.drawable.theme_nature_11, startColor = Color.parseColor("#012A2D"), endColor = Color.parseColor("#08415C")),
        AppTheme("nature_12", "Golden Canyon", ThemeType.IMAGE, "Nature", drawableRes = R.drawable.theme_nature_12, startColor = Color.parseColor("#2E1005"), endColor = Color.parseColor("#7F2E08"))
    )

    fun getPetThemes(): List<AppTheme> = listOf(
        AppTheme("pet_1", "Cute Kitten", ThemeType.IMAGE, "Pet", drawableRes = R.drawable.theme_pet_1, startColor = Color.parseColor("#1A0B2E"), endColor = Color.parseColor("#3E1B68")),
        AppTheme("pet_2", "Golden Pup", ThemeType.IMAGE, "Pet", drawableRes = R.drawable.theme_pet_2, startColor = Color.parseColor("#261404"), endColor = Color.parseColor("#5C3308")),
        AppTheme("pet_3", "Husky Moonlight", ThemeType.IMAGE, "Pet", drawableRes = R.drawable.theme_pet_3, startColor = Color.parseColor("#081226"), endColor = Color.parseColor("#1A3059")),
        AppTheme("pet_4", "British Shorthair", ThemeType.IMAGE, "Pet", drawableRes = R.drawable.theme_pet_4, startColor = Color.parseColor("#111726"), endColor = Color.parseColor("#242E42")),
        AppTheme("pet_5", "White Bunny", ThemeType.IMAGE, "Pet", drawableRes = R.drawable.theme_pet_5, startColor = Color.parseColor("#1F0A24"), endColor = Color.parseColor("#4D1B59")),
        AppTheme("pet_6", "Little Parakeet", ThemeType.IMAGE, "Pet", drawableRes = R.drawable.theme_pet_6, startColor = Color.parseColor("#04261B"), endColor = Color.parseColor("#0D5940")),
        AppTheme("pet_7", "Frenchie Chic", ThemeType.IMAGE, "Pet", drawableRes = R.drawable.theme_pet_7, startColor = Color.parseColor("#1A1A1A"), endColor = Color.parseColor("#333333")),
        AppTheme("pet_8", "Sleepy Kitty", ThemeType.IMAGE, "Pet", drawableRes = R.drawable.theme_pet_8, startColor = Color.parseColor("#140726"), endColor = Color.parseColor("#301357")),
        AppTheme("pet_9", "Arctic Fox", ThemeType.IMAGE, "Pet", drawableRes = R.drawable.theme_pet_9, startColor = Color.parseColor("#0A1D33"), endColor = Color.parseColor("#1D3E66")),
        AppTheme("pet_10", "Panda Bamboo", ThemeType.IMAGE, "Pet", drawableRes = R.drawable.theme_pet_10, startColor = Color.parseColor("#071F14"), endColor = Color.parseColor("#1B4A33")),
        AppTheme("pet_11", "Playful Paws", ThemeType.IMAGE, "Pet", drawableRes = R.drawable.theme_pet_11, startColor = Color.parseColor("#2E081F"), endColor = Color.parseColor("#6E164D")),
        AppTheme("pet_12", "Midnight Cat", ThemeType.IMAGE, "Pet", drawableRes = R.drawable.theme_pet_12, startColor = Color.parseColor("#05050A"), endColor = Color.parseColor("#151226"))
    )

    fun getCartoonThemes(): List<AppTheme> = listOf(
        AppTheme("cartoon_1", "Anime Headphones", ThemeType.IMAGE, "Cartoon", drawableRes = R.drawable.theme_cartoon_1, startColor = Color.parseColor("#12072B"), endColor = Color.parseColor("#3B1360")),
        AppTheme("cartoon_2", "Cyber Samurai", ThemeType.IMAGE, "Cartoon", drawableRes = R.drawable.theme_cartoon_2, startColor = Color.parseColor("#1A052E"), endColor = Color.parseColor("#4D0E75")),
        AppTheme("cartoon_3", "Pixel City 8-bit", ThemeType.IMAGE, "Cartoon", drawableRes = R.drawable.theme_cartoon_3, startColor = Color.parseColor("#0A0017"), endColor = Color.parseColor("#25003E")),
        AppTheme("cartoon_4", "Synthwave Sun", ThemeType.IMAGE, "Cartoon", drawableRes = R.drawable.theme_cartoon_4, startColor = Color.parseColor("#140024"), endColor = Color.parseColor("#47004B")),
        AppTheme("cartoon_5", "Neon Dragon", ThemeType.IMAGE, "Cartoon", drawableRes = R.drawable.theme_cartoon_5, startColor = Color.parseColor("#001B2E"), endColor = Color.parseColor("#00476E")),
        AppTheme("cartoon_6", "Retro Gamer", ThemeType.IMAGE, "Cartoon", drawableRes = R.drawable.theme_cartoon_6, startColor = Color.parseColor("#0E0024"), endColor = Color.parseColor("#2E006A")),
        AppTheme("cartoon_7", "Cyber Idol", ThemeType.IMAGE, "Cartoon", drawableRes = R.drawable.theme_cartoon_7, startColor = Color.parseColor("#24001F"), endColor = Color.parseColor("#5C004F")),
        AppTheme("cartoon_8", "Mecha Guardian", ThemeType.IMAGE, "Cartoon", drawableRes = R.drawable.theme_cartoon_8, startColor = Color.parseColor("#091426"), endColor = Color.parseColor("#17335C")),
        AppTheme("cartoon_9", "Chibi Star", ThemeType.IMAGE, "Cartoon", drawableRes = R.drawable.theme_cartoon_9, startColor = Color.parseColor("#1A0B2E"), endColor = Color.parseColor("#421B73")),
        AppTheme("cartoon_10", "Cloud Castle", ThemeType.IMAGE, "Cartoon", drawableRes = R.drawable.theme_cartoon_10, startColor = Color.parseColor("#180829"), endColor = Color.parseColor("#3F1963")),
        AppTheme("cartoon_11", "Kawaii Cosmic", ThemeType.IMAGE, "Cartoon", drawableRes = R.drawable.theme_cartoon_11, startColor = Color.parseColor("#240320"), endColor = Color.parseColor("#610B55")),
        AppTheme("cartoon_12", "Space Voyager", ThemeType.IMAGE, "Cartoon", drawableRes = R.drawable.theme_cartoon_12, startColor = Color.parseColor("#030B1E"), endColor = Color.parseColor("#0D224D"))
    )

    fun getOtherThemes(): List<AppTheme> = listOf(
        AppTheme("other_1", "Blue Earth Space", ThemeType.IMAGE, "Other", drawableRes = R.drawable.theme_other_1, startColor = Color.parseColor("#01050F"), endColor = Color.parseColor("#06132E")),
        AppTheme("other_2", "Red Supercar", ThemeType.IMAGE, "Other", drawableRes = R.drawable.theme_other_2, startColor = Color.parseColor("#0D0406"), endColor = Color.parseColor("#290A10")),
        AppTheme("other_3", "Cosmic Nebula", ThemeType.IMAGE, "Other", drawableRes = R.drawable.theme_other_3, startColor = Color.parseColor("#0A0314"), endColor = Color.parseColor("#2A0B42")),
        AppTheme("other_4", "Cyber 3D Grid", ThemeType.IMAGE, "Other", drawableRes = R.drawable.theme_other_4, startColor = Color.parseColor("#000D1A"), endColor = Color.parseColor("#002B4D")),
        AppTheme("other_5", "Formula Speed", ThemeType.IMAGE, "Other", drawableRes = R.drawable.theme_other_5, startColor = Color.parseColor("#140404"), endColor = Color.parseColor("#3D0E0E")),
        AppTheme("other_6", "Dark Carbon", ThemeType.IMAGE, "Other", drawableRes = R.drawable.theme_other_6, startColor = Color.parseColor("#080808"), endColor = Color.parseColor("#1C1C1C")),
        AppTheme("other_7", "Liquid Gold", ThemeType.IMAGE, "Other", drawableRes = R.drawable.theme_other_7, startColor = Color.parseColor("#1A1102"), endColor = Color.parseColor("#473007")),
        AppTheme("other_8", "Abstract Aurora", ThemeType.IMAGE, "Other", drawableRes = R.drawable.theme_other_8, startColor = Color.parseColor("#031B1C"), endColor = Color.parseColor("#08474A")),
        AppTheme("other_9", "Prismatic Glass", ThemeType.IMAGE, "Other", drawableRes = R.drawable.theme_other_9, startColor = Color.parseColor("#140826"), endColor = Color.parseColor("#381666")),
        AppTheme("other_10", "Neon Tokyo", ThemeType.IMAGE, "Other", drawableRes = R.drawable.theme_other_10, startColor = Color.parseColor("#1A0026"), endColor = Color.parseColor("#4A0057")),
        AppTheme("other_11", "Deep Void", ThemeType.IMAGE, "Other", drawableRes = R.drawable.theme_other_11, startColor = Color.parseColor("#040408"), endColor = Color.parseColor("#0C0C17")),
        AppTheme("other_12", "Minimalist Geo", ThemeType.IMAGE, "Other", drawableRes = R.drawable.theme_other_12, startColor = Color.parseColor("#0D131A"), endColor = Color.parseColor("#1E2B3A"))
    )

    fun getAllImageThemes(): List<AppTheme> =
        getNatureThemes() + getPetThemes() + getCartoonThemes() + getOtherThemes()

    fun getThemesByCategory(category: String): List<AppTheme> = when (category) {
        "Nature" -> getNatureThemes()
        "Pet" -> getPetThemes()
        "Cartoon" -> getCartoonThemes()
        "Other" -> getOtherThemes()
        else -> getAllImageThemes()
    }

    fun findThemeById(id: String): AppTheme? {
        val all = getColorThemes() + getAllImageThemes()
        return all.firstOrNull { it.id == id }
    }
}
