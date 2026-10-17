param([string]$ApiRoot = "http://127.0.0.1:8770/api")
$ErrorActionPreference = "Stop"

function Send-Status([string]$Message) {
    try {
        Invoke-RestMethod -Uri ($ApiRoot.TrimEnd("/") + "/voice-status") -Method Post -ContentType "application/x-www-form-urlencoded" -TimeoutSec 2 -Body @{ status = $Message } | Out-Null
    } catch {
        # The app may be starting or shutting down.
    }
}

$engine = $null
try {
    Add-Type -AssemblyName System.Speech
    $recognizers = [System.Speech.Recognition.SpeechRecognitionEngine]::InstalledRecognizers()
    $recognizerInfo = $recognizers | Where-Object { $_.Culture.Name -eq "pt-BR" } | Select-Object -First 1
    if ($null -eq $recognizerInfo) {
        $recognizerInfo = $recognizers | Where-Object { $_.Culture.Name.StartsWith("pt") } | Select-Object -First 1
    }
    if ($null -eq $recognizerInfo) {
        Send-Status "Falta reconhecimento de fala em português no Windows. Instale o recurso de voz em Português (Brasil)."
        exit 2
    }

    $engine = New-Object System.Speech.Recognition.SpeechRecognitionEngine($recognizerInfo)
    $engine.LoadGrammar((New-Object System.Speech.Recognition.DictationGrammar))
    $engine.SetInputToDefaultAudioDevice()
    $apiVoice = $ApiRoot.TrimEnd("/") + "/voice"

    $engine.add_SpeechRecognized({
        param($sender, $eventArgs)
        try {
            $text = [string]$eventArgs.Result.Text
            $confidence = [double]$eventArgs.Result.Confidence
            if ([string]::IsNullOrWhiteSpace($text) -or $confidence -lt 0.55) { return }
            Invoke-RestMethod -Uri $apiVoice -Method Post -ContentType "application/x-www-form-urlencoded" -TimeoutSec 4 -Body @{ command = $text; confidence = $confidence.ToString([Globalization.CultureInfo]::InvariantCulture) } | Out-Null
        } catch {
            # Ignore one failed transcription and continue listening.
        }
    })

    Send-Status ("Motor de voz do Windows iniciado (" + $recognizerInfo.Culture.Name + ").")
    $isListening = $false
    while ($true) {
        try {
            $control = Invoke-RestMethod -Uri ($ApiRoot.TrimEnd("/") + "/voice-control") -Method Get -TimeoutSec 2
            $shouldListen = [bool]$control.active
            if ($shouldListen -and -not $isListening) {
                try {
                    $engine.RecognizeAsync([System.Speech.Recognition.RecognizeMode]::Multiple)
                    $isListening = $true
                    Send-Status ("Ouvindo continuamente em " + $recognizerInfo.Culture.Name + ".")
                } catch {
                    Start-Sleep -Milliseconds 500
                }
            } elseif (-not $shouldListen -and $isListening) {
                try { $engine.RecognizeAsyncCancel() } catch { }
                $isListening = $false
                Send-Status "Microfone pausado temporariamente ou pelo usuário."
            }
        } catch {
            # Retry after temporary local HTTP errors.
        }
        Start-Sleep -Seconds 1
    }
} catch {
    Send-Status ("Escuta por voz indisponível: " + $_.Exception.Message)
    exit 1
} finally {
    if ($null -ne $engine) {
        try { $engine.RecognizeAsyncCancel() } catch { }
        try { $engine.Dispose() } catch { }
    }
}