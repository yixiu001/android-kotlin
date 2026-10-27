# Vault 为版本化加密文件；Gson 反射字段名必须在升级后保持稳定。
-keep class cn.yixiu.starter.core.VaultState { *; }
-keep class cn.yixiu.starter.core.Session { *; }
-keep class cn.yixiu.starter.core.Pending { *; }
-keep class cn.yixiu.starter.core.Challenge { *; }
-keep class cn.yixiu.starter.core.WireRequest { *; }
-keep class com.tencent.mm.opensdk.** { *; }
