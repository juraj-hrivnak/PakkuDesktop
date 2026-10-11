# Native libraries for Compose UI tests and `./gradlew run` on NixOS.
#
# Skiko and AWT dlopen X11, GL, and fontconfig by name. Nucleus Tao also
# dlopens GTK. `LD_LIBRARY_PATH` has to list those lib dirs; linker flags
# from a normal nix-shell are not enough. Packages here are 64-bit, so this
# does not pick up a 32-bit libXtst the way a /nix/store glob can.
#
# The libraries stay out of `packages` so the shell does not pull their
# headers and a compiler. Only JDK 25 is on PATH.
{ pkgs }:
let
  nativeLibs = with pkgs; [
    libx11
    libxext
    libxrender
    libxtst
    libxi
    libxrandr
    libxcursor
    libxxf86vm
    libxinerama
    libxfixes
    libxcb
    libglvnd
    fontconfig
    freetype
    zlib
    libxkbcommon
    libdbusmenu
    gtk3
    gdk-pixbuf
    glib
    cairo
    pango
    atk
    harfbuzz
    stdenv.cc.cc.lib
  ];
in
pkgs.mkShell {
  packages = [ pkgs.jdk25 ];

  shellHook = ''
    export JAVA_HOME="${pkgs.jdk25}"
    export LD_LIBRARY_PATH="${pkgs.lib.makeLibraryPath nativeLibs}''${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
  '';
}
