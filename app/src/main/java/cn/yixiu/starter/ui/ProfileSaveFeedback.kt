package cn.yixiu.starter.ui

import cn.yixiu.starter.core.AccountView
import com.google.gson.JsonParser

internal fun profileTargetMatches(input: String, before: AccountView, after: AccountView): Boolean = runCatching {
    val target = JsonParser.parseString(input).asJsonObject
    before.origin == after.origin && before.userId == after.userId && !after.privateHidden &&
        (target.get("nickname")?.asString?.let { it == after.nickname } ?: true) &&
        (target.get("avatar")?.asString?.let { it == after.avatar } ?: true)
}.getOrDefault(false)

/** 仅描述当前界面提交的结果，不替代 AccountEngine 中持久化的待确认请求。 */
enum class ProfileSaveStatus { Idle, Saving, Saved, Failed, Pending }

data class ProfileSaveFeedback(val attempt: Long = 0, val origin: String = "", val userId: String = "",
                               val status: ProfileSaveStatus = ProfileSaveStatus.Idle, val error: String = "")
