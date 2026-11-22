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
}

class LocalBook : BookApi {
    override val giveaways = SeedGiveaways.all.toMutableStateList()
    private val givenToGiveaways = mutableStateListOf<GiveawayContribution>()
    override val orders = mutableStateListOf<DeliveryOrder>()
    private val photos = mutableStateMapOf<String, String>()

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
}
