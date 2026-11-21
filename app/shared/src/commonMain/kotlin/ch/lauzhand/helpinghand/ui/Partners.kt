package ch.lauzhand.helpinghand.ui

import ch.lauzhand.helpinghand.model.AchievementId
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.StringResource

/**
 * Partners who thank donors with rewards.
 * JetBrains and IMS are real partners. All others are invented for the demo
 * (replace them when real partners join).
 */
enum class Partner(val displayName: String, val kind: StringResource) {
    JETBRAINS("JetBrains", Res.string.partner_kind_jetbrains),
    IMS("IMS", Res.string.partner_kind_ims),
    WAMAZON("Wamazon", Res.string.partner_kind_wamazon),       // fictional
    KOLIBRI("Kafé Kolibri", Res.string.partner_kind_kolibri),  // fictional
    VELOLAC("Velolac", Res.string.partner_kind_velolac),       // fictional
    PAGINA("Pagina Books", Res.string.partner_kind_pagina),    // fictional
}

/** What a badge earns, and from which partner. */
data class BadgeReward(val partner: Partner, val reward: StringResource)

// NOTE: the JetBrains and IMS reward texts are placeholders; confirm them with the partners.
val AchievementId.reward: BadgeReward
    get() = when (this) {
        AchievementId.FIRST_HELP -> BadgeReward(Partner.KOLIBRI, Res.string.reward_coffee)
        AchievementId.HELPED_3 -> BadgeReward(Partner.WAMAZON, Res.string.reward_gift_20)
        AchievementId.HELPED_10 -> BadgeReward(Partner.JETBRAINS, Res.string.reward_jetbrains)
        AchievementId.HAND_TO_HAND -> BadgeReward(Partner.VELOLAC, Res.string.reward_bike)
        AchievementId.GOAL_REACHED -> BadgeReward(Partner.IMS, Res.string.reward_ims)
        AchievementId.GAVE_100 -> BadgeReward(Partner.WAMAZON, Res.string.reward_gift_50)
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
