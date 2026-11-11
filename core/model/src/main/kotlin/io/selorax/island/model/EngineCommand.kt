package io.selorax.island.model

public sealed interface EngineCommand {
    public data class ApplyEvent(public val event: IslandEvent) : EngineCommand
    public data class HandleIntent(public val intent: UserIntent) : EngineCommand
    public data class Tick(public val currentEpochMs: Long) : EngineCommand
    public data class SetSystemState(public val event: SystemEvent) : EngineCommand
}
