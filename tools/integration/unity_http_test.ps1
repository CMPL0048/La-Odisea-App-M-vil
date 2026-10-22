# Prueba de integración HTTP del contrato con Unity (sprint 4).
#
# Hace por HTTP, contra Supabase, exactamente las llamadas que hará Unity:
#   login con contraseña -> get_player_snapshot -> claim_pending_rewards -> save_game_progress
#   -> adjust_coins (montos rechazados) -> llamadas sin sesión.
#
# No deja datos alterados en la cuenta:
#   - save_game_progress se llama con +10 XP / +60 s y luego otra vez con los stats originales
#     (solo cambia stats.updated_at).
#   - claim_pending_rewards solo se llama si no hay recompensas pendientes (entonces no hace nada);
#     con pendientes se omite, salvo con -Claim (las entrega de verdad y no se pueden regresar por HTTP).
#   - adjust_coins solo se prueba con montos que el servidor rechaza (0, 1001, gastar de más),
#     que no escriben nada. Con -WithCoins además hace +1 y -1: el saldo queda igual pero quedan
#     dos movimientos en coin_transactions. La ruta completa con rollback está en unity_contract_test.sql.
#
# Uso (PowerShell, desde la raíz del repo):
#   $env:ODISEA_DEMO_PASSWORD = '...'   # o lo pide al ejecutar
#   powershell -ExecutionPolicy Bypass -File tools\integration\unity_http_test.ps1
# La URL y la llave pública se leen de local.properties (SUPABASE_URL, SUPABASE_KEY).

param(
    [string]$Email = 'tic-310114@utnay.edu.mx',
    [switch]$Claim,
    [switch]$WithCoins
)

$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$propsPath = Join-Path $PSScriptRoot '..\..\local.properties'
$props = @{}
foreach ($line in Get-Content $propsPath) {
    if ($line -match '^\s*([^#=]+?)\s*=\s*(.*)$') { $props[$Matches[1]] = $Matches[2] -replace '\\:', ':' }
}
$url = $props['SUPABASE_URL'].TrimEnd('/')
$key = $props['SUPABASE_KEY']
if (-not $url -or -not $key) { throw 'Faltan SUPABASE_URL o SUPABASE_KEY en local.properties' }

$password = $env:ODISEA_DEMO_PASSWORD
if (-not $password) {
    $secure = Read-Host "Contraseña de $Email" -AsSecureString
    $password = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure))
}

$script:failures = 0
function Pass($step, $detail = '') { Write-Host "[OK]    $step $detail" -ForegroundColor Green }
function Fail($step, $detail = '') { Write-Host "[FALLO] $step $detail" -ForegroundColor Red; $script:failures++ }
function Skip($step, $detail = '') { Write-Host "[OMITE] $step $detail" -ForegroundColor Yellow }

# Llama a una función RPC como lo hace Unity. Regresa @{ ok; body; error }.
function Invoke-Rpc($name, $body = @{}, $token = $null) {
    $headers = @{ apikey = $key; 'Content-Type' = 'application/json' }
    if ($token) { $headers.Authorization = "Bearer $token" }
    $json = ConvertTo-Json $body -Depth 10 -Compress
    try {
        $res = Invoke-RestMethod -Method Post -Uri "$url/rest/v1/rpc/$name" -Headers $headers `
            -Body ([Text.Encoding]::UTF8.GetBytes($json))
        return @{ ok = $true; body = $res }
    } catch {
        $msg = $_.ErrorDetails.Message
        if (-not $msg) { $msg = $_.Exception.Message }
        return @{ ok = $false; error = $msg }
    }
}

# 1) Login con contraseña (lo mismo que hace Unity al iniciar).
try {
    $login = Invoke-RestMethod -Method Post -Uri "$url/auth/v1/token?grant_type=password" `
        -Headers @{ apikey = $key; 'Content-Type' = 'application/json' } `
        -Body (ConvertTo-Json @{ email = $Email; password = $password } -Compress)
    $token = $login.access_token
    Pass 'login con contraseña' "(usuario $($login.user.id))"
} catch {
    Fail 'login con contraseña' $_.ErrorDetails.Message
    exit 1
}
$userId = $login.user.id

# 2) Snapshot inicial.
$r = Invoke-Rpc 'get_player_snapshot' @{} $token
if (-not $r.ok -or $r.body.player.id -ne $userId) { Fail 'get_player_snapshot' $r.error; exit 1 }
$before = $r.body
$stats = $before.stats
$coins = [long]$before.coins
Pass 'get_player_snapshot' "(monedas $coins, nivel $($stats.level), pendientes $($before.pending_rewards))"

# 3) Recompensas pendientes.
$pending = [int]$before.pending_rewards
if ($pending -gt 0 -and -not $Claim) {
    Skip 'claim_pending_rewards' "(hay $pending pendiente(s); usa -Claim para entregarlas de verdad)"
} else {
    $r = Invoke-Rpc 'claim_pending_rewards' @{} $token
    $claimed = if ($r.ok) { [long]$r.body.coins } else { 0 }
    $after = (Invoke-Rpc 'get_player_snapshot' @{} $token).body
    if (-not $r.ok) { Fail 'claim_pending_rewards' $r.error }
    elseif ([long]$after.coins -ne $coins + $claimed -or [int]$after.pending_rewards -ne 0) {
        Fail 'claim_pending_rewards' "(saldo $($after.coins), esperado $($coins + $claimed); pendientes $($after.pending_rewards))"
    } else {
        Pass 'claim_pending_rewards' "(monedas +$claimed, objetos $(@($r.body.items).Count))"
        $coins += $claimed
    }
}

# 4) Guardado de partida y restauración. Unity manda siempre los máximos (si faltan vuelven a 100).
function StatsPayload($s, $xpDelta, $playDelta, $health) {
    @{ stats = [ordered]@{
        level = [int]$s.level; xp = [int]$s.xp + $xpDelta
        health = $health; health_max = [int]$s.health_max
        resistance = [int]$s.resistance; resistance_max = [int]$s.resistance_max
        enemies_defeated = [int]$s.enemies_defeated
        playtime_seconds = [long]$s.playtime_seconds + $playDelta
        current_zone_code = $s.current_zone_code
    } }
}
$r = Invoke-Rpc 'save_game_progress' (StatsPayload $stats 10 60 99999) $token
$saved = (Invoke-Rpc 'get_player_snapshot' @{} $token).body.stats
if (-not $r.ok) { Fail 'save_game_progress' $r.error }
elseif ([int]$saved.xp -ne [int]$stats.xp + 10 -or [long]$saved.playtime_seconds -ne [long]$stats.playtime_seconds + 60) {
    Fail 'save_game_progress' "(xp $($saved.xp), jugado $($saved.playtime_seconds))"
} elseif ([int]$saved.health -ne [int]$saved.health_max) {
    Fail 'save_game_progress' '(la vida no se recortó al máximo)'
} else { Pass 'save_game_progress' "(saved_at $($r.body.saved_at); vida recortada a $($saved.health))" }

$r = Invoke-Rpc 'save_game_progress' (StatsPayload $stats 0 0 ([int]$stats.health)) $token
$restored = (Invoke-Rpc 'get_player_snapshot' @{} $token).body.stats
$same = $r.ok
foreach ($f in 'level', 'xp', 'health', 'health_max', 'resistance', 'resistance_max', 'enemies_defeated', 'playtime_seconds', 'current_zone_code') {
    if ("$($restored.$f)" -ne "$($stats.$f)") { $same = $false }
}
if ($same) { Pass 'restaurar stats originales' } else { Fail 'restaurar stats originales' "($($r.error) $(ConvertTo-Json $restored -Compress))" }

# 5) Monedas: montos que el servidor debe rechazar (no escriben nada).
$overspend = -[Math]::Min($coins + 1, 1000)
foreach ($amount in 0, 1001, $overspend) {
    if ($amount -eq $overspend -and $coins + 1 -gt 1000) { Skip "adjust_coins($amount)" '(saldo >= 1000)'; continue }
    $r = Invoke-Rpc 'adjust_coins' @{ p_amount = $amount; p_reason = 'prueba integración: inválido' } $token
    if ($r.ok) { Fail "adjust_coins($amount) debió rechazarse" } else { Pass "adjust_coins($amount) rechazado" "($($r.error))" }
}
if ($WithCoins) {
    $up = Invoke-Rpc 'adjust_coins' @{ p_amount = 1; p_reason = 'prueba integración: ganar' } $token
    $down = Invoke-Rpc 'adjust_coins' @{ p_amount = -1; p_reason = 'prueba integración: gastar' } $token
    if ($up.ok -and $down.ok -and [long]$up.body.coins -eq $coins + 1 -and [long]$down.body.coins -eq $coins) {
        Pass 'adjust_coins +1/-1' "(saldo de vuelta en $coins)"
    } else { Fail 'adjust_coins +1/-1' "($($up.error) $($down.error))" }
}

# 6) Sin sesión (solo la llave pública): todo el contrato debe negarse.
foreach ($call in @(
        @('get_player_snapshot', @{}),
        @('claim_pending_rewards', @{}),
        @('save_game_progress', @{ p_payload = @{} }),
        @('adjust_coins', @{ p_amount = 1; p_reason = 'prueba integración: sin sesión' }))) {
    $r = Invoke-Rpc $call[0] $call[1]
    if ($r.ok -and $call[0] -eq 'get_player_snapshot' -and -not $r.body.player) {
        Pass "$($call[0]) sin sesión" '(regresa vacío)'
    } elseif ($r.ok) { Fail "$($call[0]) sin sesión fue aceptado" } else { Pass "$($call[0]) sin sesión rechazado" }
}

# Comprobación final: saldo y stats iguales que al inicio (salvo lo reclamado con -Claim).
$final = (Invoke-Rpc 'get_player_snapshot' @{} $token).body
if ([long]$final.coins -eq $coins) { Pass 'saldo final' "($coins)" } else { Fail 'saldo final' "($($final.coins), esperado $coins)" }

if ($script:failures -gt 0) { Write-Host "`n$($script:failures) comprobación(es) fallaron." -ForegroundColor Red; exit 1 }
Write-Host "`nTodo bien: el contrato con Unity responde como se espera." -ForegroundColor Green
