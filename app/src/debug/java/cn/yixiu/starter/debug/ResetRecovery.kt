package cn.yixiu.starter.debug

import com.google.gson.JsonObject

data class ResetRecovery(val preview: JsonObject?, val unknown: Boolean, val done: Boolean = false)

/** A later refusal cannot disprove an earlier execution with an unknown outcome. */
fun recoverResetFailure(original: JsonObject, error: Exception, priorUnknown: Boolean = false): ResetRecovery {
    if (priorUnknown) return ResetRecovery(original, unknown = true)
    val definitive = error is TestToolsFailure && when (error.code) {
        "TEST_GRANT_INVALID" -> error.status == 401
        "TEST_TOOLS_DISABLED" -> error.status == 403
        "TEST_PREVIEW_CHANGED", "TEST_TARGET_INVALID" -> error.status == 409
        else -> false
    }
    return if (definitive) ResetRecovery(null, unknown = false)
    else ResetRecovery(original, unknown = true)
}
