package com.frameout.engine.repair

enum class ContainerHealth { Healthy, TruncatedData, MissingIndex, Damaged, NotIsoBmff }

class ContainerReport(
    val health: ContainerHealth,
    val validEnd: Long,
    val length: Long,
    val fragmented: Boolean = false
) {
    val needsRepair: Boolean get() = health == ContainerHealth.TruncatedData || health == ContainerHealth.Damaged
    val recoverable: Boolean get() = health != ContainerHealth.MissingIndex
}
