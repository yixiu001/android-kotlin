package cn.yixiu.starter.core

import com.google.gson.JsonObject
import java.util.UUID

data class Session(val access: String, val refresh: String, val id: String)
data class Challenge(val id: String, val purpose: String, val expiresAt: Long, val resendAt: Long = 0,
                     val state: String = "", val appId: String = "", val scope: String = "", val phone: String = "",
                     val useWechatProfile: Boolean = false, val manualProfile: Boolean = false)
data class Pending(val request: WireRequest, val createdAt: Long, val notBefore: Long = 0,
                   val manualProfile: Boolean = false, val mayRefresh: Boolean = true,
                   val continuation: WriteContinuation? = null)

// Encrypted local metadata only; never appended to an API body or debug log.
data class WriteContinuation(val path: String, val body: String, val manualProfile: Boolean,
    val origin: String, val userId: String, val sessionId: String, val generation: Long)

data class WechatProfile(val previewId: String, val userId: String, val sessionId: String,
    val origin: String, val generation: Long, val expiresAt: Long, val nickname: String,
    val avatarUrl: String, val overwrite: Boolean, val canNickname: Boolean, val canAvatar: Boolean)
data class VaultState(val origin: String, val installationId: String = UUID.randomUUID().toString(),
    val generation: Long = 0, val tokenVersion: Long = 0, val session: Session? = null,
    val profile: JsonObject? = null, val pending: Pending? = null, val sms: Challenge? = null,
    val wechat: Challenge? = null, val blocked: String? = null, val notBefore: Long = 0,
    val wechatProfile: WechatProfile? = null)
data class AccountView(val origin: String, val userId: String = "", val accountType: String = "",
    val phone: String = "", val wechatBound: Boolean = false, val hasSession: Boolean = false,
    val pendingPath: String? = null, val privateHidden: Boolean = false, val blocked: String? = null,
    val smsResendAt: Long = 0, val smsExpiresAt: Long = 0, val smsPhone: String = "",
    val notBefore: Long = 0, val nickname: String = "", val avatar: String = "", val avatarUrl: String = "",
    val smsPurpose: String = "", val wechatProfile: WechatProfile? = null)
interface StateStore { fun load(): VaultState?; fun save(state: VaultState) }
class PendingOperation : Exception("有未确认操作，请先恢复原请求")
