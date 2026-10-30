param([string]$BaseUrl = 'http://127.0.0.1:9090', [string]$FrontendUrl = 'http://127.0.0.1:8080')
$ErrorActionPreference = 'Stop'
foreach ($url in @($BaseUrl, $FrontendUrl)) {
    $uri = [uri]$url
    if ($uri.Scheme -ne 'http' -or $uri.Host -notin @('127.0.0.1', 'localhost')) { throw 'This test is restricted to local HTTP services.' }
}
function Assert-Success($result, [string]$operation) {
    if ($result.code -ne '0') { throw "$operation failed: $($result.msg)" }
}
function Invoke-Write([string]$Root, [string]$Path, $Session, [string]$Body = '{}') {
    $csrf = Invoke-RestMethod "$Root/auth/csrf" -WebSession $Session -TimeoutSec 15
    $headers = @{}; $headers[$csrf.data.headerName] = $csrf.data.token
    Invoke-RestMethod "$Root$Path" -Method Post -WebSession $Session -Headers $headers -ContentType 'application/json; charset=utf-8' -Body $Body -TimeoutSec 15
}
function Assert-Status([int]$Expected, [scriptblock]$Action) {
    try { & $Action | Out-Null } catch {
        if ($_.Exception.Response -and [int]$_.Exception.Response.StatusCode -eq $Expected) { return }
        throw
    }
    throw "Expected HTTP $Expected but request succeeded"
}
Assert-Status 401 { Invoke-RestMethod "$BaseUrl/stu/find" -TimeoutSec 15 }
Assert-Status 403 { Invoke-RestMethod "$BaseUrl/stu/login" -Method Post -ContentType 'application/json' -Body '{}' -TimeoutSec 15 }
$accounts = @(@{ Role = 'admin'; Username = 'admin01' }, @{ Role = 'dormManager'; Username = 'dorm01' }, @{ Role = 'stu'; Username = '20230001' })
foreach ($account in $accounts) {
    $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $body = @{ username = $account.Username; password = 'Demo123456' } | ConvertTo-Json
    $login = Invoke-Write $BaseUrl "/$($account.Role)/login" $session $body
    Assert-Success $login "$($account.Role) login"
    if ($login.data.username -ne $account.Username -or $login.data.PSObject.Properties.Name -contains 'password') { throw 'Unexpected login profile or exposed password' }
    $identity = Invoke-RestMethod "$BaseUrl/main/loadIdentity" -WebSession $session -TimeoutSec 15
    Assert-Success $identity 'Load identity'
    if ($identity.data -ne $account.Role) { throw 'Session role mismatch' }
    Assert-Success (Invoke-RestMethod "$BaseUrl/auth/me" -WebSession $session -TimeoutSec 15) 'Restore profile'
    foreach ($path in @('/stu/stuNum', '/room/selectHaveRoomStuNum', '/room/noFullRoom', '/repair/orderNum', '/notice/homePageNotice')) {
        Assert-Success (Invoke-RestMethod "$BaseUrl$path" -WebSession $session -TimeoutSec 15) $path
    }
    if ($account.Role -eq 'stu') {
        $room = Invoke-RestMethod "$BaseUrl/room/getMyRoom/$($account.Username)" -WebSession $session -TimeoutSec 15
        Assert-Success $room 'My room'
        if ($room.data.secondBed -eq '20230002') { throw 'Roommate identity exposed' }
        Assert-Status 403 { Invoke-RestMethod "$BaseUrl/room/getMyRoom/20230002" -WebSession $session -TimeoutSec 15 }
        Assert-Status 403 { Invoke-RestMethod "$BaseUrl/dormManager/find" -WebSession $session -TimeoutSec 15 }
    } else {
        $paths = @('/stu/find', '/building/find', '/room/find', '/notice/find', '/repair/find', '/adjustRoom/find', '/visitor/find')
        if ($account.Role -eq 'admin') { $paths += '/dormManager/find' }
        foreach ($path in $paths) {
            $page = Invoke-RestMethod "$BaseUrl$path" -WebSession $session -TimeoutSec 15
            Assert-Success $page $path
            if ($null -eq $page.data.records) { throw "Missing pagination records: $path" }
        }
        if ($account.Role -eq 'dormManager') {
            $rooms = Invoke-RestMethod "$BaseUrl/room/find?pageSize=100" -WebSession $session -TimeoutSec 15
            foreach ($room in $rooms.data.records) { if ($room.dormBuildId -ne 1) { throw 'Manager read another building' } }
            Assert-Status 403 { Invoke-RestMethod "$BaseUrl/dormManager/find" -WebSession $session -TimeoutSec 15 }
        }
    }
    Assert-Success (Invoke-Write $BaseUrl '/auth/logout' $session) 'Sign out'
    Assert-Status 401 { Invoke-RestMethod "$BaseUrl/auth/me" -WebSession $session -TimeoutSec 15 }
    Write-Output "PASS: $($account.Role) login, session, scoped queries and CSRF logout"
}
$invalidSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
Assert-Status 401 { Invoke-Write $BaseUrl '/stu/login' $invalidSession '{"username":"20230001","password":"incorrect"}' }
$html = Invoke-WebRequest "$FrontendUrl/Login" -Headers @{ Accept = 'text/html' } -UseBasicParsing -TimeoutSec 30
if ($html.StatusCode -ne 200 -or $html.Content -notmatch '<div id="app">') { throw 'Frontend entry is unavailable' }
$proxySession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
Assert-Success (Invoke-Write "$FrontendUrl/api" '/stu/login' $proxySession '{"username":"20230001","password":"Demo123456"}') 'Frontend API proxy login'
Assert-Success (Invoke-RestMethod "$FrontendUrl/api/auth/me" -WebSession $proxySession -TimeoutSec 15) 'Frontend API proxy session'
Assert-Success (Invoke-Write "$FrontendUrl/api" '/auth/logout' $proxySession) 'Proxy logout'
Write-Output 'PASS: anonymous denial, CSRF, invalid password, frontend entry and API proxy'
Write-Output 'Local HTTP smoke checks passed. No business records were modified.'
