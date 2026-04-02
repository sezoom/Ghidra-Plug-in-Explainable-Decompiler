{ inputs, ... }:
{
  imports = [ inputs.git-hooks-nix.flakeModule ];

  perSystem =
    {
      config,
      self',
      lib,
      ...
    }:
    {
      checks = lib.mapAttrs' (n: lib.nameValuePair "package-${n}") self'.packages;

      pre-commit.settings.hooks = {
        gofmt.enable = true;
        govet.enable = true;
        golangci-lint.enable = true;
        gotest.enable = true;
        treefmt = {
          enable = true;
          package = config.treefmt.build.wrapper;
        };
      };
    };
}
