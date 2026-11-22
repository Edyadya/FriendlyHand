package ch.lauzhand.helpinghand.model

import kotlinx.serialization.Serializable

/**
 * A brand offers leftover goods (clothes, say) for free; patrons pay for the LOGISTICS (collection, sorting,
 * transport) as a sum goal. When the goal is reached, the goods are collected and handed out through the volunteers.
 * [demo] marks offers that are examples for the demo and not a real agreement with the brand.
 */
@Serializable
data class Giveaway(
    val id: String,
    val brand: String,
    val title: String,
    val description: String,
    val logisticsGoalChf: Int,
    val fundedChf: Int = 0,
    val demo: Boolean = true,
) {
    val isFunded: Boolean get() = fundedChf >= logisticsGoalChf
    val remainingChf: Int get() = (logisticsGoalChf - fundedChf).coerceAtLeast(0)
}

/** What a patron paid towards a giveaway's logistics. */
@Serializable
data class GiveawayContribution(val donorId: String, val giveawayId: String, val amountChf: Int, val atMillis: Long = 0)

/** Adds [amountChf] to the goal (never beyond it). Returns the giveaway and how much was really taken. */
fun Giveaway.withContribution(amountChf: Int): Pair<Giveaway, Int> {
    val taken = amountChf.coerceIn(0, remainingChf)
    return copy(fundedChf = fundedChf + taken) to taken
}

object SeedGiveaways {
    // Example offers for the demo. The brand named here has NOT agreed to anything.
    val all = listOf(
        Giveaway(
            "gv-zara-coats", "Zara", "Leftover winter coats and jackets",
            "End-of-season coats and jackets that were not sold. Zara would give them for free; patrons cover the transport and sorting so volunteers can hand them out.",
            logisticsGoalChf = 600, fundedChf = 180,
        ),
        Giveaway(
            "gv-basics", "A clothing brand", "Leftover socks, underwear and T-shirts",
            "Basic clothes in clean, unopened packs. The brand gives the stock; the money pays for the van and the sorting.",
            logisticsGoalChf = 350, fundedChf = 90,
        ),
        Giveaway(
            "gv-sleep", "An outdoor brand", "Leftover sleeping bags and rain jackets",
            "Outdoor gear that kept the shop floor too long. Patrons pay for storage and delivery to the volunteers.",
            logisticsGoalChf = 800, fundedChf = 0,
        ),
    )
}
