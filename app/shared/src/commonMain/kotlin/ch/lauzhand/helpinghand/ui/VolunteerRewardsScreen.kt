package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.lauzhand.helpinghand.data.HelpRepository
import ch.lauzhand.helpinghand.model.Account
import ch.lauzhand.helpinghand.model.VolunteerBadge
import ch.lauzhand.helpinghand.model.VolunteerProgress
import ch.lauzhand.helpinghand.model.deliveriesDone
import ch.lauzhand.helpinghand.model.deliveriesInProgress
import ch.lauzhand.helpinghand.model.peopleHelpedOverall
import ch.lauzhand.helpinghand.model.volunteerBadges
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal fun VolunteerBadge.title(): StringResource = when (this) {
    VolunteerBadge.FIRST_DELIVERY -> Res.string.vr_badge_first
    VolunteerBadge.FIVE_DELIVERIES -> Res.string.vr_badge_five
    VolunteerBadge.TEN_DELIVERIES -> Res.string.vr_badge_ten
    VolunteerBadge.TWENTY_FIVE_DELIVERIES -> Res.string.vr_badge_25
    VolunteerBadge.THREE_AREAS -> Res.string.vr_badge_areas
    VolunteerBadge.FIVE_AREAS -> Res.string.vr_badge_areas5
    VolunteerBadge.FIVE_PEOPLE -> Res.string.vr_badge_people
    VolunteerBadge.TEN_PEOPLE -> Res.string.vr_badge_people10
    VolunteerBadge.FIRST_PURCHASE -> Res.string.vr_badge_purchase
    VolunteerBadge.FIVE_PURCHASES -> Res.string.vr_badge_purchase5
    VolunteerBadge.FIRST_HANDOVER -> Res.string.vr_badge_handover1
    VolunteerBadge.FIVE_HANDOVERS -> Res.string.vr_badge_handover5
}

internal fun VolunteerBadge.description(): StringResource = when (this) {
    VolunteerBadge.FIRST_DELIVERY -> Res.string.vr_badge_first_desc
    VolunteerBadge.FIVE_DELIVERIES -> Res.string.vr_badge_five_desc
    VolunteerBadge.TEN_DELIVERIES -> Res.string.vr_badge_ten_desc
    VolunteerBadge.TWENTY_FIVE_DELIVERIES -> Res.string.vr_badge_25_desc
    VolunteerBadge.THREE_AREAS -> Res.string.vr_badge_areas_desc
    VolunteerBadge.FIVE_AREAS -> Res.string.vr_badge_areas5_desc
    VolunteerBadge.FIVE_PEOPLE -> Res.string.vr_badge_people_desc
    VolunteerBadge.TEN_PEOPLE -> Res.string.vr_badge_people10_desc
    VolunteerBadge.FIRST_PURCHASE -> Res.string.vr_badge_purchase_desc
    VolunteerBadge.FIVE_PURCHASES -> Res.string.vr_badge_purchase5_desc
    VolunteerBadge.FIRST_HANDOVER -> Res.string.vr_badge_handover1_desc
    VolunteerBadge.FIVE_HANDOVERS -> Res.string.vr_badge_handover5_desc
}

internal fun VolunteerBadge.glyph(): BadgeGlyph = when (this) {
    VolunteerBadge.FIRST_DELIVERY -> BadgeGlyph.PARCEL
    VolunteerBadge.FIVE_DELIVERIES -> BadgeGlyph.PARCELS
    VolunteerBadge.TEN_DELIVERIES -> BadgeGlyph.TRUCK
    VolunteerBadge.TWENTY_FIVE_DELIVERIES -> BadgeGlyph.TRUCK_STAR
    VolunteerBadge.THREE_AREAS -> BadgeGlyph.PIN
    VolunteerBadge.FIVE_AREAS -> BadgeGlyph.ROUTE
    VolunteerBadge.FIVE_PEOPLE -> BadgeGlyph.PEOPLE
    VolunteerBadge.TEN_PEOPLE -> BadgeGlyph.CROWD
    VolunteerBadge.FIRST_PURCHASE -> BadgeGlyph.BAG
    VolunteerBadge.FIVE_PURCHASES -> BadgeGlyph.CART
    VolunteerBadge.FIRST_HANDOVER -> BadgeGlyph.HAND
    VolunteerBadge.FIVE_HANDOVERS -> BadgeGlyph.HANDS
}

/**
 * Volunteers' Rewards tab: the same design as the patrons'. A personal badge, the community banner, the goals panel
 * (each delivered order is a goal), the partner voucher, a "Next up" card and all twelve badges in three groups. Tap a
 * badge to see how to unlock it, its reward and, once it is unlocked, its QR code.
 */
@Composable
fun VolunteerRewardsScreen(repo: HelpRepository, account: Account) {
    val done = deliveriesDone(repo.orders, account.id)
    val onTheWay = deliveriesInProgress(repo.orders, account.id)
    val badges = volunteerBadges(repo.orders, account.id)
    // two designs: big boxes in four columns on wide windows (web), small blocks on phones
    val wide = rememberIsWide()
    var opened by remember { mutableStateOf<VolunteerProgress?>(null) }

    fun tile(b: VolunteerBadge): RewardTile {
        val p = badges.first { it.badge == b }
        return RewardTile(b.name, b.title(), b.glyph(), p.unlocked, false, p.progress, b.target)
    }
    val groups = listOf(
        RewardGroup(Res.string.group_delivering, listOf(VolunteerBadge.FIRST_DELIVERY, VolunteerBadge.FIVE_DELIVERIES, VolunteerBadge.TEN_DELIVERIES, VolunteerBadge.TWENTY_FIVE_DELIVERIES).map(::tile)),
        RewardGroup(Res.string.group_reaching, listOf(VolunteerBadge.THREE_AREAS, VolunteerBadge.FIVE_AREAS, VolunteerBadge.FIVE_PEOPLE, VolunteerBadge.TEN_PEOPLE).map(::tile)),
        RewardGroup(Res.string.group_handing, listOf(VolunteerBadge.FIRST_HANDOVER, VolunteerBadge.FIVE_HANDOVERS, VolunteerBadge.FIRST_PURCHASE, VolunteerBadge.FIVE_PURCHASES).map(::tile)),
    )

    opened?.let { b ->
        RewardInfoDialog(
            repo = repo, title = b.badge.title(), description = b.badge.description(), reward = b.badge.reward, glyph = b.badge.glyph(),
            unlocked = b.unlocked, pending = false, progress = b.progress, target = b.badge.target,
            qrKey = "v:${b.badge.name}",
        ) { opened = null }
    }

    // centred, and no wider than 960 dp on big screens
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4), // four across: big boxes on the web, small blocks on a phone
            modifier = (if (wide) Modifier.widthIn(max = 960.dp) else Modifier).fillMaxSize(),
            contentPadding = PaddingValues(if (wide) 16.dp else 12.dp),
            horizontalArrangement = Arrangement.spacedBy(if (wide) 14.dp else 8.dp),
            verticalArrangement = Arrangement.spacedBy(if (wide) 14.dp else 8.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) { Text(stringResource(Res.string.achievements_title), style = MaterialTheme.typography.headlineSmall) }
            item(span = { GridItemSpan(maxLineSpan) }) {
                PersonalBadgeCard(
                    name = account.name, count = done,
                    countLabel = if (done == 1) stringResource(Res.string.art_count_deliveries_one) else stringResource(Res.string.art_count_deliveries, done),
                    goldDone = badges.count { it.unlocked }, caption = Res.string.art_caption_volunteer,
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) { PlatformHelped(peopleHelpedOverall(repo.needs)) }
            item(span = { GridItemSpan(maxLineSpan) }) { GoalsHero(done, pending = onTheWay) }
            item(span = { GridItemSpan(maxLineSpan) }) { VoucherCard(done, pending = onTheWay) }
            rewardGroups(groups, wide) { t -> opened = badges.first { it.badge.name == t.key } }
        }
    }
}
