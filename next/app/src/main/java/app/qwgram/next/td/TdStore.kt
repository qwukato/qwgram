package app.qwgram.next.td

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.qwgram.next.Notifier
import kotlinx.coroutines.flow.MutableSharedFlow
import org.drinkless.tdlib.TdApi

/** Everything the UI shows, kept up to date from TDLib updates (main thread only). */
object TdStore {
    var auth by mutableStateOf<TdApi.AuthorizationState?>(null)
    var connection by mutableStateOf<TdApi.ConnectionState>(TdApi.ConnectionStateConnecting())
    var me by mutableStateOf<TdApi.User?>(null)

    val chats = KeyedMap<Long, TdApi.Chat>()
    val users = KeyedMap<Long, TdApi.User>()
    val basicGroups = KeyedMap<Long, TdApi.BasicGroup>()
    val supergroups = KeyedMap<Long, TdApi.Supergroup>()
    val files = KeyedMap<Int, TdApi.File>()

    /** unread unmuted chats per chat list (folder), straight from TDLib */
    val unreadByList = KeyedMap<String, Int>()
    fun listKey(l: TdApi.ChatList): String = when (l) {
        is TdApi.ChatListFolder -> "f" + l.chatFolderId
        is TdApi.ChatListArchive -> "a"
        else -> "m"
    }
    fun unreadIn(l: TdApi.ChatList): Int = unreadByList[listKey(l)] ?: 0

    /** unread (unmuted) chats in the main list, straight from TDLib */
    var unreadChats by mutableIntStateOf(0)
    val folders = mutableStateListOf<TdApi.ChatFolderInfo>()
    var mainListPosition by mutableIntStateOf(0)

    /** bumped whenever any chat order / list membership changes */
    var listVersion by mutableIntStateOf(0)

    /** chat id -> user ids currently typing (or other action) */
    val actions = KeyedMap<Long, Map<Long, TdApi.ChatAction>>()

    /** message-level events for open chat screens */
    val messageEvents = MutableSharedFlow<TdApi.Update>(extraBufferCapacity = 256)

    fun onUpdate(obj: TdApi.Object) {
        when (obj) {
            is TdApi.UpdateAuthorizationState -> onAuth(obj.authorizationState)
            is TdApi.UpdateConnectionState -> connection = obj.state
            is TdApi.UpdateUser -> {
                users[obj.user.id] = obj.user
                if (me?.id == obj.user.id) me = obj.user
            }
            is TdApi.UpdateUserStatus -> users.peek(obj.userId)?.let { u -> u.status = obj.status; users[obj.userId] = copyUser(u) }
            is TdApi.UpdateBasicGroup -> basicGroups[obj.basicGroup.id] = obj.basicGroup
            is TdApi.UpdateSupergroup -> supergroups[obj.supergroup.id] = obj.supergroup
            is TdApi.UpdateNewChat -> {
                chats[obj.chat.id] = obj.chat
                listVersion++
            }
            is TdApi.UpdateChatTitle -> edit(obj.chatId) { it.title = obj.title }
            is TdApi.UpdateChatPhoto -> edit(obj.chatId) { it.photo = obj.photo }
            is TdApi.UpdateChatLastMessage -> {
                edit(obj.chatId) {
                    it.lastMessage = obj.lastMessage
                    if (obj.positions.isNotEmpty()) it.positions = obj.positions
                }
                listVersion++
            }
            is TdApi.UpdateChatPosition -> {
                edit(obj.chatId) { c -> setPosition(c, obj.position) }
                listVersion++
            }
            is TdApi.UpdateChatReadInbox -> {
                edit(obj.chatId) {
                    it.lastReadInboxMessageId = obj.lastReadInboxMessageId
                    it.unreadCount = obj.unreadCount
                }
                if (obj.unreadCount == 0) Notifier.clear(obj.chatId)
            }
            is TdApi.UpdateChatReadOutbox -> edit(obj.chatId) { it.lastReadOutboxMessageId = obj.lastReadOutboxMessageId }
            is TdApi.UpdateChatUnreadMentionCount -> edit(obj.chatId) { it.unreadMentionCount = obj.unreadMentionCount }
            is TdApi.UpdateChatUnreadReactionCount -> edit(obj.chatId) { it.unreadReactionCount = obj.unreadReactionCount }
            is TdApi.UpdateChatIsMarkedAsUnread -> edit(obj.chatId) { it.isMarkedAsUnread = obj.isMarkedAsUnread }
            is TdApi.UpdateChatNotificationSettings -> edit(obj.chatId) { it.notificationSettings = obj.notificationSettings }
            is TdApi.UpdateChatDraftMessage -> {
                edit(obj.chatId) {
                    it.draftMessage = obj.draftMessage
                    if (obj.positions.isNotEmpty()) it.positions = obj.positions
                }
                listVersion++
            }
            is TdApi.UpdateChatFolders -> {
                folders.clear()
                folders.addAll(obj.chatFolders)
                mainListPosition = obj.mainChatListPosition
            }
            is TdApi.UpdateFile -> files[obj.file.id] = obj.file
            is TdApi.UpdateUnreadChatCount -> {
                unreadByList[listKey(obj.chatList)] = obj.unreadUnmutedCount + obj.markedAsUnreadUnmutedCount
                if (obj.chatList is TdApi.ChatListMain) unreadChats = obj.unreadUnmutedCount + obj.markedAsUnreadUnmutedCount
            }
            is TdApi.UpdateChatAvailableReactions -> edit(obj.chatId) { it.availableReactions = obj.availableReactions }
            is TdApi.UpdateChatPermissions -> edit(obj.chatId) { it.permissions = obj.permissions }
            is TdApi.UpdateChatEmojiStatus -> edit(obj.chatId) { it.emojiStatus = obj.emojiStatus }
            is TdApi.UpdateChatAction -> {
                val sender = obj.senderId
                val uid = when (sender) {
                    is TdApi.MessageSenderUser -> sender.userId
                    is TdApi.MessageSenderChat -> sender.chatId
                    else -> 0L
                }
                val cur = actions.peek(obj.chatId).orEmpty().toMutableMap()
                if (obj.action is TdApi.ChatActionCancel) cur.remove(uid) else cur[uid] = obj.action
                if (cur.isEmpty()) actions.remove(obj.chatId) else actions[obj.chatId] = cur
            }
            is TdApi.UpdateNewMessage -> {
                messageEvents.tryEmit(obj)
                Notifier.onNewMessage(obj.message)
            }
            is TdApi.UpdateMessageSendSucceeded, is TdApi.UpdateMessageSendFailed, is TdApi.UpdateDeleteMessages,
            is TdApi.UpdateMessageContent, is TdApi.UpdateMessageEdited, is TdApi.UpdateMessageInteractionInfo,
            is TdApi.UpdateMessageIsPinned -> messageEvents.tryEmit(obj as TdApi.Update)
            else -> Unit
        }
    }

    private fun copyUser(u: TdApi.User): TdApi.User = TdApi.User().also { n ->
        n.id = u.id; n.firstName = u.firstName; n.lastName = u.lastName; n.usernames = u.usernames
        n.phoneNumber = u.phoneNumber; n.status = u.status; n.profilePhoto = u.profilePhoto
        n.accentColorId = u.accentColorId; n.emojiStatus = u.emojiStatus; n.isContact = u.isContact
        n.isMutualContact = u.isMutualContact; n.isPremium = u.isPremium; n.isSupport = u.isSupport
        n.verificationStatus = u.verificationStatus; n.type = u.type; n.languageCode = u.languageCode
        n.haveAccess = u.haveAccess
    }

    /** chats are mutable TDLib objects; re-putting a shallow copy makes Compose notice the change */
    private inline fun edit(chatId: Long, block: (TdApi.Chat) -> Unit) {
        val c = chats.peek(chatId) ?: return
        block(c)
        chats[chatId] = shallow(c)
    }

    private fun shallow(c: TdApi.Chat): TdApi.Chat = TdApi.Chat().also { n ->
        n.id = c.id; n.type = c.type; n.title = c.title; n.photo = c.photo; n.accentColorId = c.accentColorId
        n.permissions = c.permissions; n.lastMessage = c.lastMessage; n.positions = c.positions; n.chatLists = c.chatLists
        n.messageSenderId = c.messageSenderId; n.blockList = c.blockList; n.hasProtectedContent = c.hasProtectedContent
        n.isMarkedAsUnread = c.isMarkedAsUnread; n.canBeDeletedOnlyForSelf = c.canBeDeletedOnlyForSelf
        n.canBeDeletedForAllUsers = c.canBeDeletedForAllUsers; n.unreadCount = c.unreadCount
        n.lastReadInboxMessageId = c.lastReadInboxMessageId; n.lastReadOutboxMessageId = c.lastReadOutboxMessageId
        n.unreadMentionCount = c.unreadMentionCount; n.unreadReactionCount = c.unreadReactionCount
        n.notificationSettings = c.notificationSettings; n.availableReactions = c.availableReactions
        n.emojiStatus = c.emojiStatus; n.draftMessage = c.draftMessage; n.actionBar = c.actionBar
        n.replyMarkupMessageId = c.replyMarkupMessageId; n.clientData = c.clientData
        n.defaultDisableNotification = c.defaultDisableNotification
    }

    private fun setPosition(c: TdApi.Chat, p: TdApi.ChatPosition) {
        val list = c.positions.filterNot { sameList(it.list, p.list) }.toMutableList()
        if (p.order != 0L) list.add(p)
        c.positions = list.toTypedArray()
    }

    fun sameList(a: TdApi.ChatList?, b: TdApi.ChatList?): Boolean = when {
        a is TdApi.ChatListMain && b is TdApi.ChatListMain -> true
        a is TdApi.ChatListArchive && b is TdApi.ChatListArchive -> true
        a is TdApi.ChatListFolder && b is TdApi.ChatListFolder -> a.chatFolderId == b.chatFolderId
        else -> false
    }

    fun position(c: TdApi.Chat, list: TdApi.ChatList): TdApi.ChatPosition? = c.positions.firstOrNull { sameList(it.list, list) }

    /** chats of a list sorted by TDLib order (pinned first) */
    fun chatsIn(list: TdApi.ChatList): List<TdApi.Chat> {
        @Suppress("UNUSED_VARIABLE") val v = listVersion
        return chats.values.mapNotNull { c -> position(c, list)?.let { p -> c to p.order } }
            .sortedWith(compareByDescending<Pair<TdApi.Chat, Long>> { it.second }.thenByDescending { it.first.id })
            .map { it.first }
    }

    private fun onAuth(state: TdApi.AuthorizationState) {
        auth = state
        when (state) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> Td.send(Td.parameters())
            is TdApi.AuthorizationStateReady -> {
                Td.send(TdApi.GetMe()) { r -> if (r is TdApi.User) me = r }
                Td.send(TdApi.LoadChats(TdApi.ChatListMain(), 100))
                Td.send(TdApi.LoadChats(TdApi.ChatListArchive(), 50))
                Td.send(TdApi.GetContacts())
            }
            is TdApi.AuthorizationStateClosed -> {
                chats.clear(); users.clear(); files.clear(); folders.clear(); me = null
            }
            else -> Unit
        }
    }

    fun loadMore(list: TdApi.ChatList) = Td.send(TdApi.LoadChats(list, 60))

    // ---------- naming helpers ----------

    fun userName(id: Long): String = users[id]?.let { fullName(it) } ?: ""
    fun fullName(u: TdApi.User): String {
        if (u.type is TdApi.UserTypeDeleted) return "Удалённый аккаунт"
        return listOf(u.firstName, u.lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Без имени" }
    }

    fun senderName(sender: TdApi.MessageSender?): String = when (sender) {
        is TdApi.MessageSenderUser -> userName(sender.userId)
        is TdApi.MessageSenderChat -> chats[sender.chatId]?.title ?: ""
        else -> ""
    }

    fun senderId(sender: TdApi.MessageSender?): Long = when (sender) {
        is TdApi.MessageSenderUser -> sender.userId
        is TdApi.MessageSenderChat -> sender.chatId
        else -> 0L
    }

    fun privateUserId(chat: TdApi.Chat): Long = when (val t = chat.type) {
        is TdApi.ChatTypePrivate -> t.userId
        is TdApi.ChatTypeSecret -> t.userId
        else -> 0L
    }

    fun isChannel(chat: TdApi.Chat): Boolean = (chat.type as? TdApi.ChatTypeSupergroup)?.isChannel == true
    fun isGroup(chat: TdApi.Chat): Boolean = chat.type is TdApi.ChatTypeBasicGroup || (chat.type as? TdApi.ChatTypeSupergroup)?.isChannel == false
    fun isSaved(chat: TdApi.Chat): Boolean = me != null && privateUserId(chat) == me?.id
    fun isBot(chat: TdApi.Chat): Boolean = users[privateUserId(chat)]?.type is TdApi.UserTypeBot

    fun isMuted(chat: TdApi.Chat): Boolean = chat.notificationSettings.muteFor > 0

    fun canSend(chat: TdApi.Chat): Boolean {
        val t = chat.type
        if (t is TdApi.ChatTypeSupergroup) {
            val sg = supergroups[t.supergroupId] ?: return !t.isChannel
            val st = sg.status
            if (t.isChannel) return st is TdApi.ChatMemberStatusCreator || (st is TdApi.ChatMemberStatusAdministrator && st.rights.canPostMessages)
            if (st is TdApi.ChatMemberStatusLeft || st is TdApi.ChatMemberStatusBanned) return false
            if (st is TdApi.ChatMemberStatusRestricted) return st.permissions.canSendBasicMessages
            return chat.permissions.canSendBasicMessages || st is TdApi.ChatMemberStatusCreator || st is TdApi.ChatMemberStatusAdministrator
        }
        if (t is TdApi.ChatTypeBasicGroup) {
            val g = basicGroups[t.basicGroupId]
            if (g != null && (g.status is TdApi.ChatMemberStatusLeft || g.status is TdApi.ChatMemberStatusBanned)) return false
        }
        return true
    }

    fun memberCount(chat: TdApi.Chat): Int = when (val t = chat.type) {
        is TdApi.ChatTypeBasicGroup -> basicGroups[t.basicGroupId]?.memberCount ?: 0
        is TdApi.ChatTypeSupergroup -> supergroups[t.supergroupId]?.memberCount ?: 0
        else -> 0
    }
}
