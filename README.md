# Noar Mordomo V18.0.0 — painel nativo e voz ajustável

Esta versão abre uma **janela própria do Windows**, construída como aplicativo de desktop. O painel não é uma página do navegador.

## Melhorias

- Janela nativa para digitar comandos, visualizar respostas e acompanhar o que o reconhecimento de voz ouviu.
- Escuta contínua em segundo plano, com botão para pausar/retomar a qualquer momento.
- Controle de sensibilidade de 30% a 80%: mais sensível à esquerda, mais seletivo a ruídos à direita.
- Ajustes de tempo do reconhecedor e filtro de repetições rápidas da mesma transcrição.
- Respostas faladas usando o sintetizador de voz local do Windows, se disponível.
- Abertura de aplicativos pelo Menu Iniciar, ditado de texto na janela ativa, atalhos permitidos e informações do PC.
- Opção de iniciar junto com o Windows.

## Baixar e usar

Quando a compilação terminar, abra **Releases** deste repositório e baixe diretamente o instalador EXE. Instale por cima da versão anterior. O programa abre uma janela própria do Noar e não precisa abrir Chrome/Google para mostrar seu painel.

## Requisitos e limites

- O instalador inclui o runtime Java, mas não inclui o modelo GGUF nem o motor local do Noar V15.1. Sem um servidor de IA local, o Noar entende comandos conhecidos, mas não terá raciocínio livre para responder qualquer pergunta.
- A escuta requer reconhecimento de fala instalado no Windows, de preferência Português (Brasil), e microfone funcional.
- O controle de sensibilidade ajuda a equilibrar falas mais baixas e ruído ambiente; não garante reconhecimento perfeito.
- O áudio não é salvo em arquivos. O reconhecimento é feito pelo mecanismo de voz do Windows e apenas o texto reconhecido é enviado ao serviço local do Noar.
- Pesquisas e páginas externas continuam abrindo no navegador padrão, mas o painel principal do Noar não depende do navegador.
- O ditado envia texto para a janela ativa. Para ditar dentro de outro aplicativo, deixe o campo de destino ativo e use o Noar com a janela dele em segundo plano.
- Comandos de terminal, exclusões e alterações do Registro seguem bloqueados. Ações complexas dentro de qualquer aplicativo podem exigir integração específica.
- Os testes automáticos não substituem o teste real do microfone, idioma e sintetizador de voz no seu PC.

## Privacidade e segurança

O serviço local atende somente em `127.0.0.1:8770`. Não há gravação de áudio. O log técnico da escuta fica em `%LOCALAPPDATA%\NoarMordomo\voice-listener.log` e contém mensagens técnicas, não áudio. O repositório é público; nunca coloque tokens, senhas ou dados privados no código.
