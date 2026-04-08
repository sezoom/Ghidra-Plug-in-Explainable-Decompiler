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
                ps.fastapi
                ps.google-ai-generativelanguage
                ps.google-api-core
                ps.google-auth
                ps.googleapis-common-protos
                ps.langchain
                ps.langchain-core
                ps.langchain-google-genai
                ps.langchain-openai
                ps.langchain-openai
                ps.langchain-text-splitters
                ps.langgraph
                ps.langgraph-checkpoint
                ps.langgraph-prebuilt
                ps.langgraph-sdk
                ps.langsmith
                ps.pydantic
                ps.python-dotenv
                ps.uvicorn
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
