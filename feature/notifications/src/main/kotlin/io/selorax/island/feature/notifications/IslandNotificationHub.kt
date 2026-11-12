package io.selorax.island.feature.notifications

public object IslandNotificationHub {
    public var onNotificationReceived: ((title: String, text: String, packageName: String) -> Unit)? = null
}
