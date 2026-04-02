{ inputs, lib, ... }:
{
  imports = [ inputs.devshell.flakeModule ];

  perSystem =
    {
      config,
      pkgs,
      inputs',
      ...
    }:
    {
      devshells.default = {
        devshell = {
          name = "Ghidra devshell";
          meta.description = "Ghidra development environment";
          packages =
            with pkgs;
            [
              jdk25_headless
              gradle_9
              (python313.withPackages (ps: [
                ps.langgraph
                ps.langchain-openai
                ps.langchain-core
                ps.uvicorn
                ps.pydantic
                ps.python-dotenv
                ps.fastapi
              ]))

            ]
            ++ [
              inputs'.nix-fast-build.packages.default
              config.treefmt.build.wrapper
            ]
            ++ lib.attrValues config.treefmt.build.programs;
        };

        env = [
          {
            name = "GHIDRA_INSTALL_DIR";
            value = "${pkgs.ghidra}/lib/ghidra";
          }
        ];
      };
    };
}
