# `nix-shell` entry. Same environment as `nix develop`.
# The pin is flake.lock, so the two stay on one nixpkgs revision.
let
  lock = builtins.fromJSON (builtins.readFile ./flake.lock);
  src = lock.nodes.nixpkgs.locked;
  nixpkgs = fetchTarball {
    url = "https://github.com/NixOS/nixpkgs/archive/${src.rev}.tar.gz";
    sha256 = src.narHash;
  };
in
import ./nix/devshell.nix { pkgs = import nixpkgs { }; }
