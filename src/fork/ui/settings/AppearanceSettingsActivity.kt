package desu.inugram.ui.settings

import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import desu.inugram.InuConfig
import desu.inugram.InuHooks
import desu.inugram.SearchRegistry
import desu.inugram.helpers.InuUtils
import desu.inugram.helpers.dialogs.DrawerHelper
import desu.inugram.helpers.icons.IconPackHelper
import desu.inugram.helpers.theme.MonetHelper
import desu.inugram.helpers.theme.SeedScheme
import android.text.InputType
import android.util.TypedValue
import android.widget.EditText
import android.widget.FrameLayout
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.AlertDialog
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.BulletinFactory
import org.telegram.ui.Components.LayoutHelper
import desu.inugram.ui.settings.fonts.FontsSettingsActivity
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import org.telegram.ui.Cells.NotificationsCheckCell
import org.telegram.ui.Cells.TextCheckCell
import org.telegram.ui.Components.UItem
import org.telegram.ui.Components.UniversalAdapter

class AppearanceSettingsActivity : SettingsPageActivity() {

    private var animationSpeedSlider: SliderCell? = null
    private var seedSwatches: SeedSwatchesCell? = null
    private var shapeCell: AvatarShapeCell? = null

    private val material3Group = ExpandableBoolGroup(
        LocaleController.getString(R.string.InuMaterial3),
        listOf(
            ExpandableBoolGroup.Option(R.string.InuMaterial3Switches, InuConfig.MATERIAL3_SWITCHES, TOGGLE_MATERIAL3_SWITCHES),
            ExpandableBoolGroup.Option(R.string.InuMaterial3Sliders, InuConfig.MATERIAL3_SLIDERS, TOGGLE_MATERIAL3_SLIDERS),
            ExpandableBoolGroup.Option(R.string.InuMaterial3Fabs, InuConfig.MATERIAL3_FABS, TOGGLE_MATERIAL3_FABS),
            ExpandableBoolGroup.Option(R.string.InuMaterial3Sections, InuConfig.M3_SECTIONS_STYLE, TOGGLE_M3_SECTIONS_STYLE),
            ExpandableBoolGroup.Option(R.string.InuMaterial3Avatars, InuConfig.MATERIAL3_AVATARS, TOGGLE_MATERIAL3_AVATARS),
            ExpandableBoolGroup.Option(R.string.InuMaterial3BottomTabs, InuConfig.M3_BOTTOM_TABS, TOGGLE_M3_BOTTOM_TABS),
            ExpandableBoolGroup.Option(R.string.InuMaterialProfileActions, InuConfig.MATERIAL_PROFILE_ACTIONS, TOGGLE_MATERIAL_PROFILE_ACTIONS),
            ExpandableBoolGroup.Option(R.string.InuMaterial3NavigationAnimation, InuConfig.M3_NAVIGATION_ANIMATION, TOGGLE_M3_NAVIGATION_ANIMATION),
        ),
    )

    private val classicUiGroup = ExpandableBoolGroup(
        LocaleController.getString(R.string.InuNonIslandUI),
        listOf(
            ExpandableBoolGroup.Option(R.string.InuNonIslandFoldersBar, InuConfig.NON_ISLAND_FOLDERS_BAR, TOGGLE_NON_ISLAND_FOLDERS_BAR),
            ExpandableBoolGroup.Option(R.string.InuNonIslandSharedMediaTabs, InuConfig.NON_ISLAND_SHARED_MEDIA_TABS, TOGGLE_NON_ISLAND_SHARED_MEDIA_TABS),
            ExpandableBoolGroup.Option(R.string.InuNonIslandGlobalSearch, InuConfig.NON_ISLAND_GLOBAL_SEARCH, TOGGLE_NON_ISLAND_GLOBAL_SEARCH),
            ExpandableBoolGroup.Option(R.string.InuNonIslandChatElements, InuConfig.NON_ISLAND_CHAT_ELEMENTS, TOGGLE_NON_ISLAND_CHAT_ELEMENTS),
            ExpandableBoolGroup.Option(R.string.InuNonIslandHeaders, InuConfig.NON_ISLAND_HEADERS, TOGGLE_NON_ISLAND_HEADERS),
            ExpandableBoolGroup.Option(R.string.InuHideFadeView, InuConfig.HIDE_FADE_VIEW, TOGGLE_HIDE_FADE_VIEW),
            ExpandableBoolGroup.Option(R.string.InuDisableGlassGlare, InuConfig.DISABLE_GLASS_GLARE, TOGGLE_DISABLE_GLASS_GLARE),
        ),
    )

    override val expandableGroups = listOf(material3Group, classicUiGroup)

    override fun getTitle(): CharSequence = LocaleController.getString(R.string.InuLookAndFeel)

    override fun onResume() {
        super.onResume()
        listView?.adapter?.update(true)
    }


    override fun fillItems(items: ArrayList<UItem>, adapter: UniversalAdapter) {
        addColorSection(items)

        items.add(UItem.asHeader(LocaleController.getString(R.string.InuTypographyAndIcons)))
        items.add(mkSubPageButton(BUTTON_FONTS, LocaleController.getString(R.string.InuFonts)))
        items.add(
            UItem.asButton(
                BUTTON_ICON_REPLACEMENT,
                LocaleController.getString(R.string.InuIconReplacement),
                IconPackHelper.getActivePacksSummary(),
            )
        )
        items.add(
            UItem.asButton(
                BUTTON_NOTIFICATION_ICON,
                LocaleController.getString(R.string.InuNotificationIcon),
                when (InuConfig.NOTIFICATION_ICON.value) {
                    InuConfig.NotificationIconItem.INUGRAM -> LocaleController.getString(R.string.InuNotificationIconInugram)
                    else -> LocaleController.getString(R.string.InuNotificationIconTelegram)
                }
            )
        )
        items.add(UItem.asShadow(null))

        items.add(UItem.asHeader(LocaleController.getString(R.string.InuNavigation)))
        items.add(
            mkTwoLineCheckItem(
                TOGGLE_NAVIGATION_DRAWER,
                R.string.InuNavigationDrawer,
                R.string.InuNavigationDrawerInfo,
                InuConfig.NAVIGATION_DRAWER.value,
            )
        )
        if (InuConfig.NAVIGATION_DRAWER.value) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                items.add(
                    mkTwoLineCheckItem(
                        TOGGLE_DRAWER_BACK_GESTURE,
                        R.string.InuDrawerBackGesture,
                        R.string.InuDrawerBackGestureInfo,
                        InuConfig.DRAWER_BACK_GESTURE.value,
                    )
                )
            }
            items.add(mkSubPageButton(BUTTON_DRAWER_MENU_ORDER, LocaleController.getString(R.string.InuDrawerMenuOrder)))
            items.add(UItem.asShadow(null))
        }
        items.add(
            UItem.asCheck(
                TOGGLE_BOTTOM_TABS,
                LocaleController.getString(R.string.InuBottomTabs),
            ).setChecked(!InuConfig.BOTTOM_TABS_HIDE.value)
        )
        if (!InuConfig.BOTTOM_TABS_HIDE.value) {
            items.add(
                UItem.asCheck(
                    TOGGLE_HIDE_CONTACTS_TAB,
                    LocaleController.getString(R.string.InuHideContactsTab),
                ).setChecked(InuConfig.BOTTOM_TABS_HIDE_CONTACTS.value)
            )
            items.add(
                mkTwoLineCheckItem(
                    TOGGLE_COMPACT_MODE,
                    R.string.InuCompactMode,
                    R.string.InuCompactModeInfo,
                    InuConfig.BOTTOM_TABS_COMPACT_MODE.value,
                )
            )
            items.add(
                UItem.asCheck(
                    TOGGLE_BOTTOM_TABS_HIDE_ON_SCROLL,
                    LocaleController.getString(R.string.InuBottomTabsHideOnScroll),
                ).setChecked(InuConfig.BOTTOM_TABS_HIDE_ON_SCROLL.value)
            )
        }
        items.add(UItem.asShadow(null))

        items.add(UItem.asHeader(LocaleController.getString(R.string.InuInterfaceStyle)))
        material3Group.addTo(items, ::applyStyleChanges)
        classicUiGroup.addTo(items, ::applyStyleChanges)
        items.add(
            mkTwoLineCheckItem(
                TOGGLE_DISABLE_SCRIM_BLUR,
                R.string.InuDisableScrimBlur,
                R.string.InuDisableScrimBlurInfo,
                InuConfig.DISABLE_SCRIM_BLUR.value
            )
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            items.add(
                UItem.asButton(
                    BUTTON_PREDICTIVE_BACK_MODE,
                    LocaleController.getString(R.string.InuPredictiveBack),
                    predictiveBackModeLabel(InuConfig.PREDICTIVE_BACK_MODE.value),
                )
            )
        }
        items.add(UItem.asShadow(LocaleController.getString(R.string.InuNonIslandHint)))

        if (animationSpeedSlider == null) animationSpeedSlider = SliderCell(
            this.context, min = 0.5f, max = 3f,
            defaultValue = InuConfig.ANIMATION_SPEED.default,
            initialValue = if (InuConfig.ANIMATION_SPEED.value >= 3f) 3f else InuConfig.ANIMATION_SPEED.value,
            step = 0.05f,
            title = LocaleController.getString(R.string.InuAnimationSpeed),
            format = {
                if (it >= 3f) LocaleController.getString(R.string.InuAnimationSpeedInstant)
                else String.format("%.2fx", it)
            },
            onChanged = {
                InuConfig.ANIMATION_SPEED.value = if (it >= 3f) 9999f else it
                InuHooks.syncAnimationSpeed()
            },
        )

        items.add(UItem.asHeader(LocaleController.getString(R.string.InuMotion)))
        items.add(
            mkTwoLineCheckItem(
                TOGGLE_REDUCE_MENU_MOTION,
                R.string.InuReduceMenuMotion,
                R.string.InuReduceMenuMotionInfo,
                InuConfig.REDUCE_MENU_MOTION.value
            )
        )
        items.add(UItem.asCustom(animationSpeedSlider))
        items.add(UItem.asShadow(LocaleController.getString(R.string.InuAnimationSpeedInfo)))
    }

    private fun addColorSection(items: ArrayList<UItem>) {
        items.add(UItem.asHeader(LocaleController.getString(R.string.QwColorHeader)))
        items.add(
            UItem.asButton(
                BUTTON_MONET_THEME,
                LocaleController.getString(R.string.InuMonetTheme),
                monetThemeModeLabel(MonetHelper.getThemeMode()),
            )
        )
        val swatches = seedSwatches ?: SeedSwatchesCell(
            context,
            onPick = { seed -> setSeed(seed) },
            onCustom = { showCustomColorDialog() },
        ).also { seedSwatches = it }
        items.add(UItem.asCustom(swatches))
        if (MonetHelper.isSeedMode()) {
            items.add(UItem.asButton(BUTTON_M3_VARIANT, LocaleController.getString(R.string.QwPalette), variantLabel(InuConfig.M3_SEED_VARIANT.value)))
            items.add(UItem.asButton(BUTTON_M3_CONTRAST, LocaleController.getString(R.string.QwContrast), contrastLabel(InuConfig.M3_SEED_CONTRAST.value)))
        }
        items.add(UItem.asShadow(LocaleController.getString(R.string.QwColorInfo)))

        items.add(UItem.asHeader(LocaleController.getString(R.string.QwAvatarShape)))
        val shapes = shapeCell ?: AvatarShapeCell(context) { key ->
            InuConfig.AVATAR_SHAPE.value = key
            softRebuild()
        }.also { shapeCell = it }
        items.add(UItem.asCustom(shapes))
        items.add(UItem.asShadow(LocaleController.getString(R.string.QwAvatarShapeInfo)))
    }

    private fun setSeed(seed: Int) {
        if (InuConfig.M3_SEED_COLOR.value == seed && MonetHelper.getThemeMode() != MonetHelper.ThemeMode.DISABLED) return
        InuConfig.M3_SEED_COLOR.value = seed
        seedSwatches?.sync()
        MonetHelper.onSeedSettingsChanged()
        listView?.adapter?.update(true)
    }

    private fun showCustomColorDialog() {
        val ctx = parentActivity ?: return
        val input = EditText(ctx).apply {
            setText(String.format("#%06X", MonetHelper.effectiveSeed() and 0xFFFFFF))
            setSelection(text.length)
            hint = LocaleController.getString(R.string.QwCustomColorHint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setSingleLine(true)
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18f)
            setTextColor(Theme.getColor(Theme.key_dialogTextBlack))
            setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText))
        }
        val frame = FrameLayout(ctx)
        frame.addView(input, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT.toFloat(), 0, 24f, 4f, 24f, 0f))
        val dialog = AlertDialog.Builder(ctx)
            .setTitle(LocaleController.getString(R.string.QwCustomColor))
            .setView(frame)
            .setPositiveButton(LocaleController.getString(R.string.OK)) { _, _ ->
                val color = parseHexColor(input.text.toString())
                if (color == null) {
                    BulletinFactory.of(this).createErrorBulletin(LocaleController.getString(R.string.QwBadColor)).show()
                } else {
                    setSeed(color)
                }
            }
            .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
            .create()
        showDialog(dialog)
        AndroidUtilities.runOnUIThread({ input.requestFocus(); AndroidUtilities.showKeyboard(input) }, 150)
    }

    private fun applyStyleChanges(changed: List<ExpandableBoolGroup.Option>) {
        val configs = changed.map { it.config }
        if (InuConfig.MATERIAL3_SWITCHES in configs || InuConfig.MATERIAL3_SLIDERS in configs) invalidateVisibleRows()
        if (InuConfig.NON_ISLAND_HEADERS in configs) rebuildAll()
        else if (configs.any { it in SOFT_REBUILD_CONFIGS }) softRebuild()
        // if (InuConfig.M3_SECTIONS_STYLE in configs) inu_rebuildSelf()
        if (InuConfig.NON_ISLAND_CHAT_ELEMENTS in configs) InuHooks.syncChatInputRowHeight()
        listView.adapter.update(true)
    }

    override fun onClick(item: UItem, view: View, position: Int, x: Float, y: Float) {
        if (material3Group.handleClick(item, view) { applyStyleChanges(listOfNotNull(it)) }) return
        if (classicUiGroup.handleClick(item, view) { applyStyleChanges(listOfNotNull(it)) }) return
        when (item.id) {
            BUTTON_ICON_REPLACEMENT -> presentFragment(IconPacksActivity())

            BUTTON_NOTIFICATION_ICON -> RadioItemOptions.show(
                this, view,
                listOf(
                    LocaleController.getString(R.string.InuNotificationIconTelegram),
                    LocaleController.getString(R.string.InuNotificationIconInugram),
                ),
                InuConfig.NOTIFICATION_ICON.value,
            ) { which ->
                InuConfig.NOTIFICATION_ICON.value = which
            }

            BUTTON_FONTS -> presentFragment(FontsSettingsActivity())

            BUTTON_DRAWER_MENU_ORDER -> presentFragment(DrawerMenuOrderActivity())

            TOGGLE_DISABLE_SCRIM_BLUR -> {
                val new = InuConfig.DISABLE_SCRIM_BLUR.toggle()
                (view as? NotificationsCheckCell)?.isChecked = new
            }

            TOGGLE_REDUCE_MENU_MOTION -> {
                val new = InuConfig.REDUCE_MENU_MOTION.toggle()
                (view as? NotificationsCheckCell)?.isChecked = new
            }

            TOGGLE_NAVIGATION_DRAWER -> {
                val new = InuConfig.NAVIGATION_DRAWER.toggle()
                (view as? NotificationsCheckCell)?.isChecked = new
                listView.adapter.update(true)
                if (new) DrawerHelper.ensureSetup(parentLayout) else DrawerHelper.teardown(parentLayout)
                softRebuild()
                if (new && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && InuConfig.DRAWER_BACK_GESTURE.value) {
                    showRestartBulletin()
                }
            }

            TOGGLE_DRAWER_BACK_GESTURE -> {
                val new = InuConfig.DRAWER_BACK_GESTURE.toggle()
                (view as? NotificationsCheckCell)?.isChecked = new
                showRestartBulletin()
            }

            TOGGLE_BOTTOM_TABS -> {
                val new = !InuConfig.BOTTOM_TABS_HIDE.toggle()
                (view as? TextCheckCell)?.isChecked = new
                listView.adapter.update(true)
                softRebuild()
            }

            TOGGLE_HIDE_CONTACTS_TAB -> {
                val new = InuConfig.BOTTOM_TABS_HIDE_CONTACTS.toggle()
                (view as? TextCheckCell)?.isChecked = new
                showRestartBulletin()
            }

            TOGGLE_BOTTOM_TABS_HIDE_ON_SCROLL -> {
                val new = InuConfig.BOTTOM_TABS_HIDE_ON_SCROLL.toggle()
                (view as? TextCheckCell)?.isChecked = new
                softRebuild()
            }

            TOGGLE_COMPACT_MODE -> {
                val new = InuConfig.BOTTOM_TABS_COMPACT_MODE.toggle()
                (view as? NotificationsCheckCell)?.isChecked = new
                softRebuild()
            }

            BUTTON_M3_VARIANT -> RadioItemOptions.show(
                this, view,
                VARIANT_LABELS.map { LocaleController.getString(it) },
                InuConfig.M3_SEED_VARIANT.value.coerceIn(0, VARIANT_LABELS.size - 1),
            ) { which ->
                InuConfig.M3_SEED_VARIANT.value = which
                MonetHelper.onSeedSettingsChanged()
            }

            BUTTON_M3_CONTRAST -> RadioItemOptions.show(
                this, view,
                CONTRAST_LABELS.map { LocaleController.getString(it) },
                InuConfig.M3_SEED_CONTRAST.value.coerceIn(0, CONTRAST_LABELS.size - 1),
            ) { which ->
                InuConfig.M3_SEED_CONTRAST.value = which
                MonetHelper.onSeedSettingsChanged()
            }

            BUTTON_MONET_THEME -> RadioItemOptions.show(
                this, view,
                listOf(
                    LocaleController.getString(R.string.InuMonetThemeDisabled),
                    LocaleController.getString(R.string.InuMonetThemeLight),
                    LocaleController.getString(R.string.InuMonetThemeDark),
                    LocaleController.getString(R.string.InuMonetThemeAmoled),
                    LocaleController.getString(R.string.InuMonetThemeAuto),
                    LocaleController.getString(R.string.InuMonetThemeAutoAmoled),
                ),
                MonetHelper.getThemeMode().ordinal,
            ) { which ->
                MonetHelper.setThemeMode(MonetHelper.ThemeMode.entries[which])
            }

            BUTTON_PREDICTIVE_BACK_MODE -> RadioItemOptions.show(
                this, view,
                listOf(
                    LocaleController.getString(R.string.InuPredictiveBackOff),
                    LocaleController.getString(R.string.InuPredictiveBackStock),
                    LocaleController.getString(R.string.InuPredictiveBackMaterial3),
                ),
                InuConfig.PREDICTIVE_BACK_MODE.value,
            ) { which ->
                if (InuConfig.PREDICTIVE_BACK_MODE.value == which) return@show
                InuConfig.PREDICTIVE_BACK_MODE.value = which
                showRestartBulletin()
            }
        }
    }

    companion object {
        private val TOGGLE_HIDE_FADE_VIEW = InuUtils.generateId()
        private val TOGGLE_NAVIGATION_DRAWER = InuUtils.generateId()
        private val TOGGLE_DRAWER_BACK_GESTURE = InuUtils.generateId()
        private val BUTTON_DRAWER_MENU_ORDER = InuUtils.generateId()
        private val TOGGLE_BOTTOM_TABS = InuUtils.generateId()
        private val TOGGLE_HIDE_CONTACTS_TAB = InuUtils.generateId()
        private val TOGGLE_COMPACT_MODE = InuUtils.generateId()
        private val TOGGLE_BOTTOM_TABS_HIDE_ON_SCROLL = InuUtils.generateId()
        private val TOGGLE_NON_ISLAND_FOLDERS_BAR = InuUtils.generateId()
        private val TOGGLE_NON_ISLAND_SHARED_MEDIA_TABS = InuUtils.generateId()
        private val TOGGLE_NON_ISLAND_GLOBAL_SEARCH = InuUtils.generateId()
        private val TOGGLE_NON_ISLAND_CHAT_ELEMENTS = InuUtils.generateId()
        private val TOGGLE_NON_ISLAND_HEADERS = InuUtils.generateId()
        private val BUTTON_FONTS = InuUtils.generateId()
        private val TOGGLE_DISABLE_SCRIM_BLUR = InuUtils.generateId()
        private val TOGGLE_DISABLE_GLASS_GLARE = InuUtils.generateId()
        private val TOGGLE_REDUCE_MENU_MOTION = InuUtils.generateId()
        private val TOGGLE_MATERIAL3_SWITCHES = InuUtils.generateId()
        private val TOGGLE_MATERIAL3_SLIDERS = InuUtils.generateId()
        private val TOGGLE_MATERIAL3_FABS = InuUtils.generateId()
        private val TOGGLE_M3_SECTIONS_STYLE = InuUtils.generateId()
        private val TOGGLE_MATERIAL3_AVATARS = InuUtils.generateId()
        private val TOGGLE_M3_BOTTOM_TABS = InuUtils.generateId()
        private val TOGGLE_MATERIAL_PROFILE_ACTIONS = InuUtils.generateId()
        private val TOGGLE_M3_NAVIGATION_ANIMATION = InuUtils.generateId()
        private val BUTTON_ICON_REPLACEMENT = InuUtils.generateId()
        private val BUTTON_NOTIFICATION_ICON = InuUtils.generateId()
        private val BUTTON_PREDICTIVE_BACK_MODE = InuUtils.generateId()
        private val BUTTON_MONET_THEME = InuUtils.generateId()
        private val BUTTON_M3_VARIANT = InuUtils.generateId()
        private val BUTTON_M3_CONTRAST = InuUtils.generateId()

        private val VARIANT_LABELS = listOf(
            R.string.QwPaletteTonal, R.string.QwPaletteVibrant, R.string.QwPaletteExpressive,
            R.string.QwPaletteNeutral, R.string.QwPaletteMono,
        )
        private val CONTRAST_LABELS = listOf(R.string.QwContrastStandard, R.string.QwContrastMedium, R.string.QwContrastHigh)

        private fun variantLabel(v: Int): String = LocaleController.getString(VARIANT_LABELS[v.coerceIn(0, VARIANT_LABELS.size - 1)])
        private fun contrastLabel(v: Int): String = LocaleController.getString(CONTRAST_LABELS[v.coerceIn(0, CONTRAST_LABELS.size - 1)])

        /** "#3D6CB5", "3d6cb5" or "#36C" → opaque ARGB, or null */
        @JvmStatic
        fun parseHexColor(raw: String): Int? {
            var h = raw.trim().removePrefix("#")
            if (h.length == 3) h = h.map { "$it$it" }.joinToString("")
            if (h.length != 6 || !h.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null
            return (0xFF000000.toInt() or h.toInt(16))
        }

        private val SOFT_REBUILD_CONFIGS = setOf(
            InuConfig.MATERIAL3_SWITCHES,
            InuConfig.MATERIAL3_FABS,
            InuConfig.M3_SECTIONS_STYLE,
            InuConfig.M3_BOTTOM_TABS,
            InuConfig.MATERIAL_PROFILE_ACTIONS,
            InuConfig.NON_ISLAND_FOLDERS_BAR,
            InuConfig.NON_ISLAND_GLOBAL_SEARCH,
            InuConfig.HIDE_FADE_VIEW,
        )

        private fun monetThemeModeLabel(mode: MonetHelper.ThemeMode): String = when (mode) {
            MonetHelper.ThemeMode.LIGHT -> LocaleController.getString(R.string.InuMonetThemeLight)
            MonetHelper.ThemeMode.DARK -> LocaleController.getString(R.string.InuMonetThemeDark)
            MonetHelper.ThemeMode.AMOLED -> LocaleController.getString(R.string.InuMonetThemeAmoled)
            MonetHelper.ThemeMode.AUTO -> LocaleController.getString(R.string.InuMonetThemeAuto)
            MonetHelper.ThemeMode.AUTO_AMOLED -> LocaleController.getString(R.string.InuMonetThemeAutoAmoled)
            else -> LocaleController.getString(R.string.InuMonetThemeDisabled)
        }

        private fun predictiveBackModeLabel(value: Int): String = when (value) {
            InuConfig.PredictiveBackModeItem.OFF -> LocaleController.getString(R.string.InuPredictiveBackOff)
            InuConfig.PredictiveBackModeItem.STOCK -> LocaleController.getString(R.string.InuPredictiveBackStock)
            else -> LocaleController.getString(R.string.InuPredictiveBackMaterial3)
        }

        @JvmField
        val PAGE = SearchRegistry.Page(
            slug = "appearance",
            titleRes = R.string.InuLookAndFeel,
            iconRes = R.drawable.msg_settings_old,
            factory = ::AppearanceSettingsActivity,
            entries = listOf(
                SearchRegistry.Entry("disable-scrim-blur", R.string.InuDisableScrimBlur, TOGGLE_DISABLE_SCRIM_BLUR),
                SearchRegistry.Entry("disable-glass-glare", R.string.InuDisableGlassGlare, TOGGLE_DISABLE_GLASS_GLARE),
                SearchRegistry.Entry("reduce-menu-motion", R.string.InuReduceMenuMotion, TOGGLE_REDUCE_MENU_MOTION),
                SearchRegistry.Entry("material3-switches", R.string.InuMaterial3Switches, TOGGLE_MATERIAL3_SWITCHES),
                SearchRegistry.Entry("material3-sliders", R.string.InuMaterial3Sliders, TOGGLE_MATERIAL3_SLIDERS),
                SearchRegistry.Entry("material3-fabs", R.string.InuMaterial3Fabs, TOGGLE_MATERIAL3_FABS),
                SearchRegistry.Entry("material3-sections", R.string.InuMaterial3Sections, TOGGLE_M3_SECTIONS_STYLE),
                SearchRegistry.Entry("material3-avatars", R.string.InuMaterial3Avatars, TOGGLE_MATERIAL3_AVATARS),
                SearchRegistry.Entry("m3-bottom-tabs", R.string.InuMaterial3BottomTabs, TOGGLE_M3_BOTTOM_TABS),
                SearchRegistry.Entry("material-profile-actions", R.string.InuMaterialProfileActions, TOGGLE_MATERIAL_PROFILE_ACTIONS),
                SearchRegistry.Entry("material3-navigation-animation", R.string.InuMaterial3NavigationAnimation, TOGGLE_M3_NAVIGATION_ANIMATION),
                SearchRegistry.Entry("monet-theme", R.string.InuMonetTheme, BUTTON_MONET_THEME),
                SearchRegistry.Entry("m3-palette", R.string.QwPalette, BUTTON_M3_VARIANT),
                SearchRegistry.Entry("m3-contrast", R.string.QwContrast, BUTTON_M3_CONTRAST),
                SearchRegistry.Entry("icon-replacement", R.string.InuIconReplacement, BUTTON_ICON_REPLACEMENT),
                SearchRegistry.Entry("notification-icon", R.string.InuNotificationIcon, BUTTON_NOTIFICATION_ICON),
                SearchRegistry.Entry("font", R.string.InuFonts, BUTTON_FONTS),
                SearchRegistry.Entry("predictive-back-mode", R.string.InuPredictiveBack, BUTTON_PREDICTIVE_BACK_MODE),
                SearchRegistry.Entry("navigation-drawer", R.string.InuNavigationDrawer, TOGGLE_NAVIGATION_DRAWER),
                SearchRegistry.Entry("drawer-back-gesture", R.string.InuDrawerBackGesture, TOGGLE_DRAWER_BACK_GESTURE),
                SearchRegistry.Entry("drawer-menu-order", R.string.InuDrawerMenuOrder, BUTTON_DRAWER_MENU_ORDER),
                SearchRegistry.Entry("bottom-tabs-hide", R.string.InuBottomTabs, TOGGLE_BOTTOM_TABS),
                SearchRegistry.Entry("hide-contacts-tab", R.string.InuHideContactsTab, TOGGLE_HIDE_CONTACTS_TAB),
                SearchRegistry.Entry("bottom-tabs-compact-mode", R.string.InuCompactMode, TOGGLE_COMPACT_MODE),
                SearchRegistry.Entry("bottom-tabs-hide-on-scroll", R.string.InuBottomTabsHideOnScroll, TOGGLE_BOTTOM_TABS_HIDE_ON_SCROLL),
                SearchRegistry.Entry("non-island-folders-bar", R.string.InuNonIslandFoldersBar, TOGGLE_NON_ISLAND_FOLDERS_BAR),
                SearchRegistry.Entry("non-island-shared-media-tabs", R.string.InuNonIslandSharedMediaTabs, TOGGLE_NON_ISLAND_SHARED_MEDIA_TABS),
                SearchRegistry.Entry("non-island-global-search", R.string.InuNonIslandGlobalSearch, TOGGLE_NON_ISLAND_GLOBAL_SEARCH),
                SearchRegistry.Entry("non-island-chat-elements", R.string.InuNonIslandChatElements, TOGGLE_NON_ISLAND_CHAT_ELEMENTS),
                SearchRegistry.Entry("hide-fade-view", R.string.InuHideFadeView, TOGGLE_HIDE_FADE_VIEW),
            ),
        )
    }
}
