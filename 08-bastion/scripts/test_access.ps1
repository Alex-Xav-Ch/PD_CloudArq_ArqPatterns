# Checks how the private server can and cannot be reached.
# Run from 08-bastion/:  .\scripts\test_access.ps1 -KeyFile $env:USERPROFILE\.ssh\labsuser.pem [-Ssm]
param(
    [Parameter(Mandatory)][string]$KeyFile,
    [switch]$Ssm,
    [string]$TerraformDir
)

if (-not $TerraformDir) { $TerraformDir = Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) "..\terraform" }

$out = terraform "-chdir=$TerraformDir" output -json | ConvertFrom-Json
$bastion = $out.bastion_public_ip.value
$private = $out.private_server_ip.value
$privateId = $out.private_server_id.value
$key = $KeyFile -replace '\\', '/'

# Temporary ssh config: it lets the private server be reached through the bastion with ProxyJump.
# Host keys are not checked or stored because the servers are recreated on every deployment.
$cfg = Join-Path $env:TEMP "bastion_ssh_config"
@"
Host *
  User ec2-user
  IdentityFile "$key"
  StrictHostKeyChecking no
  UserKnownHostsFile NUL
  LogLevel ERROR
  BatchMode yes
  ConnectTimeout 8

Host bastion
  HostName $bastion

Host private-direct
  HostName $private

Host private-via-bastion
  HostName $private
  ProxyJump bastion
"@ | Set-Content -Encoding ascii $cfg

function Invoke-Ssh([string]$HostAlias, [string]$Command) {
    $lines = & ssh -F $cfg $HostAlias $Command 2>&1 | ForEach-Object { "$_" }
    [pscustomobject]@{ Code = $LASTEXITCODE; Output = ($lines -join "`n").Trim() }
}

Write-Host "Bastion (public): $bastion | Private server: $private"
Write-Host ""

Write-Host "== TEST 1: SSH straight to the private server (no bastion)"
$r = Invoke-Ssh "private-direct" "hostname"
if ($r.Code -ne 0) { Write-Host "BLOCKED (expected): $($r.Output)" } else { Write-Host "REACHABLE (unexpected): $($r.Output)" }
Write-Host ""

Write-Host "== TEST 2: SSH to the bastion from the administrator IP"
$r = Invoke-Ssh "bastion" "hostname; hostname -I"
if ($r.Code -eq 0) { Write-Host "OK"; Write-Host $r.Output } else { Write-Host "FAILED: $($r.Output)" }
Write-Host ""

Write-Host "== TEST 3: SSH to the private server through the bastion"
$r = Invoke-Ssh "private-via-bastion" "hostname; hostname -I"
if ($r.Code -eq 0) { Write-Host "OK"; Write-Host $r.Output } else { Write-Host "FAILED: $($r.Output)" }
Write-Host ""

Write-Host "== TEST 4: audit trail kept by the bastion (successful SSH logins recorded by sshd)"
$r = Invoke-Ssh "bastion" "sudo journalctl -u sshd --no-pager | grep -c 'Accepted publickey'"
if ($r.Code -eq 0) { Write-Host "OK: $($r.Output) successful logins recorded on the bastion" } else { Write-Host "FAILED: $($r.Output)" }
Write-Host ""

if ($Ssm) {
    Write-Host "== TEST 5: manage the private server with Systems Manager (no SSH, no bastion)"
    $missing = $true
    for ($i = 0; $i -lt 30; $i++) {
        $online = aws ssm describe-instance-information --query "InstanceInformationList[?PingStatus=='Online'].InstanceId" --output text
        if ($online -match $privateId) { $missing = $false; break }
        Start-Sleep -Seconds 10
    }
    if ($missing) {
        Write-Host "FAILED: the private server did not register in Systems Manager"
    } else {
        $params = Join-Path $env:TEMP "ssm_bastion.json"
        @{ commands = @("hostname", "hostname -I") } | ConvertTo-Json | Set-Content -Encoding ascii $params
        $cmdId = aws ssm send-command --instance-ids $privateId --document-name AWS-RunShellScript --parameters "file://$params" --query "Command.CommandId" --output text
        do {
            Start-Sleep -Seconds 2
            $status = aws ssm get-command-invocation --command-id $cmdId --instance-id $privateId --query Status --output text 2>$null
        } while ($status -in @("Pending", "InProgress", "Delayed") -or -not $status)
        $o = (aws ssm get-command-invocation --command-id $cmdId --instance-id $privateId --query StandardOutputContent --output text) -join "`n"
        Write-Host "OK (status $status)"
        Write-Host $o.Trim()
    }
    Write-Host ""
}

Remove-Item $cfg -ErrorAction SilentlyContinue
Write-Host "== DONE"
