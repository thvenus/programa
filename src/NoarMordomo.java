import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public final class NoarMordomo {
    private static final int PORT = 8770;
    private static final String HOST = "127.0.0.1";
    private static final String APP_NAME = "Noar Mordomo";
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

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
<p>Digite um comando ou use sua voz. Exemplo: <b>“abrir calculadora”</b>.</p>
<label for="command">O que você quer que eu faça?</label>
<textarea id="command" rows="3" placeholder="Digite aqui ou clique em Falar..."></textarea>
<div class="buttons"><button class="primary" id="send">Executar comando</button><button id="talk">🎙 Falar</button><button id="system">Informações do PC</button></div>
<div class="quick">
<button data-cmd="abrir calculadora">Calculadora</button><button data-cmd="abrir bloco de notas">Bloco de Notas</button><button data-cmd="abrir explorador de arquivos">Explorador</button><button data-cmd="abrir downloads">Downloads</button><button data-cmd="abrir documentos">Documentos</button>
</div>
<div class="answer" id="answer" role="status" aria-live="polite">Olá! Sou o Noar. Como posso ajudar?</div>
</section>
<section class="card">
<b>Comandos disponíveis</b>
<p>Abra aplicativos e pastas, consulte informações básicas do computador, pesquise arquivos pelo nome, pesquise na internet ou crie uma pasta dentro de Documentos\\Noar.</p>
<p class="warn">Por segurança, não executo comandos de terminal, não apago arquivos e não altero o Registro do Windows.</p>
</section>
<div class="foot">O painel funciona neste computador em 127.0.0.1. A função de voz depende do navegador e da permissão do microfone.</div>
</main>
<script>
const $=id=>document.getElementById(id);
async function execute(command){
  if(!command.trim()){ $('answer').textContent='Digite um comando primeiro.'; return; }
  $('send').disabled=true;
  $('answer').textContent='Processando...';
  try{
    const response=await fetch('/api/command',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8'},body:new URLSearchParams({command})});
    const data=await response.json();
    $('answer').textContent=data.message||'Comando recebido.';
    if(data.ok && 'speechSynthesis' in window && data.speak!==false){
      speechSynthesis.cancel();
      const utterance=new SpeechSynthesisUtterance(data.message||'Pronto');
      utterance.lang='pt-BR'; speechSynthesis.speak(utterance);
    }
  }catch(e){$('answer').textContent='Não consegui falar com o Noar. Feche e abra o programa novamente.';}
  finally{$('send').disabled=false;}
}
$('send').addEventListener('click',()=>execute($('command').value));
$('command').addEventListener('keydown',e=>{if(e.key==='Enter'&&!e.shiftKey){e.preventDefault();execute($('command').value);}});
$('system').addEventListener('click',()=>execute('informações do sistema'));
document.querySelectorAll('[data-cmd]').forEach(b=>b.addEventListener('click',()=>{ $('command').value=b.dataset.cmd; execute(b.dataset.cmd); }));
$('talk').addEventListener('click',()=>{
 const SR=window.SpeechRecognition||window.webkitSpeechRecognition;
 if(!SR){$('answer').textContent='Seu navegador não oferece reconhecimento de voz aqui. Use o Chrome ou Edge atualizado, ou digite o comando.';return;}
 const recognition=new SR(); recognition.lang='pt-BR'; recognition.interimResults=false; recognition.maxAlternatives=1;
 $('talk').disabled=true; $('answer').textContent='Estou ouvindo...';
 recognition.onresult=e=>{const words=e.results[0][0].transcript;$('command').value=words;execute(words);};
 recognition.onerror=e=>{$('answer').textContent='Não consegui reconhecer a voz ('+e.error+'). Verifique a permissão do microfone.';};
 recognition.onend=()=>{$('talk').disabled=false;};
 try{recognition.start();}catch(e){$('talk').disabled=false;$('answer').textContent='Não foi possível iniciar o microfone. Tente novamente.';}
});
fetch('/api/status').then(r=>r.json()).then(s=>{$('statusLine').textContent=s.platform+' · serviço local ativo';}).catch(()=>{$('statusLine').textContent='Conectando ao serviço local...';});
</script></body></html>
""";

    private enum Intent {
        EMPTY, STATUS, CALCULATOR, NOTEPAD, EXPLORER, SETTINGS,
        DOWNLOADS, DOCUMENTS, DESKTOP, SEARCH_FILES, SEARCH_WEB,
        CREATE_FOLDER, BLOCKED, UNKNOWN
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("--self-test")) {
            runSelfTests();
            return;
        }
        boolean noBrowser = List.of(args).contains("--no-browser");
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName(HOST), PORT), 0);
        server.setExecutor(Executors.newFixedThreadPool(6));
        server.createContext("/", NoarMordomo::handle);
        server.start();
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
            if ("/api/command".equals(path) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String origin = exchange.getRequestHeaders().getFirst("Origin");
                if (origin != null && !origin.equals("http://" + HOST + ":" + PORT)
                        && !origin.equals("http://localhost:" + PORT)) {
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
                case BLOCKED -> new CommandResult(false,
                        "Por segurança, não executo comandos arbitrários nem ações destrutivas. Posso abrir aplicativos, pastas e pesquisas.");
                case EMPTY -> new CommandResult(false, "Digite um comando para eu ajudar.");
                case UNKNOWN -> new CommandResult(false,
                        "Ainda não reconheço esse comando. Experimente: abrir calculadora, abrir downloads, informações do sistema, pesquisar arquivos nome ou pesquisar na internet assunto.");
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
        return Intent.UNKNOWN;
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
                "\",\"message\":\"" + jsonEscape(statusText()) + "\"}";
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
        System.out.println("Testes locais: " + passed[0] + "/10 aprovados.");
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
