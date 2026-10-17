param(
    [string]$ApiRoot = "http://127.0.0.1:8770/api",
    [switch]$ListMicrophones,
    [switch]$TestMicrophone
)
$ErrorActionPreference = "Stop"

$audioInterop = @'
using System;
using System.Collections.Generic;
using System.Runtime.InteropServices;
namespace NoarAudio {
    public enum DataFlow { Render = 0, Capture = 1, All = 2 }
    public enum AudioRole { Console = 0, Multimedia = 1, Communications = 2 }
    public sealed class AudioInput { public string Id { get; set; } public string Name { get; set; } public bool IsDefault { get; set; } }
    public static class AudioDevices {
        private const int Active = 1;
        private static readonly PROPERTYKEY FriendlyNameKey = new PROPERTYKEY { fmtid = new Guid("a45c254e-df1c-4efd-8020-67d146a850e0"), pid = 14 };
        public static AudioInput[] GetCaptureEndpoints() {
            var result = new List<AudioInput>();
            IMMDeviceEnumerator en = null; IMMDeviceCollection col = null;
            try {
                en = (IMMDeviceEnumerator)new MMDeviceEnumerator();
                Marshal.ThrowExceptionForHR(en.EnumAudioEndpoints(DataFlow.Capture, Active, out col));
                int count; Marshal.ThrowExceptionForHR(col.GetCount(out count));
                string defaultId = GetDefaultCaptureId(en);
                for (int i=0;i<count;i++) {
                    IMMDevice dev = null;
                    try {
                        Marshal.ThrowExceptionForHR(col.Item(i,out dev));
                        string id; Marshal.ThrowExceptionForHR(dev.GetId(out id));
                        string name = GetFriendlyName(dev);
                        if (!String.IsNullOrWhiteSpace(id) && !String.IsNullOrWhiteSpace(name))
                            result.Add(new AudioInput { Id=id, Name=name, IsDefault=String.Equals(id,defaultId,StringComparison.OrdinalIgnoreCase) });
                    } finally { if (dev != null && Marshal.IsComObject(dev)) Marshal.ReleaseComObject(dev); }
                }
            } finally {
                if (col != null && Marshal.IsComObject(col)) Marshal.ReleaseComObject(col);
                if (en != null && Marshal.IsComObject(en)) Marshal.ReleaseComObject(en);
            }
            return result.ToArray();
        }
        public static void SetDefaultCaptureEndpoint(string endpointId) {
            if (String.IsNullOrWhiteSpace(endpointId)) throw new ArgumentException("Microfone sem identificador.");
            IPolicyConfig policy = null;
            try {
                policy = (IPolicyConfig)new PolicyConfigClient();
                foreach (AudioRole role in new AudioRole[] { AudioRole.Console, AudioRole.Multimedia, AudioRole.Communications })
                    Marshal.ThrowExceptionForHR(policy.SetDefaultEndpoint(endpointId,role));
            } finally { if (policy != null && Marshal.IsComObject(policy)) Marshal.ReleaseComObject(policy); }
        }
        private static string GetDefaultCaptureId(IMMDeviceEnumerator en) {
            IMMDevice dev = null;
            try {
                int hr = en.GetDefaultAudioEndpoint(DataFlow.Capture,AudioRole.Communications,out dev);
                if (hr != 0 || dev == null) return null;
                string id; return dev.GetId(out id) == 0 ? id : null;
            } catch { return null; }
            finally { if (dev != null && Marshal.IsComObject(dev)) Marshal.ReleaseComObject(dev); }
        }
        private static string GetFriendlyName(IMMDevice dev) {
            IPropertyStore store = null; PROPVARIANT value = new PROPVARIANT();
            try {
                Marshal.ThrowExceptionForHR(dev.OpenPropertyStore(0,out store));
                PROPERTYKEY key = FriendlyNameKey;
                Marshal.ThrowExceptionForHR(store.GetValue(ref key,out value));
                return value.vt == 31 && value.pwszVal != IntPtr.Zero ? Marshal.PtrToStringUni(value.pwszVal) : "(Dispositivo de áudio)";
            } finally {
                PropVariantClear(ref value);
                if (store != null && Marshal.IsComObject(store)) Marshal.ReleaseComObject(store);
            }
        }
        [DllImport("ole32.dll")] private static extern int PropVariantClear(ref PROPVARIANT value);
        [StructLayout(LayoutKind.Sequential)] private struct PROPERTYKEY { public Guid fmtid; public int pid; }
        [StructLayout(LayoutKind.Explicit)] private struct PROPVARIANT { [FieldOffset(0)] public short vt; [FieldOffset(8)] public IntPtr pwszVal; }
        [ComImport, Guid("BCDE0395-E52F-467C-8E3D-C4579291692E"), ClassInterface(ClassInterfaceType.None)] private class MMDeviceEnumerator { }
        [ComImport, Guid("A95664D2-9614-4F35-A746-DE8DB63617E6"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
        private interface IMMDeviceEnumerator {
            [PreserveSig] int EnumAudioEndpoints(DataFlow flow,int mask,out IMMDeviceCollection devices);
            [PreserveSig] int GetDefaultAudioEndpoint(DataFlow flow,AudioRole role,out IMMDevice device);
            [PreserveSig] int GetDevice([MarshalAs(UnmanagedType.LPWStr)] string id,out IMMDevice device);
            [PreserveSig] int RegisterEndpointNotificationCallback(IntPtr client);
            [PreserveSig] int UnregisterEndpointNotificationCallback(IntPtr client);
        }
        [ComImport, Guid("0BD7A1BE-7A1A-44DB-8397-CC5392387B5E"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
        private interface IMMDeviceCollection {
            [PreserveSig] int GetCount(out int count);
            [PreserveSig] int Item(int index,out IMMDevice device);
        }
        [ComImport, Guid("D666063F-1587-4E43-81F1-B948E807363F"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
        private interface IMMDevice {
            [PreserveSig] int Activate(ref Guid iid,int clsCtx,IntPtr parameters,[MarshalAs(UnmanagedType.IUnknown)] out object iface);
            [PreserveSig] int OpenPropertyStore(int access,out IPropertyStore store);
            [PreserveSig] int GetId([MarshalAs(UnmanagedType.LPWStr)] out string id);
            [PreserveSig] int GetState(out int state);
        }
        [ComImport, Guid("886d8eeb-8cf2-4446-8d02-cdba1dbdcf99"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
        private interface IPropertyStore {
            [PreserveSig] int GetCount(out int count);
            [PreserveSig] int GetAt(int index,out PROPERTYKEY key);
            [PreserveSig] int GetValue(ref PROPERTYKEY key,out PROPVARIANT value);
            [PreserveSig] int SetValue(ref PROPERTYKEY key,ref PROPVARIANT value);
            [PreserveSig] int Commit();
        }
        [ComImport, Guid("870af99c-171d-4f9e-af0d-e63df40c2bc9"), ClassInterface(ClassInterfaceType.None)] private class PolicyConfigClient { }
        [ComImport, Guid("f8679f50-850a-41cf-9c72-430f290290c8"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
        private interface IPolicyConfig {
            [PreserveSig] int GetMixFormat(string id,IntPtr format);
            [PreserveSig] int GetDeviceFormat(string id,bool def,IntPtr format);
            [PreserveSig] int ResetDeviceFormat(string id);
            [PreserveSig] int SetDeviceFormat(string id,IntPtr endpointFormat,IntPtr mixFormat);
            [PreserveSig] int GetProcessingPeriod(string id,bool def,IntPtr defaultPeriod,IntPtr minimumPeriod);
            [PreserveSig] int SetProcessingPeriod(string id,IntPtr period);
            [PreserveSig] int GetShareMode(string id,IntPtr mode);
            [PreserveSig] int SetShareMode(string id,IntPtr mode);
            [PreserveSig] int GetPropertyValue(string id,bool store,ref PROPERTYKEY key,out PROPVARIANT value);
            [PreserveSig] int SetPropertyValue(string id,bool store,ref PROPERTYKEY key,ref PROPVARIANT value);
            [PreserveSig] int SetDefaultEndpoint([MarshalAs(UnmanagedType.LPWStr)] string id,AudioRole role);
            [PreserveSig] int SetEndpointVisibility(string id,bool visible);
        }
    }
}
'@

try { Add-Type -TypeDefinition $audioInterop -Language CSharp -ErrorAction Stop } catch {
    if (-not ("NoarAudio.AudioDevices" -as [type])) { Write-Output ("ERROR:" + $_.Exception.Message); exit 3 }
}

if ($ListMicrophones) {
    try {
        $devices = [NoarAudio.AudioDevices]::GetCaptureEndpoints()
        foreach ($device in $devices) {
            $id64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes([string]$device.Id))
            $name64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes([string]$device.Name))
            Write-Output ("MIC" + [char]9 + $id64 + [char]9 + $name64 + [char]9 + [string]$device.IsDefault)
        }
        if (-not $devices -or $devices.Count -eq 0) { Write-Output "NO_MICROPHONES" }
    } catch { Write-Output ("ERROR:" + $_.Exception.Message); exit 3 }
    exit 0
}

if ($TestMicrophone) {
    $testEngine = $null
    try {
        Add-Type -AssemblyName System.Speech
        $available = [System.Speech.Recognition.SpeechRecognitionEngine]::InstalledRecognizers()
        $recognizerInfo = $available | Where-Object { $_.Culture.Name -eq "pt-BR" } | Select-Object -First 1
        if ($null -eq $recognizerInfo) { $recognizerInfo = $available | Where-Object { $_.Culture.Name.StartsWith("pt") } | Select-Object -First 1 }
        if ($null -eq $recognizerInfo) { Write-Output "ERROR:O reconhecimento de fala em português não está instalado no Windows."; exit 0 }
        $testEngine = New-Object System.Speech.Recognition.SpeechRecognitionEngine($recognizerInfo)
        $testEngine.LoadGrammar((New-Object System.Speech.Recognition.DictationGrammar))
        $testEngine.InitialSilenceTimeout = [TimeSpan]::FromSeconds(5)
        $testEngine.EndSilenceTimeout = [TimeSpan]::FromMilliseconds(700)
        $testEngine.SetInputToDefaultAudioDevice()
        Write-Output ("TEST_STARTED:" + $recognizerInfo.Culture.Name)
        $result = $testEngine.Recognize([TimeSpan]::FromSeconds(8))
        if ($null -eq $result -or [string]::IsNullOrWhiteSpace([string]$result.Text)) { Write-Output "NO_SPEECH" }
        else {
            $text64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes([string]$result.Text))
            Write-Output ("RESULT" + [char]9 + $text64 + [char]9 + ([double]$result.Confidence).ToString([Globalization.CultureInfo]::InvariantCulture))
        }
    } catch { Write-Output ("ERROR:" + $_.Exception.Message) }
    finally { if ($null -ne $testEngine) { try { $testEngine.Dispose() } catch { } } }
    exit 0
}

function Send-Status([string]$Message) {
    try { Invoke-RestMethod -Uri ($ApiRoot.TrimEnd("/") + "/voice-status") -Method Post -ContentType "application/x-www-form-urlencoded" -TimeoutSec 2 -Body @{ status = $Message } | Out-Null } catch { }
}

$engine = $null
$script:confidenceThreshold = 0.43
$script:lastUtterance = ""
$script:lastUtteranceAt = [DateTime]::MinValue
$lastAppliedMicrophoneId = ""
$lastAppliedMicrophoneName = ""
$apiVoice = $ApiRoot.TrimEnd("/") + "/voice"

try {
    Add-Type -AssemblyName System.Speech
    $recognizers = [System.Speech.Recognition.SpeechRecognitionEngine]::InstalledRecognizers()
    $recognizerInfo = $recognizers | Where-Object { $_.Culture.Name -eq "pt-BR" } | Select-Object -First 1
    if ($null -eq $recognizerInfo) { $recognizerInfo = $recognizers | Where-Object { $_.Culture.Name.StartsWith("pt") } | Select-Object -First 1 }
    if ($null -eq $recognizerInfo) {
        Send-Status "Falta reconhecimento de fala em português no Windows. Instale a voz de Português (Brasil)."
        exit 2
    }

    $engine = New-Object System.Speech.Recognition.SpeechRecognitionEngine($recognizerInfo)
    $engine.LoadGrammar((New-Object System.Speech.Recognition.DictationGrammar))
    $engine.InitialSilenceTimeout = [TimeSpan]::FromSeconds(8)
    $engine.EndSilenceTimeout = [TimeSpan]::FromMilliseconds(800)
    $engine.EndSilenceTimeoutAmbiguous = [TimeSpan]::FromMilliseconds(1200)
    try {
        $captureDevices = [NoarAudio.AudioDevices]::GetCaptureEndpoints()
        $defaultInput = $captureDevices | Where-Object { $_.IsDefault } | Select-Object -First 1
        if ($null -ne $defaultInput) { Send-Status ("Reconhecedor " + $recognizerInfo.Culture.Name + ". Microfone padrão detectado: " + $defaultInput.Name + ".") }
        elseif ($captureDevices.Count -gt 0) { Send-Status ("Reconhecedor " + $recognizerInfo.Culture.Name + ". " + $captureDevices.Count + " entrada(s) ativa(s), sem padrão identificado.") }
        else { Send-Status "Não encontrei microfone ativo. Conecte ou habilite um dispositivo de entrada no Windows." }
    } catch { Send-Status ("Falha ao consultar dispositivos de entrada: " + $_.Exception.Message) }
    $engine.SetInputToDefaultAudioDevice()

    $engine.add_SpeechRecognized({
        param($sender, $eventArgs)
        try {
            $text = [string]$eventArgs.Result.Text
            $confidence = [double]$eventArgs.Result.Confidence
            if ([string]::IsNullOrWhiteSpace($text) -or $confidence -lt $script:confidenceThreshold) { return }
            $now = Get-Date
            if ($text -eq $script:lastUtterance -and ($now - $script:lastUtteranceAt).TotalMilliseconds -lt 1800) { return }
            $script:lastUtterance = $text
            $script:lastUtteranceAt = $now
            Invoke-RestMethod -Uri $apiVoice -Method Post -ContentType "application/x-www-form-urlencoded" -TimeoutSec 4 -Body @{
                command = $text
                confidence = $confidence.ToString([Globalization.CultureInfo]::InvariantCulture)
            } | Out-Null
        } catch { }
    })

    Send-Status ("Reconhecedor pronto (" + $recognizerInfo.Culture.Name + ").")
    $isListening = $false
    while ($true) {
        try {
            $control = Invoke-RestMethod -Uri ($ApiRoot.TrimEnd("/") + "/voice-control") -Method Get -TimeoutSec 2
            $shouldListen = [bool]$control.active
            if ($null -ne $control.confidenceThreshold) { $script:confidenceThreshold = [Math]::Max(0.30, [Math]::Min(0.80, [double]$control.confidenceThreshold)) }
            $targetId = [string]$control.microphoneId
            if (-not [string]::IsNullOrWhiteSpace($targetId) -and $targetId -ne $lastAppliedMicrophoneId) {
                try {
                    if ($isListening) { try { $engine.RecognizeAsyncCancel() } catch { }; Start-Sleep -Milliseconds 700; $isListening = $false }
                    [NoarAudio.AudioDevices]::SetDefaultCaptureEndpoint($targetId)
                    $engine.SetInputToDefaultAudioDevice()
                    $lastAppliedMicrophoneId = $targetId
                    $lastAppliedMicrophoneName = [string]$control.microphoneName
                    Send-Status ("Microfone selecionado ativo: " + $lastAppliedMicrophoneName + ".")
                    $control = Invoke-RestMethod -Uri ($ApiRoot.TrimEnd("/") + "/voice-control") -Method Get -TimeoutSec 2
                    $shouldListen = [bool]$control.active
                } catch {
                    Send-Status ("Falha ao ativar microfone selecionado: " + $_.Exception.Message)
                    $lastAppliedMicrophoneId = $targetId
                }
            }
            if ($shouldListen -and -not $isListening) {
                try {
                    $engine.RecognizeAsync([System.Speech.Recognition.RecognizeMode]::Multiple)
                    $isListening = $true
                    Send-Status ("Ouvindo continuamente em " + $recognizerInfo.Culture.Name + ".")
                } catch { Send-Status ("Falha ao iniciar a escuta: " + $_.Exception.Message); Start-Sleep -Milliseconds 500 }
            } elseif (-not $shouldListen -and $isListening) {
                try { $engine.RecognizeAsyncCancel() } catch { }
                $isListening = $false
                Send-Status "Microfone pausado pelo usuário ou durante a resposta."
            }
        } catch { Send-Status ("Erro consultando o estado da voz: " + $_.Exception.Message) }
        Start-Sleep -Seconds 1
    }
} catch {
    Send-Status ("Escuta por voz indisponível: " + $_.Exception.Message)
    exit 1
} finally {
    if ($null -ne $engine) { try { $engine.RecognizeAsyncCancel() } catch { }; try { $engine.Dispose() } catch { } }
}
