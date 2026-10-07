#!/usr/bin/env bash
# JitPack's install step (see jitpack.yml). JitPack serves whatever ends up in ~/.m2/repository under
# $GROUP.$ARTIFACT at version $VERSION.
#
# Since early October 2026, Java on JitPack's build image often can't open jars at all, so neither the
# Gradle wrapper nor an SDKMAN Gradle can start (https://github.com/jitpack/jitpack.io/issues/8096).
# Tagged releases therefore don't build here: the release workflow attaches the artifacts it built and
# tested, already laid out as a Maven repository, and this script unpacks them. Other versions
# (commits, branches) still build with Gradle.
set -uo pipefail

repository="${GROUP#com.github.}/$ARTIFACT"
asset="https://github.com/$repository/releases/download/$VERSION/jitpack-maven-repo.zip"

install_release() {
    # the release workflow may still be running when JitPack is first asked for a tag, so wait for it
    for attempt in $(seq 1 30); do
        if curl -fsSL -o /tmp/jitpack-maven-repo.zip "$asset"; then
            local installed="$HOME/.m2/repository/${GROUP//.//}/$ARTIFACT"
            # JitPack looks for artifacts written during the build, both in the project directory and in
            # ~/.m2 (as a Gradle publish would leave them), so unpack into both without the zip's timestamps
            for target in "$HOME/.m2/repository" build/jitpack-repo; do
                mkdir -p "$target"
                unzip -oq -DD /tmp/jitpack-maven-repo.zip -d "$target" || return 1
            done
            find "$installed" build/jitpack-repo -type f -exec touch {} +
            echo "== installed release artifacts from $asset:"
            find "$installed" -type f \( -name "*.jar" -o -name "*.pom" \) -exec ls -l {} +
            return 0
        fi
        echo "== release artifacts not available yet ($attempt/30): $asset"
        sleep 20
    done
    return 1
}

build_with_gradle() {
    export JAVA_HOME="${SDKMAN_DIR:-$HOME/.sdkman}/candidates/java/25.0.4-tem"
    export PATH="$JAVA_HOME/bin:$PATH"
    local args="publishToMavenLocal -Pdiegetic.group=$GROUP.$ARTIFACT -Pversion=$VERSION"

    # NebsClient's only successful build during the outage ran these first; keep them as a warm-up
    sha256sum gradle/wrapper/gradle-wrapper.jar
    unzip -t gradle/wrapper/gradle-wrapper.jar | tail -1
    java -version

    echo "== build - wrapper in place"
    java -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain $args && return 0

    echo "== build - wrapper copied to HOME"
    mkdir -p "$HOME/diegetic-wrapper"
    cp gradle/wrapper/gradle-wrapper.* "$HOME/diegetic-wrapper/"
    java -cp "$HOME/diegetic-wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain $args && return 0

    echo "== build - SDKMAN gradle"
    source "${SDKMAN_DIR:-$HOME/.sdkman}/bin/sdkman-init.sh"
    sdk install gradle 9.7.0 && "${SDKMAN_DIR:-$HOME/.sdkman}/candidates/gradle/9.7.0/bin/gradle" $args
}

if [[ "$VERSION" =~ ^v[0-9]+\.[0-9]+\.[0-9]+ ]]; then
    install_release && exit 0
    echo "== no release artifacts for $VERSION, building instead"
fi
build_with_gradle
