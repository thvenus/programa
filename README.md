# Noar Mordomo — compilação automática para Windows

Projeto do Noar Mordomo com compilação de instalador Windows por GitHub Actions.

## O que inclui esta versão

- Interface de comandos local no navegador, servida apenas em \`127.0.0.1:8770\`.
- Abertura de Calculadora, Bloco de Notas, Explorador de Arquivos, Configurações e pastas comuns.
- Consulta básica de informações do computador.
- Pesquisa limitada de arquivos em Desktop, Documentos, Downloads e Imagens.
- Pesquisa na internet usando o navegador padrão.
- Criação de pastas dentro de \`Documentos\Noar\`.
- Controles para não executar comandos arbitrários de terminal nem apagar arquivos ou alterar o Registro.

## Como pegar o instalador EXE

Abra **Releases** neste repositório e baixe o arquivo EXE depois que um build terminar com sucesso. O fluxo publica um instalador individual, sem precisar baixar o ZIP do artefato.

A aba **Actions** mostra o resultado do build e os logs de compilação. Se o workflow falhar, ainda não há um instalador válido.

## Limitações

- **Não inclui o modelo GGUF ou o motor de IA local do Noar V15.1.** É a versão de assistente local de comandos.
- A voz depende do suporte do navegador e da permissão do microfone; o reconhecimento de fala pode depender da conexão com a internet.
- É uma versão experimental. Embora a compilação execute autotestes e um teste HTTP, ainda precisa ser validada no Windows do usuário.
- Esta é uma reconstrução focada em comandos de sistema para permitir o build; não é o pacote original V16 completo.
- O repositório é público, conforme autorizado pelo proprietário; o código-fonte pode ser visto por outras pessoas.

## Segurança

O servidor escuta somente em \`127.0.0.1\`, e os comandos disponíveis são uma lista restrita. Não coloque tokens, senhas, chaves ou arquivos pessoais neste repositório.
