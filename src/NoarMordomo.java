import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class NoarMordomo {
    private static final int PORT = 8770;
    private static final String HOST = "127.0.0.1";
    private static final String APP_NAME = "Noar Mordomo";
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final AtomicBoolean LISTENING_ENABLED = new AtomicBoolean(true);
    private static final AtomicBoolean SPEAKING = new AtomicBoolean(false);
    private static final CopyOnWriteArrayList<String> EVENTS = new CopyOnWriteArrayList<>();
    private static volatile String voiceStatus = "Iniciando escuta contínua...";
    private static volatile Process voiceProcess;
    private static volatile List<Path> installedShortcutsCache = List.of();
    private static volatile boolean installedShortcutsLoaded = false;

    private static final String PAGE = """
<!doctype html>
<html lang="pt-BR">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<meta name="color-scheme" content="dark">
<title>Noar Mordomo</title>
<style>
:root{color-scheme:dark;--bg:#0b1020;--panel:#131b30;--line:#293754;--text:#eef4ff;--muted:#a9b7d0;--blue:#6aa6ff;--good:#65d6a2}
*{box-sizing:border-box}body{margin:0;background:radial-gradient(ellipse at top,#182647 0,#0b1020 60%);font:16px/1.5 "Segoe UI",system-ui,sans-serif;color:var(--text);min-height:100vh;display:grid;place-items:center;padding:22px}
main{width:min(800px,100%)}header{display:flex;align-items:center;gap:15px;margin-bottom:20px}.logo{width:62px;height:62px;border-radius:20px;background:linear-gradient(145deg,#4389f6,#243b75);display:grid;place-items:center;font-size:30px;box-shadow:0 12px 32px #0005}
h1{font-size:clamp(25px,5vw,36px);margin:0;letter-spacing:.3px}header p{margin:4px 0 0;color:var(--muted)}
.card{background:#131b30ee;border:1px solid var(--line);border-radius:22px;padding:22px;box-shadow:0 18px 60px #0004;margin-bottom:14px}
.status{display:flex;align-items:center;gap:8px;color:var(--muted);font-size:14px}.dot{width:9px;height:9px;border-radius:50%;background:var(--good);box-shadow:0 0 12px #65d6a277}
label{display:block;font-weight:600;margin-bottom:10px}textarea{width:100%;min-height:94px;resize:vertical;background:#0c1324;border:1px solid #354565;border-radius:13px;padding:14px;color:var(--text);font:inherit;outline:none}textarea:focus{border-color:var(--blue);box-shadow:0 0 0 3px #6aa6ff22}
.buttons{display:flex;flex-wrap:wrap;gap:10px;margin-top:12px}button{border:1px solid #385174;border-radius:12px;background:#1c2a46;color:var(--text);font:600 14px/1.2 inherit;padding:12px 15px;cursor:pointer}button:hover{filter:brightness(1.17)}button.primary{background:linear-gradient(145deg,#3e83ed,#2d5fba);border-color:#6aa6ff}button:disabled{opacity:.5;cursor:wait}
.answer{white-space:pre-wrap;overflow-wrap:anywhere;min-height:55px;border-radius:12px;background:#0b1222;padding:14px;margin-top:15px;color:#d8e5fc}.quick{display:flex;flex-wrap:wrap;gap:8px;margin-top:12px}.quick button{background:#101a2e;font-weight:500;color:#c6d8f5}.foot{color:#8294b6;font-size:12px;text-align:center;padding:8px 10px}.warn{color:#ffd89a}
</style>
</head>
<body><main>
<header><div class="logo" aria-hidden="true">✦</div><div><h1>Noar Mordomo</h1><p>Seu assistente local para Windows</p></div></header>
<section class="card">
<div class="status"><span class="dot"></span><span id="statusLine">Conectado ao assistente local</span></div>
<p>O microfone fica <b>escutando continuamente</b>, mesmo quando você está usando outros programas. Exemplo: “abrir Chrome”, “digite olá” ou “Noar, me explique isso”.</p>
<label for="command">O que você quer que eu faça?</label>
<textarea id="command" rows="3" placeholder="Digite aqui ou fale um comando a qualquer momento..."></textarea>
<div class="buttons"><button class="primary" id="send">Executar comando digitado</button><button id="listenToggle">Pausar microfone</button><button id="autostartToggle">Início com Windows</button><button id="system">Informações do PC</button></div>
<div class="quick">
<button data-cmd="abrir calculadora">Calculadora</button><button data-cmd="abrir bloco de notas">Bloco de Notas</button><button data-cmd="abrir explorador de arquivos">Explorador</button><button data-cmd="abrir downloads">Downloads</button><button data-cmd="abrir documentos">Documentos</button>
</div>
<div class="answer" id="answer" role="status" aria-live="polite">Olá! Sou o Noar. Como posso ajudar?</div>
</section>
<section class="card">
<b>Comandos disponíveis</b>
<p>O Noar pode abrir aplicativos instalados que tenham atalhos no Menu Iniciar, ditar texto na janela ativa, pressionar atalhos de teclado e consultar o computador. Para perguntas abertas, pode usar um motor de IA local compatível rodando em <code>127.0.0.1:8080</code> ou <code>127.0.0.1:1234</code>.</p>
<p class="warn">O microfone não grava arquivos de áudio: o reconhecimento é feito pelo mecanismo de voz do Windows e apenas o texto reconhecido é enviado ao Noar local. Você pode pausar a escuta. Comandos de terminal, exclusões e alterações do Registro continuam bloqueados.</p>
</section>
<div class="foot">Escuta contínua pelo Windows Speech. O reconhecimento exige o recurso de voz em português (Brasil) instalado no Windows. O serviço local não grava áudio.</div>
</main>
<script>
const $=id=>document.getElementById(id);
let lastSpokenEvent=''; let eventsInitialized=false;
function speakText(text){
  if(!text || !('speechSynthesis' in window)) return;
  fetch('/api/speaking',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8'},body:new URLSearchParams({active:'true'})}).catch(()=>{});
  speechSynthesis.cancel();
  const u=new SpeechSynthesisUtterance(text.slice(0,260)); u.lang='pt-BR';
  const finish=()=>fetch('/api/speaking',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8'},body:new URLSearchParams({active:'false'})}).catch(()=>{});
  u.onend=finish; u.onerror=finish; speechSynthesis.speak(u);
}
async function execute(command){
  if(!command.trim()){ $('answer').textContent='Digite um comando primeiro.'; return; }
  $('send').disabled=true; $('answer').textContent='Processando...';
  try{
    const response=await fetch('/api/command',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8'},body:new URLSearchParams({command})});
    const data=await response.json(); $('answer').textContent=data.message||'Comando recebido.';
    if(data.ok && data.speak!==false) speakText(data.message||'Pronto');
  }catch(e){$('answer').textContent='Não consegui falar com o Noar. Feche e abra o programa novamente.';}
  finally{$('send').disabled=false;}
}
async function refreshStatus(){
  try{
    const s=await (await fetch('/api/status')).json();
    $('statusLine').textContent=s.voiceStatus+' · '+(s.listening?'microfone ligado':'microfone pausado');
    $('listenToggle').dataset.active=String(s.listening);
    $('listenToggle').textContent=s.listening?'Pausar microfone':'Retomar microfone';
    $('autostartToggle').dataset.enabled=String(s.autostart);
    $('autostartToggle').textContent=s.autostart?'Início com Windows: ligado':'Início com Windows: desligado';
  }catch(e){$('statusLine').textContent='Conectando ao serviço local...';}
}
async function refreshEvents(){
  try{
    const d=await (await fetch('/api/events')).json(); const events=d.events||[];
    if(events.length) $('answer').textContent=events.slice(-12).join('\n');
    const speech=events.filter(x=>x.startsWith('FALAR: ')).at(-1);
    if(!eventsInitialized){lastSpokenEvent=speech||'';eventsInitialized=true;}
    else if(speech && speech!==lastSpokenEvent){lastSpokenEvent=speech;speakText(speech.slice(7));}
  }catch(e){}
}
$('send').addEventListener('click',()=>execute($('command').value));
$('command').addEventListener('keydown',e=>{if(e.key==='Enter'&&!e.shiftKey){e.preventDefault();execute($('command').value);}});
$('system').addEventListener('click',()=>execute('informações do sistema'));
$('listenToggle').addEventListener('click',async()=>{
 const enabled=$('listenToggle').dataset.active!=='true';
 await fetch('/api/listening',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8'},body:new URLSearchParams({active:String(enabled)})});
 refreshStatus();
});
$('autostartToggle').addEventListener('click',async()=>{
 const enabled=$('autostartToggle').dataset.enabled!=='true';
 const r=await fetch('/api/autostart',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8'},body:new URLSearchParams({active:String(enabled)})});
 const d=await r.json(); $('answer').textContent=d.message; refreshStatus();
});
document.querySelectorAll('[data-cmd]').forEach(b=>b.addEventListener('click',()=>{ $('command').value=b.dataset.cmd; execute(b.dataset.cmd); }));
refreshStatus(); refreshEvents();
setInterval(refreshStatus,1800); setInterval(refreshEvents,1100);
</script></body></html>
""";

    private enum Intent {
        EMPTY, STATUS, CALCULATOR, NOTEPAD, EXPLORER, SETTINGS,
        DOWNLOADS, DOCUMENTS, DESKTOP, SEARCH_FILES, SEARCH_WEB,
        CREATE_FOLDER, TYPE_TEXT, PRESS_KEY, BLOCKED, UNKNOWN
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("--self-test")) {
            runSelfTests();
            return;
        }
        boolean noBrowser = List.of(args).contains("--no-browser");
        boolean noVoice = List.of(args).contains("--no-voice");
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName(HOST), PORT), 0);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.createContext("/", NoarMordomo::handle);
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(NoarMordomo::stopVoiceListener));
        if (!noVoice) startVoiceListener();
        if (isPackagedWindowsApp() && !isAutoStartEnabled()) {
            boolean enabled = setAutoStart(true);
            if (enabled) addEvent("SISTEMA: Noar configurado para iniciar com o Windows e escutar continuamente.");
        }
        URI uri = URI.create("http://" + HOST + ":" + PORT + "/");
        System.out.println(APP_NAME + " ativo em " + uri);
        if (!noBrowser) {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(uri);
                } else {
                    System.out.println("Abra manualmente: " + uri);
                }
            } catch (Exception ex) {
                System.out.println("Abra manualmente: " + uri);
            }
        }
    }

    private static void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            String path = exchange.getRequestURI().getPath();
            if ("/".equals(path) && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                send(exchange, 200, "text/html; charset=utf-8", PAGE);
                return;
            }
            if ("/api/status".equals(path) && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 200, statusJson());
                return;
            }
            if ("/api/voice-control".equals(path) && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 200, voiceControlJson());
                return;
            }
            if ("/api/events".equals(path) && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 200, eventsJson());
                return;
            }
            if ("/api/voice-status".equals(path) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readNBytes(4096), StandardCharsets.UTF_8);
                voiceStatus = parseForm(body).getOrDefault("status", "Status de voz não informado.");
                sendJson(exchange, 200, "{\"ok\":true}");
                return;
            }
            if ("/api/listening".equals(path) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                if (!trustedOrigin(exchange)) {
                    sendJson(exchange, 403, "{\"ok\":false,\"message\":\"Origem não permitida.\"}");
                    return;
                }
                Map<String, String> form = parseForm(new String(exchange.getRequestBody().readNBytes(4096), StandardCharsets.UTF_8));
                boolean active = Boolean.parseBoolean(form.getOrDefault("active", "true"));
                LISTENING_ENABLED.set(active);
                voiceStatus = active ? "Solicitando escuta contínua..." : "Microfone pausado pelo usuário.";
                addEvent(active ? "SISTEMA: escuta ativada." : "SISTEMA: escuta pausada pelo usuário.");
                sendJson(exchange, 200, voiceControlJson());
                return;
            }
            if ("/api/speaking".equals(path) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> form = parseForm(new String(exchange.getRequestBody().readNBytes(4096), StandardCharsets.UTF_8));
                SPEAKING.set(Boolean.parseBoolean(form.getOrDefault("active", "false")));
                sendJson(exchange, 200, voiceControlJson());
                return;
            }
            if ("/api/autostart".equals(path) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                if (!trustedOrigin(exchange)) {
                    sendJson(exchange, 403, "{\"ok\":false,\"message\":\"Origem não permitida.\"}");
                    return;
                }
                Map<String, String> form = parseForm(new String(exchange.getRequestBody().readNBytes(4096), StandardCharsets.UTF_8));
                boolean active = Boolean.parseBoolean(form.getOrDefault("active", "false"));
                boolean success = setAutoStart(active);
                String message = success
                        ? (active ? "O Noar será iniciado junto com o Windows." : "A inicialização automática foi desativada.")
                        : "Não consegui alterar a inicialização automática nesta instalação.";
                sendJson(exchange, success ? 200 : 500,
                        "{\"ok\":" + success + ",\"autostart\":" + isAutoStartEnabled() +
                                ",\"message\":\"" + jsonEscape(message) + "\"}");
                return;
            }
            if ("/api/voice".equals(path) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                if (!trustedOrigin(exchange)) {
                    sendJson(exchange, 403, "{\"ok\":false,\"message\":\"Origem não permitida.\"}");
                    return;
                }
                byte[] body = exchange.getRequestBody().readNBytes(8193);
                if (body.length > 8192) {
                    sendJson(exchange, 413, "{\"ok\":false,\"message\":\"Fala reconhecida muito longa.\"}");
                    return;
                }
                String spoken = parseForm(new String(body, StandardCharsets.UTF_8)).getOrDefault("command", "");
                handleVoiceCommand(spoken);
                sendJson(exchange, 200, "{\"ok\":true}");
                return;
            }
            if ("/api/command".equals(path) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                if (!trustedOrigin(exchange)) {
                    sendJson(exchange, 403, "{\"ok\":false,\"message\":\"Origem não permitida.\"}");
                    return;
                }
                byte[] body = exchange.getRequestBody().readNBytes(8193);
                if (body.length > 8192) {
                    sendJson(exchange, 413, "{\"ok\":false,\"message\":\"Comando muito longo.\"}");
                    return;
                }
                String command = parseForm(new String(body, StandardCharsets.UTF_8)).getOrDefault("command", "");
                CommandResult result = execute(command);
                sendJson(exchange, result.ok ? 200 : 400,
                        "{\"ok\":" + result.ok + ",\"message\":\"" + jsonEscape(result.message) + "\"}");
                return;
            }
            send(exchange, 404, "text/plain; charset=utf-8", "Não encontrado.");
        } catch (Exception ex) {
            try {
                sendJson(exchange, 500, "{\"ok\":false,\"message\":\"Erro interno ao processar o pedido.\"}");
            } catch (Exception ignored) {
                // The connection may already be closed.
            }
        }
    }

    private record CommandResult(boolean ok, String message) {}

    private static CommandResult execute(String original) {
        if (original == null || original.isBlank()) {
            return new CommandResult(false, "Digite um comando para eu ajudar.");
        }
        Intent intent = classify(original);
        String text = normalize(original);
        try {
            return switch (intent) {
                case STATUS -> new CommandResult(true, statusText());
                case CALCULATOR -> launch(List.of("calc.exe"), "Abrindo a Calculadora.");
                case NOTEPAD -> launch(List.of("notepad.exe"), "Abrindo o Bloco de Notas.");
                case EXPLORER -> launch(List.of("explorer.exe"), "Abrindo o Explorador de Arquivos.");
                case SETTINGS -> launch(List.of("explorer.exe", "ms-settings:"), "Abrindo as Configurações do Windows.");
                case DOWNLOADS -> openDirectory(Path.of(System.getProperty("user.home"), "Downloads"), "Downloads");
                case DOCUMENTS -> openDirectory(documentsPath(), "Documentos");
                case DESKTOP -> openDirectory(Path.of(System.getProperty("user.home"), "Desktop"), "Área de Trabalho");
                case SEARCH_FILES -> searchFiles(extractTail(original, List.of(
                        "pesquisar arquivos", "pesquisar arquivo", "buscar arquivos", "buscar arquivo",
                        "procurar arquivos", "procurar arquivo")));
                case SEARCH_WEB -> openWebSearch(extractWebQuery(original));
                case CREATE_FOLDER -> createFolder(extractTail(original, List.of(
                        "criar pasta", "crie pasta", "crie uma pasta", "criar uma pasta")));
                case TYPE_TEXT -> typeText(extractTail(original, List.of("digite ", "escreva ", "escreve ", "dite ", "ditar ")));
                case PRESS_KEY -> pressKey(extractTail(original, List.of("pressione tecla ", "pressione ", "aperte tecla ", "aperte ", "aperta ", "tecla ")));
                case BLOCKED -> new CommandResult(false,
                        "Por segurança, não executo comandos arbitrários nem ações destrutivas. Posso abrir aplicativos, pastas e pesquisas.");
                case EMPTY -> new CommandResult(false, "Digite um comando para eu ajudar.");
                case UNKNOWN -> {
                    CommandResult app = tryLaunchInstalledApp(original);
                    if (app.ok) yield app;
                    String answer = askLocalAI(original);
                    if (answer != null) yield new CommandResult(true, answer);
                    yield new CommandResult(false,
                            "Ainda não reconheço essa ação. Posso abrir aplicativos pelo nome do Menu Iniciar, ditar texto na janela ativa e pressionar atalhos de teclado. Perguntas abertas precisam de um motor de IA local ligado em 127.0.0.1:8080 ou 127.0.0.1:1234.");
                }
            };
        } catch (Exception ex) {
            return new CommandResult(false, "Não consegui concluir a ação. Verifique se o aplicativo ou a pasta está disponível.");
        }
    }

    private static Intent classify(String original) {
        String text = normalize(original);
        if (text.isBlank()) return Intent.EMPTY;

        if (startsAny(text, "apagar ", "apague ", "excluir ", "exclua ", "deletar ", "delete ",
                "formatar ", "formate ", "desligar ", "desligue ", "reiniciar ", "reinicie ",
                "executar comando", "executar powershell", "abrir powershell", "abrir cmd",
                "rodar comando", "terminal ")) return Intent.BLOCKED;

        if (text.equals("status") || text.contains("informacoes do sistema") || text.contains("informacoes do pc")
                || text.contains("como esta o pc") || text.equals("status do pc") || text.contains("estado do computador")) return Intent.STATUS;
        if (text.contains("calculadora") || text.contains("calculator") || text.equals("abrir calc")) return Intent.CALCULATOR;
        if (text.contains("bloco de notas") || text.contains("notepad")) return Intent.NOTEPAD;
        if (text.contains("explorador de arquivos") || text.equals("abrir explorer") || text.equals("abrir explorador")) return Intent.EXPLORER;
        if (text.contains("configuracoes do windows") || text.equals("abrir configuracoes") || text.equals("configuracoes")) return Intent.SETTINGS;
        if (text.contains("downloads") && (text.startsWith("abrir") || text.startsWith("ir para") || text.startsWith("mostrar"))) return Intent.DOWNLOADS;
        if (text.contains("documentos") && (text.startsWith("abrir") || text.startsWith("ir para") || text.startsWith("mostrar"))) return Intent.DOCUMENTS;
        if ((text.contains("area de trabalho") || text.contains("desktop")) && (text.startsWith("abrir") || text.startsWith("ir para") || text.startsWith("mostrar"))) return Intent.DESKTOP;
        if (startsAny(text, "pesquisar arquivos", "pesquisar arquivo", "buscar arquivos", "buscar arquivo",
                "procurar arquivos", "procurar arquivo")) return Intent.SEARCH_FILES;
        if (startsAny(text, "pesquisar na internet", "buscar na internet", "pesquisar no google",
                "pesquisar web", "pesquise ", "pesquisar:")) return Intent.SEARCH_WEB;
        if (startsAny(text, "criar pasta", "crie pasta", "crie uma pasta", "criar uma pasta")) return Intent.CREATE_FOLDER;
        if (startsAny(text, "digite ", "escreva ", "escreve ", "dite ", "ditar ")) return Intent.TYPE_TEXT;
        if (startsAny(text, "pressione ", "pressione tecla ", "aperte ", "aperte tecla ", "aperta ", "tecla ")) return Intent.PRESS_KEY;
        return Intent.UNKNOWN;
    }

    private static CommandResult typeText(String value) {
        if (value == null || value.isBlank()) return new CommandResult(false, "Diga o texto depois de “digite”.");
        if (GraphicsEnvironment.isHeadless()) return new CommandResult(false, "Não há uma área de trabalho ativa para digitar texto.");
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable previous = null;
        try {
            previous = clipboard.getContents(null);
            clipboard.setContents(new StringSelection(value), null);
            Robot robot = new Robot();
            robot.setAutoDelay(35);
            robot.delay(250);
            robot.keyPress(KeyEvent.VK_CONTROL);
            robot.keyPress(KeyEvent.VK_V);
            robot.keyRelease(KeyEvent.VK_V);
            robot.keyRelease(KeyEvent.VK_CONTROL);
            robot.delay(350);
            if (previous != null) {
                try { clipboard.setContents(previous, null); } catch (IllegalStateException ignored) { }
            }
            return new CommandResult(true, "Texto enviado para a janela ativa.");
        } catch (Exception ex) {
            if (previous != null) {
                try { clipboard.setContents(previous, null); } catch (Exception ignored) { }
            }
            return new CommandResult(false, "Não consegui digitar no aplicativo ativo. Clique primeiro no campo de texto e tente novamente.");
        }
    }

    private static CommandResult pressKey(String requested) {
        String key = normalize(requested).replace(" ", "");
        int keyCode;
        boolean control = false;
        boolean alt = false;
        switch (key) {
            case "enter", "entrar" -> keyCode = KeyEvent.VK_ENTER;
            case "tab" -> keyCode = KeyEvent.VK_TAB;
            case "esc", "escape" -> keyCode = KeyEvent.VK_ESCAPE;
            case "espaco", "space" -> keyCode = KeyEvent.VK_SPACE;
            case "backspace", "retrocesso" -> keyCode = KeyEvent.VK_BACK_SPACE;
            case "ctrl+l", "control+l" -> { keyCode = KeyEvent.VK_L; control = true; }
            case "ctrl+f", "control+f" -> { keyCode = KeyEvent.VK_F; control = true; }
            case "ctrl+t", "control+t" -> { keyCode = KeyEvent.VK_T; control = true; }
            case "ctrl+c", "control+c" -> { keyCode = KeyEvent.VK_C; control = true; }
            case "ctrl+v", "control+v" -> { keyCode = KeyEvent.VK_V; control = true; }
            case "ctrl+a", "control+a" -> { keyCode = KeyEvent.VK_A; control = true; }
            case "alt+tab" -> { keyCode = KeyEvent.VK_TAB; alt = true; }
            case "setaparacima", "cima", "up" -> keyCode = KeyEvent.VK_UP;
            case "setaparabaixo", "baixo", "down" -> keyCode = KeyEvent.VK_DOWN;
            case "setaparaesquerda", "esquerda", "left" -> keyCode = KeyEvent.VK_LEFT;
            case "setaparadireita", "direita", "right" -> keyCode = KeyEvent.VK_RIGHT;
            default -> { return new CommandResult(false, "Atalho não permitido. Tente Enter, Tab, Esc, Espaço, Backspace, Ctrl+L, Ctrl+F, Ctrl+T ou Alt+Tab."); }
        }
        if (GraphicsEnvironment.isHeadless()) return new CommandResult(false, "Não há uma janela ativa para enviar o atalho.");
        try {
            Robot robot = new Robot();
            robot.setAutoDelay(35);
            if (control) robot.keyPress(KeyEvent.VK_CONTROL);
            if (alt) robot.keyPress(KeyEvent.VK_ALT);
            robot.keyPress(keyCode);
            robot.keyRelease(keyCode);
            if (alt) robot.keyRelease(KeyEvent.VK_ALT);
            if (control) robot.keyRelease(KeyEvent.VK_CONTROL);
            return new CommandResult(true, "Atalho de teclado enviado para a janela ativa.");
        } catch (Exception ex) {
            return new CommandResult(false, "Não consegui enviar esse atalho para a janela ativa.");
        }
    }

    private static String extractAppLaunchTarget(String original) {
        String normalized = normalize(original);
        List<String> prefixes = List.of("abrir o aplicativo ", "abra o aplicativo ", "abra o ", "abrir o ",
                "abre o ", "iniciar o ", "inicie o ", "executar o ", "execute o ",
                "abrir ", "abra ", "abre ", "iniciar ", "inicie ", "executar ", "execute ", "rode ", "rodar ", "open ");
        for (String prefix : prefixes) {
            if (normalized.startsWith(prefix)) {
                int idx = original.toLowerCase(Locale.ROOT).indexOf(prefix);
                if (idx < 0) idx = 0;
                String target = original.substring(Math.min(original.length(), idx + prefix.length())).trim();
                String nt = normalize(target);
                if (startsAny(nt, "powershell", "cmd", "prompt de comando", "terminal", "regedit", "editor do registro")) return null;
                return target.replaceAll("^[,.:; ]+|[,.:; ]+$", "");
            }
        }
        return null;
    }

    private static CommandResult tryLaunchInstalledApp(String original) {
        String target = extractAppLaunchTarget(original);
        if (target == null || target.isBlank()) return new CommandResult(false, "");
        String needle = normalize(target);
        if (needle.length() > 80) return new CommandResult(false, "Nome de aplicativo muito longo.");
        try {
            List<Path> shortcuts = installedShortcuts();
            Path best = null;
            Path exact = null;
            int bestScore = 0;
            boolean ambiguous = false;
            for (Path path : shortcuts) {
                String filename = path.getFileName().toString().replaceFirst("(?i)\\.(lnk|appref-ms)$", "");
                String name = normalize(filename);
                int score = name.equals(needle) ? 100 : name.startsWith(needle) ? 70 : name.contains(needle) ? 40 : 0;
                if (score > bestScore) {
                    bestScore = score; best = path; exact = score == 100 ? path : null; ambiguous = false;
                } else if (score > 0 && score == bestScore && best != null && !path.equals(best)) ambiguous = true;
            }
            if (best == null || bestScore < 40)
                return new CommandResult(false, "Não encontrei um atalho para “" + target + "” no Menu Iniciar ou na área de trabalho.");
            if (ambiguous && exact == null)
                return new CommandResult(false, "Encontrei mais de um aplicativo parecido com “" + target + "”. Use o nome mais completo.");
            Path selected = exact != null ? exact : best;
            new ProcessBuilder("explorer.exe", selected.toString()).start();
            return new CommandResult(true, "Abrindo " + selected.getFileName().toString().replaceFirst("(?i)\\.(lnk|appref-ms)$", "") + ".");
        } catch (Exception ex) {
            return new CommandResult(false, "Não consegui abrir o aplicativo “" + target + "”.");
        }
    }

    private static List<Path> installedShortcuts() {
        if (installedShortcutsLoaded) return installedShortcutsCache;
        synchronized (NoarMordomo.class) {
            if (installedShortcutsLoaded) return installedShortcutsCache;
            List<Path> found = new ArrayList<>();
            String appData = System.getenv("APPDATA");
            String programData = System.getenv("ProgramData");
            List<Path> roots = new ArrayList<>();
            if (appData != null) roots.add(Path.of(appData, "Microsoft", "Windows", "Start Menu", "Programs"));
            if (programData != null) roots.add(Path.of(programData, "Microsoft", "Windows", "Start Menu", "Programs"));
            Path home = Path.of(System.getProperty("user.home"));
            roots.add(home.resolve("Desktop"));
            if (programData != null) roots.add(Path.of(programData, "Desktop"));
            for (Path root : roots) {
                if (!Files.isDirectory(root)) continue;
                try (Stream<Path> stream = Files.walk(root, 6)) {
                    stream.filter(Files::isRegularFile)
                            .filter(p -> {
                                String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
                                return name.endsWith(".lnk") || name.endsWith(".appref-ms");
                            })
                            .limit(5000).forEach(found::add);
                } catch (IOException ignored) { }
            }
            installedShortcutsCache = List.copyOf(found);
            installedShortcutsLoaded = true;
            return installedShortcutsCache;
        }
    }

    private static boolean startsWithWakeWord(String text) {
        String normalized = normalize(text);
        return normalized.equals("noar") || startsAny(normalized, "noar ", "noar,", "noar:", "noar;", "noar.", "noar!");
    }

    private static String removeWakeWord(String text) {
        if (!startsWithWakeWord(text)) return text.trim();
        int index = text.toLowerCase(Locale.ROOT).indexOf("noar");
        return index < 0 ? text.trim() : text.substring(index + 4).replaceFirst("^[,.:;!? ]+", "").trim();
    }

    private static void handleVoiceCommand(String recognized) {
        String spoken = recognized == null ? "" : recognized.trim();
        if (spoken.isBlank()) return;
        addEvent("OUVI: " + spoken);
        boolean addressed = startsWithWakeWord(spoken);
        String command = removeWakeWord(spoken);
        if (command.isBlank()) return;
        Intent intent = classify(command);
        CommandResult result;
        if (intent == Intent.UNKNOWN) {
            result = tryLaunchInstalledApp(command);
            if (result.ok) {
                addEvent("AÇÃO: " + result.message);
                addEvent("FALAR: " + result.message);
                return;
            }
            if (addressed) {
                String answer = askLocalAI(command);
                if (answer == null || answer.isBlank()) answer = "Estou escutando continuamente, mas ainda não encontrei um cérebro de IA local. Conecte o motor local Noar em 127.0.0.1:8080 ou 127.0.0.1:1234 para responder perguntas abertas.";
                addEvent("NOAR: " + answer);
                addEvent("FALAR: " + answer);
            }
            return;
        }
        result = execute(command);
        if (result.ok) {
            addEvent("AÇÃO: " + result.message);
            addEvent("FALAR: " + (intent == Intent.STATUS ? "As informações do computador estão na tela." : result.message));
        } else {
            addEvent("BLOQUEADO/ERRO: " + result.message);
            addEvent("FALAR: " + result.message);
        }
    }

    private static String askLocalAI(String prompt) {
        String configured = System.getenv("NOAR_AI_ENDPOINT");
        List<String> endpoints = new ArrayList<>();
        if (configured != null && !configured.isBlank()) endpoints.add(configured.trim());
        endpoints.add("http://127.0.0.1:8080/v1/chat/completions");
        endpoints.add("http://127.0.0.1:1234/v1/chat/completions");
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(700)).build();
        String clipped = prompt.length() > 1800 ? prompt.substring(0, 1800) : prompt;
        String payload = "{\"model\":\"local-model\",\"messages\":[{\"role\":\"system\",\"content\":\"Você é Noar, um assistente local em português do Brasil. Seja útil, direto e honesto. Não afirme ter executado ações que não executou. Para operações do computador, explique limites e peça confirmação antes de ações destrutivas.\"},{\"role\":\"user\",\"content\":\"" +
                jsonEscape(clipped) + "\"}],\"temperature\":0.4,\"max_tokens\":220}";
        for (String endpoint : endpoints) {
            try {
                URI uri = URI.create(endpoint);
                if (!"127.0.0.1".equals(uri.getHost()) && !"localhost".equalsIgnoreCase(uri.getHost())) continue;
                HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(18))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8)).build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    String answer = extractJsonString(response.body(), "content");
                    if (answer != null && !answer.isBlank()) return answer.trim();
                }
            } catch (Exception ignored) { }
        }
        return null;
    }

    private static String extractJsonString(String json, String key) {
        if (json == null) return null;
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) return null;
        String raw = matcher.group(1);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '\\' && i + 1 < raw.length()) {
                char n = raw.charAt(++i);
                switch (n) {
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case '"' -> out.append('"');
                    case '\\' -> out.append('\\');
                    case '/' -> out.append('/');
                    default -> out.append(n);
                }
            } else out.append(c);
        }
        return out.toString();
    }

    private static boolean isPackagedWindowsApp() {
        String command = ProcessHandle.current().info().command().orElse("");
        if (command.isBlank()) return false;
        try { return Path.of(command).getFileName().toString().equalsIgnoreCase("NoarMordomo.exe"); }
        catch (Exception ignored) { return false; }
    }

    private static Path startupShortcutPath() {
        String appData = System.getenv("APPDATA");
        if (appData == null || appData.isBlank()) return null;
        return Path.of(appData, "Microsoft", "Windows", "Start Menu", "Programs", "Startup", "Noar Mordomo.lnk");
    }

    private static boolean isAutoStartEnabled() {
        Path shortcut = startupShortcutPath();
        return shortcut != null && Files.exists(shortcut);
    }

    private static boolean setAutoStart(boolean active) {
        Path shortcut = startupShortcutPath();
        if (shortcut == null) return false;
        try {
            if (!active) {
                Files.deleteIfExists(shortcut);
                return true;
            }
            String command = ProcessHandle.current().info().command().orElse("");
            if (command.isBlank() || !Path.of(command).getFileName().toString().equalsIgnoreCase("NoarMordomo.exe")) return false;
            Path exe = Path.of(command).toAbsolutePath().normalize();
            Files.createDirectories(shortcut.getParent());
            String ps = "$w=New-Object -ComObject WScript.Shell; $s=$w.CreateShortcut(" + psQuote(shortcut.toString()) +
                    "); $s.TargetPath=" + psQuote(exe.toString()) + "; $s.WorkingDirectory=" + psQuote(exe.getParent().toString()) + "; $s.Save()";
            String encoded = Base64.getEncoder().encodeToString(ps.getBytes(StandardCharsets.UTF_16LE));
            Process proc = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-EncodedCommand", encoded)
                    .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
            if (!proc.waitFor(8, TimeUnit.SECONDS)) { proc.destroyForcibly(); return false; }
            return proc.exitValue() == 0 && Files.exists(shortcut);
        } catch (Exception ex) { return false; }
    }

    private static String psQuote(String input) { return "'" + input.replace("'", "''") + "'"; }

    private static Path locateVoiceScript() {
        String cp = System.getProperty("java.class.path", "");
        for (String entry : cp.split(Pattern.quote(File.pathSeparator))) {
            try {
                Path p = Path.of(entry).toAbsolutePath().normalize();
                Path dir = Files.isDirectory(p) ? p : p.getParent();
                if (dir != null) {
                    Path candidate = dir.resolve("NoarVoiceListener.ps1");
                    if (Files.isRegularFile(candidate)) return candidate;
                }
            } catch (Exception ignored) { }
        }
        Path fallback = Path.of("NoarVoiceListener.ps1").toAbsolutePath().normalize();
        return Files.isRegularFile(fallback) ? fallback : null;
    }

    private static void startVoiceListener() {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("windows")) {
            voiceStatus = "Escuta contínua disponível somente no Windows.";
            addEvent("SISTEMA: escuta por voz requer Windows.");
            return;
        }
        Path script = locateVoiceScript();
        if (script == null) {
            voiceStatus = "Script de voz contínua não encontrado.";
            addEvent("ERRO: faltou NoarVoiceListener.ps1 junto ao aplicativo.");
            return;
        }
        try {
            String local = System.getenv("LOCALAPPDATA");
            Path log = local == null ? Path.of(System.getProperty("user.home"), "noar-voice.log") :
                    Path.of(local, "NoarMordomo", "voice-listener.log");
            Files.createDirectories(log.getParent());
            voiceProcess = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass",
                    "-WindowStyle", "Hidden", "-File", script.toString(), "-ApiRoot", "http://" + HOST + ":" + PORT + "/api")
                    .redirectErrorStream(true).redirectOutput(log.toFile()).start();
            voiceStatus = "Microfone iniciando; escuta contínua ativa.";
            addEvent("SISTEMA: serviço de voz contínua iniciado. Log técnico em " + log);
        } catch (Exception ex) {
            voiceStatus = "Não consegui iniciar a escuta contínua do Windows.";
            addEvent("ERRO: não foi possível iniciar o serviço de voz.");
        }
    }

    private static void stopVoiceListener() {
        Process current = voiceProcess;
        if (current != null && current.isAlive()) current.destroyForcibly();
    }

    private static String eventsJson() {
        StringBuilder out = new StringBuilder("{\"events\":[");
        for (int i = 0; i < EVENTS.size(); i++) {
            if (i > 0) out.append(',');
            out.append('"').append(jsonEscape(EVENTS.get(i))).append('"');
        }
        return out.append("]}").toString();
    }

    private static void addEvent(String event) {
        EVENTS.add(event);
        while (EVENTS.size() > 40) EVENTS.remove(0);
    }

    private static boolean trustedOrigin(HttpExchange exchange) {
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        return origin == null || origin.equals("http://" + HOST + ":" + PORT) || origin.equals("http://localhost:" + PORT);
    }

    private static String voiceControlJson() {
        return "{\"ok\":true,\"active\":" + (LISTENING_ENABLED.get() && !SPEAKING.get()) +
                ",\"listeningEnabled\":" + LISTENING_ENABLED.get() + ",\"speaking\":" + SPEAKING.get() +
                ",\"voiceStatus\":\"" + jsonEscape(voiceStatus) + "\"}";
    }

    private static CommandResult launch(List<String> command, String successMessage) throws IOException {
        new ProcessBuilder(command).start();
        return new CommandResult(true, successMessage);
    }

    private static CommandResult openDirectory(Path path, String label) throws IOException {
        if (!Files.isDirectory(path)) {
            return new CommandResult(false, "Não encontrei a pasta " + label + " em " + path + ".");
        }
        new ProcessBuilder("explorer.exe", path.toAbsolutePath().toString()).start();
        return new CommandResult(true, "Abrindo " + label + ".");
    }

    private static CommandResult openWebSearch(String query) throws Exception {
        if (query == null || query.isBlank()) {
            return new CommandResult(false, "Diga o assunto da pesquisa. Exemplo: pesquisar na internet clima.");
        }
        String url = "https://www.google.com/search?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI.create(url));
            return new CommandResult(true, "Abri a pesquisa na internet para: " + query);
        }
        return new CommandResult(false, "Não consegui abrir o navegador automaticamente. Pesquise por: " + query);
    }

    private static CommandResult searchFiles(String query) {
        if (query == null || query.isBlank()) {
            return new CommandResult(false, "Informe o nome do arquivo. Exemplo: pesquisar arquivos orçamento.");
        }
        String needle = normalize(query);
        List<Path> roots = List.of(
                Path.of(System.getProperty("user.home"), "Desktop"),
                documentsPath(),
                Path.of(System.getProperty("user.home"), "Downloads"),
                Path.of(System.getProperty("user.home"), "Pictures"));
        List<String> matches = new ArrayList<>();
        for (Path root : roots) {
            if (!Files.isDirectory(root) || matches.size() >= 10) continue;
            try (Stream<Path> files = Files.walk(root, 3)) {
                files.limit(3000)
                        .filter(Files::isRegularFile)
                        .filter(p -> normalize(p.getFileName().toString()).contains(needle))
                        .limit(10 - matches.size())
                        .forEach(p -> matches.add(p.toAbsolutePath().toString()));
            } catch (IOException ignored) {
                // Continue with other known folders.
            }
        }
        if (matches.isEmpty()) return new CommandResult(true, "Não encontrei arquivos com esse nome nas pastas Desktop, Documentos, Downloads e Imagens.");
        return new CommandResult(true, "Encontrei estes arquivos (máximo 10):\n" + String.join("\n", matches));
    }

    private static CommandResult createFolder(String originalName) throws IOException {
        if (originalName == null || originalName.isBlank()) {
            return new CommandResult(false, "Diga o nome da pasta. Exemplo: criar pasta Projetos.");
        }
        String name = originalName.trim().replaceAll("^[\"']+|[\"']+$", "");
        name = name.replaceAll("[^\\p{L}\\p{N} _.-]", "").trim();
        if (name.isBlank() || name.equals(".") || name.equals("..")) {
            return new CommandResult(false, "Esse nome de pasta não é válido.");
        }
        Path base = documentsPath().resolve("Noar").toAbsolutePath().normalize();
        Path target = base.resolve(name).normalize();
        if (!target.startsWith(base)) return new CommandResult(false, "Por segurança, só posso criar pastas dentro de Documentos\\Noar.");
        Files.createDirectories(target);
        return new CommandResult(true, "Pasta criada ou já existente: " + target);
    }

    private static Path documentsPath() {
        return Path.of(System.getProperty("user.home"), "Documents");
    }

    private static String statusText() {
        Runtime runtime = Runtime.getRuntime();
        long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long maxMb = runtime.maxMemory() / (1024 * 1024);
        return "Informações do PC\nSistema: " + System.getProperty("os.name") + " " + System.getProperty("os.version")
                + "\nArquitetura: " + System.getProperty("os.arch")
                + "\nProcessadores lógicos: " + runtime.availableProcessors()
                + "\nMemória usada pela aplicação: " + usedMb + " MB"
                + "\nLimite de memória da aplicação: " + maxMb + " MB"
                + "\nData e hora: " + LocalDateTime.now().format(DATE_FORMAT);
    }

    private static String statusJson() {
        return "{\"ok\":true,\"platform\":\"" + jsonEscape(System.getProperty("os.name")) +
                "\",\"message\":\"" + jsonEscape(statusText()) +
                "\",\"voiceStatus\":\"" + jsonEscape(voiceStatus) +
                "\",\"listening\":" + (LISTENING_ENABLED.get() && !SPEAKING.get()) +
                ",\"autostart\":" + isAutoStartEnabled() + "}";
    }

    private static String extractWebQuery(String original) {
        return extractTail(original, List.of("pesquisar na internet", "buscar na internet",
                "pesquisar no google", "pesquisar web", "pesquisar:", "pesquise"));
    }

    private static String extractTail(String original, List<String> prefixes) {
        String normalized = normalize(original);
        for (String prefix : prefixes) {
            if (normalized.startsWith(prefix)) {
                int index = original.toLowerCase(Locale.ROOT).indexOf(prefix);
                if (index < 0) index = 0;
                return original.substring(Math.min(original.length(), index + prefix.length())).trim();
            }
        }
        return "";
    }

    private static boolean startsAny(String text, String... prefixes) {
        for (String prefix : prefixes) if (text.startsWith(prefix)) return true;
        return false;
    }

    private static String normalize(String text) {
        return Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    private static Map<String, String> parseForm(String input) {
        java.util.HashMap<String, String> result = new java.util.HashMap<>();
        for (String part : input.split("&")) {
            if (part.isEmpty()) continue;
            String[] pair = part.split("=", 2);
            try {
                String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
                String value = pair.length > 1 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "";
                result.put(key, value);
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed form field.
            }
        }
        return result;
    }

    private static String jsonEscape(String value) {
        StringBuilder out = new StringBuilder();
        for (char c : (value == null ? "" : value).toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.toString();
    }

    private static void sendJson(HttpExchange exchange, int status, String content) throws IOException {
        send(exchange, status, "application/json; charset=utf-8", content);
    }

    private static void send(HttpExchange exchange, int status, String contentType, String content) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        if ("text/html; charset=utf-8".equals(contentType)) {
            exchange.getResponseHeaders().set("Content-Security-Policy", "default-src 'self' 'unsafe-inline'; connect-src 'self'; object-src 'none'; base-uri 'none'");
            exchange.getResponseHeaders().set("Referrer-Policy", "no-referrer");
        }
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static void runSelfTests() {
        int[] passed = {0};
        check("normaliza acentos", normalize("INFORMAÇÕES") .equals("informacoes"), passed);
        check("reconhece calculadora", classify("abrir calculadora") == Intent.CALCULATOR, passed);
        check("reconhece bloco de notas", classify("abrir bloco de notas") == Intent.NOTEPAD, passed);
        check("reconhece status", classify("informações do PC") == Intent.STATUS, passed);
        check("bloqueia apagar arquivos", classify("apagar arquivos") == Intent.BLOCKED, passed);
        check("bloqueia terminal", classify("executar comando dir") == Intent.BLOCKED, passed);
        check("reconhece pesquisa de arquivos", classify("pesquisar arquivos fotos") == Intent.SEARCH_FILES, passed);
        check("reconhece pesquisa web", classify("pesquisar na internet notícias") == Intent.SEARCH_WEB, passed);
        check("reconhece criar pasta", classify("criar pasta Projetos") == Intent.CREATE_FOLDER, passed);
        check("não trata comando arbitrário como ação", classify("format C:") == Intent.UNKNOWN, passed);
        check("reconhece ditado universal", classify("digite olá mundo") == Intent.TYPE_TEXT, passed);
        check("reconhece atalho de teclado", classify("pressione enter") == Intent.PRESS_KEY, passed);
        check("extrai nome de aplicativo para abrir", "google chrome".equals(normalize(extractAppLaunchTarget("abra o Google Chrome"))), passed);
        check("reconhece palavra de ativação", startsWithWakeWord("Noar, me explique isso"), passed);
        System.out.println("Testes locais: " + passed[0] + "/14 aprovados.");
        if (passed[0] != 10) System.exit(1);
    }

    private static void check(String name, boolean condition, int[] passed) {
        if (!condition) {
            System.err.println("FALHA: " + name);
            System.exit(1);
        }
        passed[0]++;
        System.out.println("OK: " + name);
    }
}
