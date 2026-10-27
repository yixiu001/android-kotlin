package cn.yixiu.starter.core

import com.google.gson.JsonObject
import java.util.UUID

data class Session(val access: String, val refresh: String, val id: String)
data class Challenge(val id: String, val purpose: String, val expiresAt: Long, val resendAt: Long = 0,
                     val state: String = "", val appId: String = "", val scope: String = "", val phone: String = "")
data class Pending(val request: WireRequest, val createdAt: Long, val notBefore: Long = 0)
data class VaultState(val origin: String, val installationId: String = UUID.randomUUID().toString(),
    val generation: Long = 0, val tokenVersion: Long = 0, val session: Session? = null,
    val profile: JsonObject? = null, val pending: Pending? = null, val sms: Challenge? = null,
    val wechat: Challenge? = null, val blocked: String? = null, val notBefore: Long = 0)
data class AccountView(val origin: String, val userId: String = "", val accountType: String = "",
    val phone: String = "", val wechatBound: Boolean = false, val hasSession: Boolean = false,
    val pendingPath: String? = null, val privateHidden: Boolean = false, val blocked: String? = null,
    val smsResendAt: Long = 0, val smsExpiresAt: Long = 0, val smsPhone: String = "",
    val notBefore: Long = 0)
interface StateStore { fun load(): VaultState?; fun save(state: VaultState) }
class PendingOperation : Exception("有未确认操作，请先恢复原请求")
