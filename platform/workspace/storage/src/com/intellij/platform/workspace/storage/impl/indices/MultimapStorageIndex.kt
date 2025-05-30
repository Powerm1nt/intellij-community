// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.platform.workspace.storage.impl.indices

import com.intellij.platform.workspace.storage.SymbolicEntityId
import com.intellij.platform.workspace.storage.impl.EntityId
import com.intellij.platform.workspace.storage.impl.WorkspaceEntityData
import com.intellij.platform.workspace.storage.impl.containers.BidirectionalLongMultiMap
import com.intellij.platform.workspace.storage.impl.containers.putAll
import org.jetbrains.annotations.TestOnly

private typealias BidirectionalMap = BidirectionalLongMultiMap<SymbolicEntityId<*>>
//private typealias BidirectionalMap = BidirectionalMultiMap<EntityId, PersistentEntityId<*>>

internal sealed class AbstractMultimapStorageIndex protected constructor(
  internal open val index: BidirectionalMap,
) {

  internal fun getIdsByEntry(entitySource: SymbolicEntityId<*>): Set<EntityId> = index.getKeys(entitySource)

  internal fun getEntriesById(id: EntityId): Set<SymbolicEntityId<*>> = index.getValues(id)

  internal fun entries(): Collection<SymbolicEntityId<*>> = index.values

  internal fun toMap(): Map<Long, Set<SymbolicEntityId<*>>> {
    return index.toMap()
  }
}

internal class ImmutableMultimapStorageIndex internal constructor(
  index: BidirectionalMap,
) : AbstractMultimapStorageIndex(index) {
  constructor() : this(BidirectionalMap())
}

internal class MutableMultimapStorageIndex private constructor(
  // Do not write to [index] directly! Create a method in this index and call [startWrite] before write.
  override var index: BidirectionalMap,
) : AbstractMultimapStorageIndex(index), WorkspaceMutableIndex<SymbolicEntityId<*>> {

  private var freezed = true

  internal fun index(id: EntityId, elements: Set<SymbolicEntityId<*>>? = null) {
    startWrite()
    index.removeKey(id)
    if (elements == null) return
    elements.forEach { index.put(id, it) }
  }

  internal fun index(id: EntityId, element: SymbolicEntityId<*>) {
    startWrite()
    index.put(id, element)
  }

  internal fun remove(id: EntityId, element: SymbolicEntityId<*>) {
    startWrite()
    index.remove(id, element)
  }

  @TestOnly
  internal fun clear() {
    startWrite()
    index.clear()
  }

  @TestOnly
  internal fun copyFrom(another: AbstractMultimapStorageIndex) {
    startWrite()
    this.index.putAll(another.index)
  }

  private fun startWrite() {
    if (!freezed) return
    freezed = false
    index = copyIndex()
  }

  private fun copyIndex(): BidirectionalMap = index.copy()

  fun toImmutable(): ImmutableMultimapStorageIndex {
    freezed = true
    return ImmutableMultimapStorageIndex(index)
  }

  companion object {
    fun from(other: AbstractMultimapStorageIndex): MutableMultimapStorageIndex {
      if (other is MutableMultimapStorageIndex) other.freezed = true
      return MutableMultimapStorageIndex(other.index)
    }
  }

  override fun index(entity: WorkspaceEntityData<*>, data: SymbolicEntityId<*>) {
    val id = entity.createEntityId()
    this.index(id, data)
  }

  override fun remove(entity: WorkspaceEntityData<*>, data: SymbolicEntityId<*>) {
    val id = entity.createEntityId()
    this.remove(id, data)
  }
}

