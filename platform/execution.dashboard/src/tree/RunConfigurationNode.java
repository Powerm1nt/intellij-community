// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.platform.execution.dashboard.tree;

import com.intellij.execution.Executor;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.dashboard.RunDashboardCustomizer;
import com.intellij.execution.dashboard.RunDashboardManager;
import com.intellij.execution.dashboard.RunDashboardManager.RunDashboardService;
import com.intellij.execution.dashboard.RunDashboardRunConfigurationNode;
import com.intellij.execution.dashboard.RunDashboardRunConfigurationStatus;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.RunContentManagerImpl;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.platform.execution.dashboard.RunDashboardManagerImpl;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.content.Content;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author konstantin.aleev
 */
public final class RunConfigurationNode extends AbstractTreeNode<RunDashboardService>
  implements RunDashboardRunConfigurationNode {

  private final List<RunDashboardCustomizer> myCustomizers;
  private final UserDataHolder myUserDataHolder = new UserDataHolderBase();

  public RunConfigurationNode(Project project, @NotNull RunDashboardService service,
                              @NotNull List<RunDashboardCustomizer> customizers) {
    super(project, service);
    myCustomizers = customizers;
  }

  @Override
  public @NotNull RunnerAndConfigurationSettings getConfigurationSettings() {
    //noinspection ConstantConditions ???
    return getValue().getSettings();
  }

  @Override
  public @Nullable RunContentDescriptor getDescriptor() {
    //noinspection ConstantConditions ???
    return getValue().getDescriptor();
  }

  @Override
  public @Nullable Content getContent() {
    //noinspection ConstantConditions ???
    return getValue().getContent();
  }

  @Override
  protected void update(@NotNull PresentationData presentation) {
    RunnerAndConfigurationSettings configurationSettings = getConfigurationSettings();
    //noinspection ConstantConditions
    boolean isStored = RunManager.getInstance(getProject()).hasSettings(configurationSettings);
    SimpleTextAttributes nameAttributes;
    if (isStored) {
      nameAttributes = getContent() != null ? SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES : SimpleTextAttributes.REGULAR_ATTRIBUTES;
    }
    else {
      nameAttributes = SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES;
    }
    presentation.addText(configurationSettings.getName(), nameAttributes);
    Icon icon = getIcon(configurationSettings);
    presentation.setIcon(isStored ? icon : IconLoader.createLazy(() -> IconLoader.getDisabledIcon(icon)));

    for (RunDashboardCustomizer customizer : myCustomizers) {
      if (customizer.updatePresentation(presentation, this)) {
        return;
      }
    }
  }

  private Icon getIcon(RunnerAndConfigurationSettings configurationSettings) {
    Icon icon = null;
    RunDashboardRunConfigurationStatus status = getStatus();
    if (RunDashboardRunConfigurationStatus.STARTED.equals(status)) {
      icon = getExecutorIcon();
    }
    else if (RunDashboardRunConfigurationStatus.FAILED.equals(status)) {
      icon = status.getIcon();
    }
    if (icon == null) {
      icon = RunManagerEx.getInstanceEx(getProject()).getConfigurationIcon(configurationSettings);
    }
    return icon;
  }

  @Override
  public @NotNull Collection<? extends AbstractTreeNode<?>> getChildren() {
    for (RunDashboardCustomizer customizer : myCustomizers) {
      Collection<? extends AbstractTreeNode<?>> children = customizer.getChildren(this);
      if (children != null) {
        for (AbstractTreeNode<?> child : children) {
          child.setParent(this);
        }
        return children;
      }
    }
    return Collections.emptyList();
  }

  @Override
  public @Nullable <T> T getUserData(@NotNull Key<T> key) {
    return myUserDataHolder.getUserData(key);
  }

  @Override
  public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {
    myUserDataHolder.putUserData(key, value);
  }

  @Override
  public @NotNull List<RunDashboardCustomizer> getCustomizers() {
    return myCustomizers;
  }

  @Override
  public @NotNull RunDashboardRunConfigurationStatus getStatus() {
    for (RunDashboardCustomizer customizer : myCustomizers) {
      RunDashboardRunConfigurationStatus status = customizer.getStatus(this);
      if (status != null) {
        return status;
      }
    }
    RunDashboardRunConfigurationStatus status = RunDashboardRunConfigurationStatus.getStatus(this);
    if (status == RunDashboardRunConfigurationStatus.CONFIGURED) {
      RunDashboardRunConfigurationStatus persistedStatus =
        ((RunDashboardManagerImpl)RunDashboardManager.getInstance(getProject())).getPersistedStatus(
          getConfigurationSettings().getConfiguration());
      if (persistedStatus != null) {
        return persistedStatus;
      }
    }
    return status;
  }

  private @Nullable Icon getExecutorIcon() {
    Content content = getContent();
    if (content != null) {
      if (!RunContentManagerImpl.isTerminated(content)) {
        Executor executor = RunContentManagerImpl.getExecutorByContent(content);
        if (executor != null) {
          return executor.getIcon();
        }
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return getConfigurationSettings().getName();
  }
}
