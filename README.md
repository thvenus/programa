# Noar Mordomo V17.0.1 — escuta contínua no Windows

Esta versão atualiza o Noar Mordomo para escutar continuamente pelo reconhecedor de fala do Windows, sem clicar em um botão para iniciar cada fala.

## O que faz

- Inicia o serviço local de reconhecimento de voz automaticamente quando o aplicativo abre.
- Na primeira execução do aplicativo instalado, configura um atalho na pasta Inicializar do usuário para abrir junto com o Windows. O painel permite desligar/ligar essa opção.
- Mostra um indicador de estado e permite pausar/retomar o microfone a qualquer momento.
- Reconhece comandos comuns sem botão e sem palavra de ativação, por exemplo abrir aplicativos/pastas, consultar estado, ditar texto e executar atalhos permitidos.
- Tenta abrir aplicativos instalados que tenham atalhos no Menu Iniciar ou na área de trabalho.
- Permite ditar texto para a janela que estiver ativa com "digite ...".
- Permite atalhos usando "pressione Enter", "pressione Tab", "pressione Esc", "pressione Ctrl+L", "pressione Ctrl+F", "pressione Ctrl+T" ou "pressione Alt+Tab".
- Para perguntas abertas, tenta usar um servidor de IA local compatível com a API OpenAI em `http://127.0.0.1:8080/v1/chat/completions` (por exemplo, llama.cpp) ou `http://127.0.0.1:1234/v1/chat/completions` (por exemplo, LM Studio). Também aceita a variável `NOAR_AI_ENDPOINT`, desde que a URL aponte para `127.0.0.1` ou `localhost`.

## Baixar o instalador

Quando o GitHub Actions terminar com sucesso, abra **Releases** deste repositório e baixe diretamente o instalador EXE. A aba **Actions** mostra o estado e os logs da compilação.

## Requisitos e limites

- O instalador inclui o runtime Java, mas não inclui o modelo GGUF nem o motor local do Noar V15.1. Sem um servidor de IA local, o Noar ainda consegue ouvir continuamente e executar os comandos conhecidos, mas não terá raciocínio livre para responder a qualquer pergunta.
- A escuta contínua requer o mecanismo de reconhecimento de fala do Windows em Português (Brasil). Se o painel indicar que ele está ausente, instale o recurso de voz em português (Brasil) nas Configurações de Idioma/Voz do Windows e reinicie o Noar.
- O áudio não é salvo em arquivos. A fala é transcrita pelo reconhecimento local do Windows e apenas o texto reconhecido é enviado ao próprio Noar no computador.
- O ditado usa a área de transferência temporariamente para colar texto na janela ativa e tenta restaurar o conteúdo anterior. Selecione primeiro o campo em que deseja digitar.
- Atalhos são restritos a uma lista permitida. Comandos de terminal, exclusão de arquivos, formatação e alterações no Registro seguem bloqueados.
- A abertura automática de aplicativos depende de existir um atalho correspondente no Menu Iniciar ou na área de trabalho. Ações complexas dentro de cada aplicativo exigem integrações específicas ou controle de interface mais avançado.
- É uma versão experimental: compilação e testes automáticos não substituem a validação do microfone, do idioma de voz e dos aplicativos no PC do usuário.

## Privacidade e segurança

O serviço web escuta somente em `127.0.0.1:8770`. Nenhum arquivo de áudio é gravado. O log técnico do mecanismo de voz fica em `%LOCALAPPDATA%\NoarMordomo\voice-listener.log` e contém mensagens técnicas, não gravações.

O repositório é público. Não coloque tokens, senhas ou dados pessoais no código.
