// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.jps;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.builders.JpsBuildBundle;
import org.jetbrains.jps.incremental.ModuleBuildTarget;
import org.jetbrains.jps.model.JpsNamedElement;
import org.jetbrains.jps.model.module.JpsModule;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a single {@link ModuleBuildTarget} or a set of {@link ModuleBuildTarget} which circularly depend on each other.
 * Since it isn't possible to compile modules which form a circular dependency one by one, the whole {@link ModuleChunk} is passed to
 * {@link org.jetbrains.jps.incremental.ModuleLevelBuilder#build} method.
 */
public final class ModuleChunk {
  private final Set<JpsModule> myModules;
  private final boolean myContainsTests;
  private final Set<ModuleBuildTarget> myTargets;

  public ModuleChunk(@NotNull Set<ModuleBuildTarget> targets) {
    boolean containsTests = false;
    myTargets = targets;
    myModules = new LinkedHashSet<>();
    for (ModuleBuildTarget target : targets) {
      myModules.add(target.getModule());
      containsTests |= target.isTests();
    }
    myContainsTests = containsTests;
  }

  public @Nls @NotNull String getPresentableShortName() {
    String first = myModules.iterator().next().getName();
    String name;
    if (myModules.size() > 1) {
      name = JpsBuildBundle.message("target.description.0.and.1.more", first, myModules.size() - 1);
      String fullName = getName();
      if (fullName.length() < name.length()) {
        name = fullName;
      }
    }
    else {
      name = first;
    }
    if (containsTests()) {
      return JpsBuildBundle.message("target.description.tests.of.0", name);
    }
    return name;
  }

  public @NotNull String getName() {
    if (myModules.size() == 1) {
      return myModules.iterator().next().getName();
    }
    return myModules.stream().map(JpsNamedElement::getName).collect(Collectors.joining(","));
  }

  public @NotNull Set<JpsModule> getModules() {
    return myModules;
  }

  public boolean containsTests() {
    return myContainsTests;
  }

  public @NotNull Set<ModuleBuildTarget> getTargets() {
    return myTargets;
  }

  @Override
  public String toString() {
    return getName();
  }

  /**
   * Returns an arbitrary target included in the chunk.
   */
  public @NotNull ModuleBuildTarget representativeTarget() {
    return myTargets.iterator().next();
  }
}
