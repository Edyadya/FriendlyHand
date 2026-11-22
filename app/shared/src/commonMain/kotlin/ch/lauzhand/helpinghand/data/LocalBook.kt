package ch.lauzhand.helpinghand.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.toMutableStateList
import ch.lauzhand.helpinghand.model.*

/**
 * Giveaways, the hand-over order book and profile pictures. This is kept on the device (in memory, like the
 * demo data) and is NOT synced with the server: the server has no endpoints for them yet. Both repositories
 * (Fake and Remote) share this one implementation.
 */
interface BookApi {
    /** Brand giveaways whose logistics patrons pay for. */
    val giveaways: List<Giveaway>
    fun giveawayContributionsOf(donorId: String): List<GiveawayContribution>
    /** Pays [amountChf] towards a giveaway's logistics goal (never more than what is missing). */
    fun giveToGiveaway(giveawayId: String, donorId: String, amountChf: Int)

    /** The order book: hand-overs waiting for a volunteer, taken by one, picked up, delivered. */
    val orders: List<DeliveryOrder>
    /** Adds the order, or replaces the one with the same id. */
    fun saveOrder(order: DeliveryOrder)

    /** A patron's or volunteer's own profile picture (a small base64 JPEG), or null. */
    fun accountPhotoOf(accountId: String): String?
    fun setAccountPhoto(accountId: String, photoBase64: String?)

    /** The temporary chat of an order (oldest first). It exists while the order is on its way and is deleted on delivery. */
    fun chatOf(orderId: String): List<ChatMessage>
    fun sendChat(orderId: String, fromVolunteer: Boolean, senderName: String, text: String)
    fun closeChat(orderId: String)

    /** The random redemption code of an unlocked reward ([key] names the badge). Made once, then always the same. */
    fun rewardCodeFor(key: String): String

    /** Badges whose "you unlocked it" pop-up was already shown. */
    fun isBadgeCelebrated(key: String): Boolean
    fun markBadgesCelebrated(keys: Collection<String>)

    /** Deliveries whose "it reached them" moment was already shown to the patron. */
    fun isCelebrated(needId: String): Boolean
    fun markCelebrated(needIds: Collection<String>)

    /** Optional gender, city and country of the account (only for themselves). */
    fun accountProfileOf(accountId: String): AccountProfile
    fun setAccountProfile(accountId: String, profile: AccountProfile)
}

class LocalBook : BookApi {
    override val giveaways = SeedGiveaways.all.toMutableStateList()
    private val givenToGiveaways = mutableStateListOf<GiveawayContribution>()
    override val orders = mutableStateListOf<DeliveryOrder>()
    private val photos = mutableStateMapOf<String, String>()
    private val profiles = mutableStateMapOf<String, AccountProfile>()
    private val celebrated = mutableStateListOf<String>()
    private val chat = mutableStateListOf<ChatMessage>()
    private val rewardCodes = mutableMapOf<String, String>()
    private val celebratedBadges = mutableStateListOf<String>()

    override fun giveawayContributionsOf(donorId: String) = givenToGiveaways.filter { it.donorId == donorId }

    override fun giveToGiveaway(giveawayId: String, donorId: String, amountChf: Int) {
        val i = giveaways.indexOfFirst { it.id == giveawayId }
        if (i < 0) return
        val (after, taken) = giveaways[i].withContribution(amountChf)
        if (taken <= 0) return
        giveaways[i] = after
        givenToGiveaways += GiveawayContribution(donorId, giveawayId, taken, nowMillis())
    }

    override fun saveOrder(order: DeliveryOrder) {
        val i = orders.indexOfFirst { it.id == order.id }
        if (i >= 0) orders[i] = order else orders += order
    }

    override fun accountPhotoOf(accountId: String): String? = photos[accountId]

    override fun setAccountPhoto(accountId: String, photoBase64: String?) {
        if (photoBase64 == null) photos.remove(accountId) else photos[accountId] = photoBase64
    }

    override fun chatOf(orderId: String): List<ChatMessage> = chat.filter { it.orderId == orderId }

    override fun sendChat(orderId: String, fromVolunteer: Boolean, senderName: String, text: String) {
        val clean = text.trim().take(MAX_CHAT_CHARS)
        if (clean.isEmpty()) return
        // only while the order is on its way
        if (orders.firstOrNull { it.id == orderId }?.chatOpen != true) return
        chat += ChatMessage(newId("m"), orderId, fromVolunteer, senderName, clean, nowMillis())
    }

    override fun closeChat(orderId: String) { chat.removeAll { it.orderId == orderId } }

    override fun rewardCodeFor(key: String): String = rewardCodes.getOrPut(key) {
        val letters = "ABCDEFGHJKMNPQRSTUVWXYZ23456789" // no look-alike characters
        "HH-" + (1..8).map { letters.random() }.joinToString("")
    }

    override fun isBadgeCelebrated(key: String): Boolean = key in celebratedBadges

    override fun markBadgesCelebrated(keys: Collection<String>) { keys.forEach { if (it !in celebratedBadges) celebratedBadges += it } }

    override fun isCelebrated(needId: String): Boolean = needId in celebrated

    override fun markCelebrated(needIds: Collection<String>) {
        needIds.forEach { if (it !in celebrated) celebrated += it }
    }

    override fun accountProfileOf(accountId: String): AccountProfile = profiles[accountId] ?: AccountProfile()

    override fun setAccountProfile(accountId: String, profile: AccountProfile) {
        profiles[accountId] = profile.copy(city = profile.city.trim(), country = profile.country.trim())
    }
}
