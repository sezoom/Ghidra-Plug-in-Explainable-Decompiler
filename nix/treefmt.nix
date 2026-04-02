{ inputs, ... }:
{
  imports = [
    inputs.flake-root.flakeModule
    inputs.treefmt-nix.flakeModule
  ];

  perSystem =
    { config, pkgs, ... }:
    {
      treefmt.config = {
        package = pkgs.treefmt;
        inherit (config.flake-root) projectRootFile;

        programs = {
          nixfmt.enable = true;
          nixfmt.package = pkgs.nixfmt-rfc-style;
          deadnix.enable = true;
          statix.enable = true;
          shellcheck.enable = true;
          gofmt.enable = true;
        };

        settings.global.excludes = [
          ".git/*"
          ".github/*"
          ".direnv/*"
          ".envrc"
          "src/vendor/*"
        ];
      };

      formatter = config.treefmt.build.wrapper;
    };
}
