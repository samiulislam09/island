package io.selorax.island.common

public sealed interface IslandResult<out T> {
    public data class Success<out T>(public val data: T) : IslandResult<T>
    public data class Failure(public val throwable: Throwable) : IslandResult<Nothing>
}
