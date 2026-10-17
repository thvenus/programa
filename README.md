# Noar Mordomo V18.1.0 — interface legível e microfone detectável

Esta versão melhora o painel nativo e adiciona busca, seleção e teste de microfone.

## Melhorias
- Cores consistentes de alto contraste em campos de texto, abas e caixas roláveis.
- Busca de dispositivos de entrada ativos por meio do Windows Core Audio, mostrando o nome amigável do dispositivo e qual está marcado como padrão.
- Seleção explícita de um microfone e pedido para torná-lo a entrada padrão do Windows em todos os perfis de áudio.
- Teste do microfone por até oito segundos, mostrando a frase que o Windows reconheceu ou a razão de não conseguir abrir o microfone.
- Estado da escuta e histórico das falas reconhecidas, controle de sensibilidade, pausa de microfone e início com Windows.

## Como usar
1. Instale o EXE V18.1.0 por cima da versão anterior.
2. Clique em **Procurar microfones** e escolha o dispositivo.
3. Clique em **Usar selecionado**.
4. Clique em **Testar microfone** e fale uma frase.
5. Caso não funcione, confira conexão, volume, permissões do microfone e reconhecimento de fala em Português (Brasil) nas configurações do Windows.

## Limitações
- Só aparecem dispositivos de captura ativos oferecidos pelo Windows; um dispositivo desconectado, desabilitado ou sem driver pode não aparecer.
- A seleção altera a entrada padrão do Windows e pode afetar outros aplicativos que usam o dispositivo padrão.
- A detecção de um dispositivo não garante que ele esteja recebendo som. O botão de teste usa `System.Speech` instalado no Windows e precisa de um reconhecedor de fala em português.
- O áudio não é gravado em arquivo; o teste mostra apenas o texto reconhecido.
- O instalador não inclui o modelo GGUF nem o motor local Noar V15.1. Perguntas abertas exigem servidor de IA local compatível.
- Os testes automáticos não substituem o teste real do microfone do usuário.

## Privacidade e segurança
O serviço local escuta apenas em `127.0.0.1:8770`. O repositório é público; nunca inclua tokens, senhas ou dados privados.
