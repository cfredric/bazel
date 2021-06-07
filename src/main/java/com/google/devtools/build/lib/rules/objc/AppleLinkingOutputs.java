// Copyright 2021 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.devtools.build.lib.rules.objc;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.analysis.RuleConfiguredTargetBuilder;
import com.google.devtools.build.lib.collect.nestedset.NestedSet;
import com.google.devtools.build.lib.packages.NativeInfo;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * The providers and artifact outputs returned by the {@code apple_common.link_multi_arch_binary}
 * API.
 */
public class AppleLinkingOutputs {
  /**
   * A set of related platform/architecture-specific outputs generated by {@code
   * apple_common.link_multi_arch_binary}.
   */
  @AutoValue
  public abstract static class LinkingOutput {
    abstract String getPlatform();

    abstract String getArchitecture();

    abstract String getEnvironment();

    abstract Artifact getBinary();

    @Nullable
    abstract Artifact getBitcodeSymbols();

    @Nullable
    abstract Artifact getDsymBinary();

    @Nullable
    abstract Artifact getLinkmap();

    static LinkingOutput.Builder builder() {
      return new AutoValue_AppleLinkingOutputs_LinkingOutput.Builder();
    }

    /** Builder for {@link LinkingOutput}. */
    @AutoValue.Builder
    public abstract static class Builder {
      abstract Builder setPlatform(String platform);

      abstract Builder setArchitecture(String architecture);

      abstract Builder setEnvironment(String environment);

      abstract Builder setBinary(Artifact binary);

      abstract Builder setBitcodeSymbols(Artifact bitcodeSymbols);

      abstract Builder setDsymBinary(Artifact dsymBinary);

      abstract Builder setLinkmap(Artifact linkmap);

      abstract LinkingOutput build();
    }
  }

  private final ObjcProvider depsObjcProvider;
  private final ImmutableList<LinkingOutput> outputs;
  private final ImmutableMap<String, NestedSet<Artifact>> outputGroups;

  private final Artifact legacyBinaryArtifact;
  private final NativeInfo legacyBinaryInfoProvider;
  private final AppleDebugOutputsInfo legacyDebugOutputsProvider;

  AppleLinkingOutputs(
      ObjcProvider depsObjcProvider,
      ImmutableList<LinkingOutput> outputs,
      ImmutableMap<String, NestedSet<Artifact>> outputGroups,
      Artifact legacyBinaryArtifact,
      NativeInfo legacyBinaryInfoProvider,
      AppleDebugOutputsInfo legacyDebugOutputsProvider) {
    this.depsObjcProvider = depsObjcProvider;
    this.outputs = outputs;
    this.outputGroups = outputGroups;

    this.legacyBinaryArtifact = legacyBinaryArtifact;
    this.legacyBinaryInfoProvider = legacyBinaryInfoProvider;
    this.legacyDebugOutputsProvider = legacyDebugOutputsProvider;
  }

  /** Returns an {@link Artifact} representing the linked binary. */
  public Artifact getLegacyBinaryArtifact() {
    return legacyBinaryArtifact;
  }

  /**
   * Returns an {@link ObjcProvider} containing information about the transitive dependencies linked
   * into the binary.
   */
  public ObjcProvider getDepsObjcProvider() {
    return depsObjcProvider;
  }

  /** Returns the list of single-architecture/platform outputs. */
  public ImmutableList<LinkingOutput> getOutputs() {
    return outputs;
  }

  /**
   * Returns a {@link NativeInfo} possessing information about the linked binary. Depending on the
   * type of binary, this may be either a {@link AppleExecutableBinaryInfo}, a {@link
   * AppleDylibBinaryInfo}, or a {@link AppleLoadableBundleBinaryInfo}.
   */
  public NativeInfo getLegacyBinaryInfoProvider() {
    return legacyBinaryInfoProvider;
  }

  /**
   * Returns a {@link AppleDebugOutputsInfo} containing debug information about the linked binary.
   */
  public AppleDebugOutputsInfo getLegacyDebugOutputsProvider() {
    return legacyDebugOutputsProvider;
  }

  /**
   * Returns a map from output group name to set of artifacts belonging to this output group. This
   * should be added to configured target information using {@link
   * RuleConfiguredTargetBuilder#addOutputGroups(Map)}.
   */
  public Map<String, NestedSet<Artifact>> getOutputGroups() {
    return outputGroups;
  }

  /** A builder for {@link AppleBinaryOutput}. */
  public static class Builder {
    private final ImmutableList.Builder<LinkingOutput> outputs;
    private final ImmutableMap.Builder<String, NestedSet<Artifact>> outputGroups;
    private ObjcProvider depsObjcProvider;

    private AppleBinary.BinaryType legacyBinaryType;
    private Artifact legacyBinaryArtifact;
    private AppleDebugOutputsInfo legacyDebugOutputsProvider;

    public Builder() {
      this.outputs = ImmutableList.builder();
      this.outputGroups = ImmutableMap.builder();
    }

    /** Adds a set of related single-architecture/platform artifacts to the output result. */
    public Builder addOutput(LinkingOutput output) {
      outputs.add(output);
      return this;
    }

    /** Adds a set of output groups to the output result. */
    public Builder addOutputGroups(Map<String, NestedSet<Artifact>> outputGroupsToAdd) {
      outputGroups.putAll(outputGroupsToAdd);
      return this;
    }

    /** Sets the legacy binary artifact and type of the output result. */
    public Builder setLegacyBinaryArtifact(
        Artifact binaryArtifact, AppleBinary.BinaryType binaryType) {
      this.legacyBinaryArtifact = binaryArtifact;
      this.legacyBinaryType = binaryType;
      return this;
    }

    /** Sets the legacy debug outputs provider of the output result. */
    public Builder setLegacyDebugOutputsProvider(AppleDebugOutputsInfo debugOutputsProvider) {
      this.legacyDebugOutputsProvider = debugOutputsProvider;
      return this;
    }

    /**
     * Sets the {@link ObjcProvider} that contains information about transitive dependencies linked
     * into the binary.
     */
    public Builder setDepsObjcProvider(ObjcProvider depsObjcProvider) {
      this.depsObjcProvider = depsObjcProvider;
      return this;
    }

    public AppleLinkingOutputs build() {
      return new AppleLinkingOutputs(
          depsObjcProvider,
          outputs.build(),
          outputGroups.build(),
          legacyBinaryArtifact,
          createLegacyBinaryInfoProvider(),
          legacyDebugOutputsProvider);
    }

    /** Returns a new legacy native provider based on the binary type created by the target. */
    private NativeInfo createLegacyBinaryInfoProvider() {
      NativeInfo provider = null;
      if (legacyBinaryType != null) {
        switch (legacyBinaryType) {
          case EXECUTABLE:
            provider = new AppleExecutableBinaryInfo(legacyBinaryArtifact, depsObjcProvider);
            break;
          case DYLIB:
            provider = new AppleDylibBinaryInfo(legacyBinaryArtifact, depsObjcProvider);
            break;
          case LOADABLE_BUNDLE:
            provider = new AppleLoadableBundleBinaryInfo(legacyBinaryArtifact, depsObjcProvider);
            break;
        }
      }
      return provider;
    }
  }
}