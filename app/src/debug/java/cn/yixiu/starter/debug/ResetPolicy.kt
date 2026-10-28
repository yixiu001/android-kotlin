package cn.yixiu.starter.debug
import cn.yixiu.starter.core.Protocol
object ResetPolicy {
    fun panelAfterToggle(expanded: Boolean, busy: Boolean) = !expanded || busy
    fun mayClearLocal(current: String, target: String, confirmed: Boolean) = confirmed && current.isNotBlank() && current == target
    fun mayAutoClear(current: String, target: String, currentOrigin: String, preparedOrigin: String,
                     currentGeneration: Long, preparedGeneration: Long, confirmed: Boolean, unknown: Boolean) =
        mayClearLocal(current, target, confirmed) && !unknown && currentOrigin == preparedOrigin && currentGeneration == preparedGeneration
    fun boundOrigin(value: String) = Protocol.origin(value)
}
