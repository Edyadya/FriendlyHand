package ch.lauzhand.helpinghand.ui

import ch.lauzhand.helpinghand.model.AchievementId
import ch.lauzhand.helpinghand.model.VolunteerBadge
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.StringResource

/**
 * Partners who thank donors with rewards.
 * JetBrains and IMC are real partners. All others are invented for the demo
 * (replace them when real partners join).
 */
enum class Partner(val displayName: String, val kind: StringResource) {
    JETBRAINS("JetBrains", Res.string.partner_kind_jetbrains),
    IMC("IMC", Res.string.partner_kind_imc),
    WAMAZON("Wamazon", Res.string.partner_kind_wamazon),       // fictional
    KOLIBRI("Kafé Kolibri", Res.string.partner_kind_kolibri),  // fictional
    VELOLAC("Velolac", Res.string.partner_kind_velolac),       // fictional
    PAGINA("Pagina Books", Res.string.partner_kind_pagina),    // fictional
}

/** What a badge earns, and from which partner. */
data class BadgeReward(val partner: Partner, val reward: StringResource)

// NOTE: the JetBrains and IMC reward texts are placeholders; confirm them with the partners.
val AchievementId.reward: BadgeReward
    get() = when (this) {
        AchievementId.FIRST_HELP -> BadgeReward(Partner.KOLIBRI, Res.string.reward_coffee)
        AchievementId.HELPED_3 -> BadgeReward(Partner.WAMAZON, Res.string.reward_gift_20)
        AchievementId.HELPED_10 -> BadgeReward(Partner.JETBRAINS, Res.string.reward_jetbrains)
        AchievementId.HAND_TO_HAND -> BadgeReward(Partner.VELOLAC, Res.string.reward_bike)
        AchievementId.GOAL_REACHED -> BadgeReward(Partner.IMC, Res.string.reward_imc)
        AchievementId.GAVE_100 -> BadgeReward(Partner.WAMAZON, Res.string.reward_gift_50)
        AchievementId.GAVE_50 -> BadgeReward(Partner.KOLIBRI, Res.string.reward_coffee)
        AchievementId.GAVE_250 -> BadgeReward(Partner.PAGINA, Res.string.reward_book)
        AchievementId.GAVE_500 -> BadgeReward(Partner.WAMAZON, Res.string.reward_gift_50)
        AchievementId.GAVE_1000 -> BadgeReward(Partner.IMC, Res.string.reward_imc)
        AchievementId.THREE_AREAS -> BadgeReward(Partner.PAGINA, Res.string.reward_book)
        AchievementId.THANKS_3 -> BadgeReward(Partner.KOLIBRI, Res.string.reward_breakfast)
    }

/** Short monogram for a partner's round badge, e.g. "JB", "KK". */
val Partner.monogram: String
    get() = displayName.split(' ').filter { it.isNotBlank() }.let { words ->
        if (displayName.length <= 3) displayName
        else if (words.size >= 2) "${words[0][0]}${words[1][0]}"
        else displayName.filter { it.isUpperCase() }.take(2).ifEmpty { displayName.take(2) }
    }.uppercase()

/** What a volunteer badge earns, and from which partner: the same kind of rewards patrons get. */
val VolunteerBadge.reward: BadgeReward
    get() = when (this) {
        VolunteerBadge.FIRST_DELIVERY -> BadgeReward(Partner.KOLIBRI, Res.string.reward_coffee)
        VolunteerBadge.FIVE_DELIVERIES -> BadgeReward(Partner.WAMAZON, Res.string.reward_gift_20)
        VolunteerBadge.TEN_DELIVERIES -> BadgeReward(Partner.JETBRAINS, Res.string.reward_jetbrains)
        VolunteerBadge.TWENTY_FIVE_DELIVERIES -> BadgeReward(Partner.IMC, Res.string.reward_imc)
        VolunteerBadge.THREE_AREAS -> BadgeReward(Partner.PAGINA, Res.string.reward_book)
        VolunteerBadge.FIVE_AREAS -> BadgeReward(Partner.WAMAZON, Res.string.reward_gift_50)
        VolunteerBadge.FIVE_PEOPLE -> BadgeReward(Partner.KOLIBRI, Res.string.reward_breakfast)
        VolunteerBadge.TEN_PEOPLE -> BadgeReward(Partner.JETBRAINS, Res.string.reward_jetbrains)
        VolunteerBadge.FIRST_PURCHASE -> BadgeReward(Partner.VELOLAC, Res.string.reward_bike)
        VolunteerBadge.FIVE_PURCHASES -> BadgeReward(Partner.WAMAZON, Res.string.reward_gift_20)
        VolunteerBadge.FIRST_HANDOVER -> BadgeReward(Partner.KOLIBRI, Res.string.reward_coffee)
        VolunteerBadge.FIVE_HANDOVERS -> BadgeReward(Partner.PAGINA, Res.string.reward_book)
    }
