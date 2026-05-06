{
  description = "PakkuDesktop - A desktop app for Minecraft modpack development";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs {
          inherit system;
          config.allowUnfree = true;
        };

        jdk = pkgs.jetbrains.jdk-no-jcef-17;
        
        runtimeLibs = with pkgs; [
          libGL
          libpulseaudio
          xorg.libX11
          xorg.libXext
          xorg.libXtst
          xorg.libXi
          xorg.libXrender
          xorg.libXrandr
          xorg.libXxf86vm
          alsa-lib
          fontconfig
          freetype
          zlib
        ];

        buildScript = pkgs.writeShellScriptBin "build-pakku" ''
          set -e
          export JAVA_HOME="${jdk}"
          export PATH="${jdk}/bin:${pkgs.gradle}/bin:$PATH"
          gradle --no-daemon packageUberJarForCurrentOS
          echo "Build complete! JAR file is in build/compose/jars/"
        '';

        runScript = pkgs.writeShellScriptBin "run-pakku" ''
          set -e
          export LD_LIBRARY_PATH="${pkgs.lib.makeLibraryPath runtimeLibs}:$LD_LIBRARY_PATH"
          
          JAR_PATH="build/compose/jars"
          if [ ! -d "$JAR_PATH" ]; then
            echo "Error: Build directory not found. Please run 'build-pakku' first."
            exit 1
          fi
          
          JAR_FILE=$(find "$JAR_PATH" -name "*.jar" -type f | head -n 1)
          
          if [ -z "$JAR_FILE" ]; then
            echo "Error: No JAR file found. Please run 'build-pakku' first."
            exit 1
          fi
          
          ${jdk}/bin/java -jar "$JAR_FILE"
        '';

      in
      {
        packages = {
          build-pakku = buildScript;
          run-pakku = runScript;
          default = buildScript;
        };

        devShells.default = pkgs.mkShell {
          buildInputs = with pkgs; [
            jdk
            gradle
            kotlin
          ] ++ runtimeLibs;

          shellHook = ''
            export JAVA_HOME="${jdk}"
            export PATH="${jdk}/bin:$PATH"
            export LD_LIBRARY_PATH="${pkgs.lib.makeLibraryPath runtimeLibs}:$LD_LIBRARY_PATH"
            export GRADLE_OPTS="-Dorg.gradle.java.home=${jdk}"
            
            
            if [ -z "$GITHUB_ACTOR" ] || [ -z "$GITHUB_TOKEN" ]; then
              echo "⚠️  GitHub credentials not set. Export GITHUB_ACTOR and GITHUB_TOKEN to download dependencies."
            fi
            
            if [ -f gradlew ]; then
              chmod +x gradlew
            fi
          '';
        };
      }
    );
}