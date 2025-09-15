package config

fun HostConfig.toUrl(): String = "${protocol.value}://${host}:${port}"