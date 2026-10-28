package com.kmb.downloader.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ProState(
    val isPro: Boolean,
    val daysRemaining: Int,
    val expiryFormatted: String,
    val referralCount: Int,
    val myCode: String,
    val hasRedeemedFriendCode: Boolean
)

object ReferralManager {
    private const val PREFS_NAME = "kmb_pro_prefs"
    private const val KEY_MY_CODE = "my_referral_code"
    private const val KEY_REFERRAL_COUNT = "referral_count"
    private const val KEY_PRO_EXPIRY = "pro_expiry_timestamp"
    private const val KEY_REDEEMED_CODE = "redeemed_friend_code"

    private lateinit var prefs: SharedPreferences

    private val _proState = MutableStateFlow(
        ProState(
            isPro = false,
            daysRemaining = 0,
            expiryFormatted = "None",
            referralCount = 0,
            myCode = "KMB-XXXXX",
            hasRedeemedFriendCode = false
        )
    )
    val proState: StateFlow<ProState> = _proState.asStateFlow()

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_MY_CODE)) {
            val randomSuffix = UUID.randomUUID().toString().replace("-", "").take(5).uppercase(Locale.ROOT)
            val generatedCode = "KMB-$randomSuffix"
            prefs.edit().putString(KEY_MY_CODE, generatedCode).apply()
        }
        refreshState()
    }

    fun getMyCode(): String {
        return prefs.getString(KEY_MY_CODE, "KMB-VIP01") ?: "KMB-VIP01"
    }

    fun refreshState() {
        val now = System.currentTimeMillis()
        val expiry = prefs.getLong(KEY_PRO_EXPIRY, 0L)
        val isPro = expiry > now
        val daysRemaining = if (isPro) {
            (((expiry - now) / (1000 * 60 * 60 * 24)) + 1).toInt()
        } else {
            0
        }

        val expiryFormatted = if (isPro) {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            sdf.format(Date(expiry))
        } else {
            "Expired"
        }

        val referralCount = prefs.getInt(KEY_REFERRAL_COUNT, 0)
        val redeemed = prefs.getString(KEY_REDEEMED_CODE, null)

        _proState.value = ProState(
            isPro = isPro,
            daysRemaining = daysRemaining,
            expiryFormatted = expiryFormatted,
            referralCount = referralCount,
            myCode = getMyCode(),
            hasRedeemedFriendCode = !redeemed.isNullOrBlank()
        )
    }

    fun addDaysToPro(days: Int) {
        val now = System.currentTimeMillis()
        val currentExpiry = prefs.getLong(KEY_PRO_EXPIRY, 0L)
        val baseTime = if (currentExpiry > now) currentExpiry else now
        val addedMillis = days.toLong() * 24L * 60L * 60L * 1000L
        val newExpiry = baseTime + addedMillis
        prefs.edit().putLong(KEY_PRO_EXPIRY, newExpiry).apply()
        refreshState()
    }

    /**
     * Redeem a friend's referral code.
     * Awards 3 days free Pro as a welcome bonus!
     */
    fun redeemFriendCode(rawCode: String): Result<String> {
        val code = rawCode.trim().uppercase(Locale.ROOT)
        if (!code.startsWith("KMB-") || code.length < 7) {
            return Result.failure(Exception("Invalid code format! Codes start with KMB- (e.g. KMB-7A9B2)"))
        }

        val myCode = getMyCode()
        if (code.equals(myCode, ignoreCase = true)) {
            return Result.failure(Exception("You cannot use your own referral code! Share it with friends."))
        }

        val alreadyRedeemed = prefs.getString(KEY_REDEEMED_CODE, null)
        if (!alreadyRedeemed.isNullOrBlank()) {
            return Result.failure(Exception("You have already redeemed invite code: $alreadyRedeemed"))
        }

        // Successfully redeem
        prefs.edit().putString(KEY_REDEEMED_CODE, code).apply()
        addDaysToPro(3) // 3 Days VIP for entering code
        return Result.success("Code accepted! You received +3 Days KMB Pro VIP Free! 👑")
    }

    /**
     * Record a friend referral (or simulate one in test mode).
     * Milestone rewards:
     * - 1 referral: 1 Day Pro
     * - 3 referrals: 7 Days Pro
     * - 5 referrals: 15 Days Pro
     * - 10 referrals: 30 Days (1 Month) Pro
     */
    fun recordReferral(): String {
        val current = prefs.getInt(KEY_REFERRAL_COUNT, 0)
        val updated = current + 1
        prefs.edit().putInt(KEY_REFERRAL_COUNT, updated).apply()

        var bonusDays = 1 // default for each referral
        var milestoneMsg = "1 friend referred! +1 Day Pro VIP added."

        when (updated) {
            3 -> {
                bonusDays = 7
                milestoneMsg = "🎉 Milestone Reached: 3 Referrals! +7 Days Pro VIP unlocked!"
            }
            5 -> {
                bonusDays = 15
                milestoneMsg = "🔥 Milestone Reached: 5 Referrals! +15 Days Pro VIP unlocked!"
            }
            10 -> {
                bonusDays = 30
                milestoneMsg = "👑 MEGA MILESTONE: 10 Referrals! 1 MONTH FREE KMB PRO VIP UNLOCKED!"
            }
            else -> {
                if (updated > 10 && updated % 5 == 0) {
                    bonusDays = 10
                    milestoneMsg = "🌟 $updated Referrals! +10 Days Pro VIP bonus unlocked!"
                }
            }
        }

        addDaysToPro(bonusDays)
        return milestoneMsg
    }

    fun getInviteMessage(): String {
        val code = getMyCode()
        return "🚀 Download KMB Downloader – The ultimate All-in-One Video Downloader, Audio Studio & WhatsApp Status Saver!\n\n" +
                "🎁 Use my VIP invite code: $code to unlock FREE KMB PRO VIP access!\n\n" +
                "📥 Download official APK here:\nhttps://github.com/khurrammunir377-dot/kmb-downloader/releases/latest"
    }
}
