package ch.lauzhand.helpinghand.model

// Status transitions for a Need, shared by the app and the server.

/** A donor commits to bring this need in person. */
fun Need.claimedBy(donorId: String): Need =
    copy(status = NeedStatus.CLAIMED, claimedByDonorId = donorId)

/**
 * A donor gives money.
 * SMALL: counts as claimed and paid. BIG: adds to the funded amount,
 * FULFILLED once the target is reached, otherwise FUNDING.
 */
fun Need.withDonation(donorId: String, amountChf: Int): Need = when (type) {
    NeedType.SMALL -> copy(status = NeedStatus.CLAIMED, claimedByDonorId = donorId, paid = true)
    NeedType.BIG -> {
        val funded = fundedAmountChf + amountChf
        val target = targetAmountChf ?: funded
        copy(
            fundedAmountChf = funded,
            status = if (funded >= target) NeedStatus.FULFILLED else NeedStatus.FUNDING,
        )
    }
}

/** The need was delivered. */
fun Need.fulfilled(): Need = copy(status = NeedStatus.FULFILLED)

/** The person confirmed they received it. */
fun Need.confirmed(): Need = copy(status = NeedStatus.CONFIRMED)

/** Counts towards a donor's completed goals. */
val Need.isCompleted: Boolean
    get() = status == NeedStatus.FULFILLED || status == NeedStatus.CONFIRMED
