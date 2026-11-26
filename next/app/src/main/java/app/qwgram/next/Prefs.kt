package app.qwgram.next

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlin.reflect.KProperty

/** Persistent interface settings, observable from Compose. */
object Prefs {
    private lateinit var sp: SharedPreferences

    fun init(context: Context) {
        sp = context.getSharedPreferences("qwgram", Context.MODE_PRIVATE)
        all.forEach { it.load() }
    }

    private val all = mutableListOf<Item<*>>()

    /** where each chat was left: "<messageId>:<offsetPx>" (not part of the settings) */
    /** own wallpaper for one chat: a file path, or null */
    fun chatWallpaper(chatId: Long): String? = sp.getString("wp_$chatId", null)?.takeIf { java.io.File(it).exists() }
    fun setChatWallpaper(chatId: Long, path: String?) {
        sp.edit().apply { if (path == null) remove("wp_$chatId") else putString("wp_$chatId", path) }.apply()
        chatWallpaperVersion.intValue++
    }
    val chatWallpaperVersion = androidx.compose.runtime.mutableIntStateOf(0)

    fun chatPos(chatId: Long): Pair<Long, Int>? = sp.getString("pos_$chatId", null)?.split(':')?.let { p ->
        val id = p.getOrNull(0)?.toLongOrNull() ?: return null
        id to (p.getOrNull(1)?.toIntOrNull() ?: 0)
    }
    fun setChatPos(chatId: Long, pos: Pair<Long, Int>?) {
        sp.edit().apply { if (pos == null) remove("pos_$chatId") else putString("pos_$chatId", "${pos.first}:${pos.second}") }.apply()
    }

    sealed class Item<T>(val key: String, val def: T) {
        val state: MutableState<T> = mutableStateOf(def)
        var value: T
            get() = state.value
            set(v) {
                state.value = v
                save(v)
            }

        operator fun getValue(thisRef: Any?, property: KProperty<*>): T = state.value
        operator fun setValue(thisRef: Any?, property: KProperty<*>, v: T) {
            value = v
        }

        abstract fun load()
        protected abstract fun save(v: T)
        fun reset() {
            value = def
        }

        init {
            @Suppress("LeakingThis")
            all.add(this)
        }
    }

    class B(key: String, def: Boolean) : Item<Boolean>(key, def) {
        override fun load() { state.value = sp.getBoolean(key, def) }
        override fun save(v: Boolean) { sp.edit().putBoolean(key, v).apply() }
    }

    class I(key: String, def: Int) : Item<Int>(key, def) {
        override fun load() { state.value = sp.getInt(key, def) }
        override fun save(v: Int) { sp.edit().putInt(key, v).apply() }
    }

    class F(key: String, def: Float) : Item<Float>(key, def) {
        override fun load() { state.value = sp.getFloat(key, def) }
        override fun save(v: Float) { sp.edit().putFloat(key, v).apply() }
    }

    class S(key: String, def: String) : Item<String>(key, def) {
        override fun load() { state.value = sp.getString(key, def) ?: def }
        override fun save(v: String) { sp.edit().putString(key, v).apply() }
    }

    // ---- theme & colour ----
    val themeMode = S("theme_mode", "system")          // system | light | dark
    val amoled = B("amoled", false)
    val dynamicColor = B("dynamic_color", true)        // Android 12+ wallpaper colours
    val seed = I("seed", 0xFF6750A4.toInt())
    val paletteStyle = I("palette_style", 0)           // SeedScheme.VARIANT_*
    val contrast = I("contrast", 0)

    // ---- shapes ----
    val avatarShape = S("avatar_shape", "circle")
    val uiRound = I("ui_round", 1)                     // legacy, superseded by corner_scale
    val cornerScale = F("corner_scale", 1f)             // global corner radius multiplier 0…2 (buttons, panels, headers)

    // ---- chat list ----
    val listStyle = S("list_style", "flat")             // flat | cards
    val density = I("density", 1)                       // 0 compact, 1 normal, 2 airy
    val previewLines = I("preview_lines", 1)
    val separators = B("separators", false)
    val badgeStyle = S("badge_style", "filled")         // filled | tonal | dot
    val showArchiveRow = B("archive_row", true)
    val avatarSize = I("avatar_size", 54)
    val topBar = S("top_bar", "search")                  // title | search
    val titleText = S("title_text", "qwgram")           // qwgram | name | chats | folder

    // ---- folders ----
    val folderStyle = S("folder_style", "group")        // group | tabs | chips
    val folderCounters = B("folder_counters", true)
    val folderSwipe = B("folder_swipe", true)
    val hideAllChats = B("hide_all_chats", false)

    // ---- messages ----
    val bubbleStyle = S("bubble_style", "tonal")        // tonal | filled | outlined
    val bubbleRadius = I("bubble_radius", 18)
    val textSize = I("text_size", 16)
    val bubbleTail = B("bubble_tail", true)
    val bubbleWidth = I("bubble_width", 82)
    val wallpaper = S("wallpaper", "shapes")            // plain | tonal | shapes | dots | image
    val wallpaperImage = S("wallpaper_image", "")         // own photo (copied into app files)
    val wallpaperEverywhere = B("wallpaper_everywhere", false) // the photo behind every screen, not only chats
    val wallpaperDim = F("wallpaper_dim", 0.35f)          // scrim over the photo (readability)
    val tgWallpapers = B("tg_wallpapers", true)           // show wallpapers set in Telegram for a chat
    val messageEnter = S("message_enter", "spring")     // spring | fade | none
    val bigEmoji = B("big_emoji", true)
    val groupAvatars = B("group_avatars", true)
    val doubleTap = S("double_tap", "react")            // react | reply | none
    val doubleTapEmoji = S("double_tap_emoji", "❤️")
    val sendByEnter = B("send_by_enter", false)
    val swipeReply = B("swipe_reply", true)
    val seconds = B("seconds", false)

    // ---- navigation ----
    val navStyle = S("nav_style", "floating")           // standard | floating
    val navLabels = B("nav_labels", true)
    val floatingBars = B("floating_bars", true)
    val hideKbOnScroll = B("hide_kb_scroll", true)        // scrolling a chat hides the keyboard
    val forwardNoAuthor = B("forward_no_author", false)   // forward as a copy by default
    val fabAction = S("fab_action", "compose")            // compose (menu: message/group/channel) | search
    val centerTitle = B("center_title", false)            // centred titles in every header
    val emojiStyle = S("emoji_style", "twemoji")          // twemoji | noto (bundled Google emoji) | system
    val pressMorph = S("press_morph", "normal")          // off | soft | normal | strong
    val outBubble = S("out_bubble", "primary")            // primary | secondary | tertiary
    val fabShape = S("fab_shape", "cookie")               // cookie | sunny | circle | square
    val screenAnim = S("screen_anim", "slide")            // slide | scale | fade
    val archiveHidden = B("archive_hidden", false)
    val ghostOffline = B("ghost_offline", false)          // never appear online
    val ghostTyping = B("ghost_typing", false)            // don't send "typing…"
    val keepDeleted = B("keep_deleted", false)            // keep messages others deleted (this session)
    val hideReactions = B("hide_reactions", false)
    val charCounter = B("char_counter", false)
    val markdown = B("markdown", true)                 // **жирный**, __курсив__, `код`, ||спойлер|| when sending
    val showIds = B("show_ids", true)
    val confirmSticker = B("confirm_sticker", false)
    val kbHeight = I("kb_height", 0)                       // last keyboard height (px), the emoji panel uses it           // floating composer & chat header
    val listSwipe = S("list_swipe", "folders")            // folders | actions (archive / read)
    val autoDlMobile = B("autodl_mobile", true)
    val autoDlWifi = B("autodl_wifi", true)
    val fabStyle = S("fab_style", "extended")           // extended | icon | none
    val showContactsTab = B("contacts_tab", true)
    val drawer = B("drawer", true)

    // ---- motion & text ----
    val motion = S("motion", "expressive")             // expressive | standard | off
    val motionSpeed = F("motion_speed", 1f)
    val predictiveBack = B("predictive_back", true)
    val uiScale = F("ui_scale", 1f)
    val font = S("font", "google")                      // google (Google Sans Flex) | system | serif | mono
    val bodyWeight = I("body_weight", 500)                // 300…700, body text (any font; variable fonts are exact)
    val headWeight = I("head_weight_v", 800)              // 400…900, headers and titles
    val haptics = B("haptics", true)

    // ---- client features ----
    val hidePhone = B("hide_phone", false)
    val stripTracking = B("strip_tracking", true)
    val silentByDefault = B("silent_default", false)
    val confirmDelete = B("confirm_delete", true)
    val keepAlive = B("keep_alive", true)
    val notifyPreview = B("notify_preview", true)
    val showOnlineDot = B("online_dot", true)
    val markReadOnOpen = B("mark_read", true)

    // ---- background blur (glass) ----
    val blurOn = B("blur_on", false)                   // master switch: off until the user turns it on
    val blurRadius = I("blur_radius", 32)              // dp
    val blurTint = F("blur_tint", 0.62f)               // how much of the panel colour lies over the blur
    val blurTone = S("blur_tone", "surface")           // surface | container | accent | dark | light | none
    val blurNoise = F("blur_noise", 0.04f)             // film grain over the glass
    val blurQuality = S("blur_quality", "balanced")    // speed | balanced | best
    val blurFade = B("blur_fade", true)                // progressive (gradient) blur on headers / bottom bars
    val blurFadeLen = F("blur_fade_len2", 1.4f)        // how far the transition reaches, × bar height (past the bar when > 1)
    val blurCurve = S("blur_curve", "soft")            // soft | linear | sharp
    val blurEdge = B("blur_edge", true)                // a hairline light edge on glass buttons
    val blurHeaders = B("blur_headers", true)
    val blurBars = B("blur_bars", true)                // bottom panel, FAB, floating buttons
    val blurComposer = B("blur_composer", true)
    val blurChips = B("blur_chips", true)              // dates and service plates over chat wallpaper
    val blurBubbles = B("blur_bubbles", false)         // frosted message bubbles over the wallpaper
    val blurBubbleAlpha = F("blur_bubble_alpha", 0.72f)
    val blurMenus = B("blur_menus", true)              // the chat behind a message menu
    val blurWindows = B("blur_windows", true)          // behind sheets, dialogs and popups (Android 12+)
    val blurWindowRadius = I("blur_window_radius", 24) // dp
    // separate looks for the two kinds of glass
    val blurFadeBlur = B("blur_fade_blur", true)       // headers / bottom fades are blurred (off: a plain colour fade)
    val blurFadeRadius = I("blur_fade_radius", 32)
    val blurFadeTint = F("blur_fade_tint", 0.62f)
    val blurBtnRadius = I("blur_btn_radius", 24)       // buttons, pills, composer
    val blurBtnTint = F("blur_btn_tint", 0.55f)
    val blurBtnNoise = F("blur_btn_noise", 0.04f)
    val blurChipsFolders = B("blur_folders", true)     // folder chips over the chat list
    val fadeOnScroll = B("fade_on_scroll", true)
    val profilePattern = B("profile_pattern", true)
    val profileGifts = B("profile_gifts", true)
    val profileMusic = B("profile_music", true)   // emoji pattern around the profile photo
    val titleSize = S("title_size", "normal")          // small | normal | large — the home header title
    val sendAnim = S("send_anim", "fly")               // fly (from the field, Telegram) | simple
    val tabAnim = S("tab_anim", "fade")                // fade | slide | none — switching bottom tabs
    val folderCascade = F("folder_cascade", 1f)        // strength of the rows' trailing wave on folder swipes
    val cascadeBounce = F("cascade_bounce", 0.55f)      // 0 = no overshoot … 1 = very springy
    val cascadeSpeed = F("cascade_speed", 1f)          // how fast the rows catch up
    val cascadeSpread = F("cascade_spread", 1f)        // how much more the lower rows lag than the top ones
    val drawerItems = S("drawer_items", "profile,group,contacts,saved,archive,settings,appearance,features,folders,invite")       // header / bottom fades appear only once content is under them
    // ---- energy saving (off by default) ----
    val powerSave = B("power_save", false)
    val psIdleAnims = B("ps_idle_anims", true)         // stop decorative loops (record spin, waves, shapes) when idle
    val psStickers = S("ps_stickers", "loop")          // loop | once | still — animated stickers and emoji
    val psBlurLow = B("ps_blur_low", true)             // blur a smaller copy
    val psGifs = B("ps_gifs", false)                   // pause GIFs / round videos in chats until tapped

    fun resetAppearance() {
        all.filter { it.key !in setOf(
            "hide_phone", "strip_tracking", "silent_default", "confirm_delete", "keep_alive", "notify_preview", "mark_read",
            "ghost_offline", "ghost_typing", "keep_deleted", "hide_reactions", "char_counter", "show_ids", "confirm_sticker",
            "kb_height", "archive_hidden", "hide_kb_scroll", "forward_no_author", "autodl_mobile", "autodl_wifi",
        ) }
            .forEach { it.reset() }
    }
}
