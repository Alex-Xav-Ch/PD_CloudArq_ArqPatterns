# Pings every instance from every other instance through AWS Systems Manager (no SSH needed).
# Run from 06-hub-and-spoke/:  .\scripts\test_connectivity.ps1
param(
    [string]$TerraformDir = (Join-Path $PSScriptRoot "..\terraform")
)

$out = terraform "-chdir=$TerraformDir" output -json | ConvertFrom-Json
$ids = $out.instance_ids.value
$ips = $out.private_ips.value
$names = $ids.PSObject.Properties.Name

Write-Host "== Waiting for the instances to register in Systems Manager"
for ($i = 0; $i -lt 40; $i++) {
    $online = aws ssm describe-instance-information --query "InstanceInformationList[?PingStatus=='Online'].InstanceId" --output text
    $missing = $names | Where-Object { $online -notmatch $ids.$_ }
    if (-not $missing) { break }
    Start-Sleep -Seconds 10
}
if ($missing) { Write-Host "Not registered yet: $($missing -join ', ')"; exit 1 }

Write-Host "== Private IPs"
foreach ($n in $names) { Write-Host ("{0,-10} {1}" -f $n, $ips.$n) }

Write-Host "== Connectivity (3 pings from each source to each destination)"
$results = @()
foreach ($src in $names) {
    foreach ($dst in $names) {
        if ($src -eq $dst) { continue }

        $params = Join-Path $env:TEMP "ssm_ping.json"
        @{ commands = @("ping -c 3 -W 2 $($ips.$dst)") } | ConvertTo-Json | Set-Content -Encoding ascii $params
        $cmdId = aws ssm send-command --instance-ids $ids.$src --document-name AWS-RunShellScript --parameters "file://$params" --query "Command.CommandId" --output text

        do {
            Start-Sleep -Seconds 2
            $status = aws ssm get-command-invocation --command-id $cmdId --instance-id $ids.$src --query Status --output text 2>$null
        } while ($status -in @("Pending", "InProgress", "Delayed") -or -not $status)

        # The CLI returns one array item per line: join them before matching.
        $stdout = (aws ssm get-command-invocation --command-id $cmdId --instance-id $ids.$src --query StandardOutputContent --output text) -join "`n"
        $match = [regex]::Match($stdout, "(\d+) received")
        $received = if ($match.Success) { [int]$match.Groups[1].Value } else { 0 }
        $verdict = if (-not $match.Success) { "NO PING OUTPUT" } elseif ($received -gt 0) { "REACHABLE" } else { "BLOCKED" }
        if (-not $match.Success) {
            $stderr = (aws ssm get-command-invocation --command-id $cmdId --instance-id $ids.$src --query StandardErrorContent --output text) -join " "
            Write-Host "   ping did not run (status $status): $stderr"
        }
        Write-Host ("{0,-10} -> {1,-10} ({2,-12}) {3}/3 replies  {4}" -f $src, $dst, $ips.$dst, $received, $verdict)
        $results += [pscustomobject]@{ From = $src; To = $dst; Replies = $received; Result = $verdict }
    }
}

Write-Host "== Summary"
$results | Format-Table -AutoSize | Out-String | Write-Host
