# Cutover of the migration: freeze the origin, export its data, upload the files, create the target
# and wait until it answers. Prints the service interruption time.
# Run from 07-lift-and-shift/ after building the jar:  .\migration\cutover.ps1
param(
    [string]$Root = (Join-Path $PSScriptRoot "..")
)

$ErrorActionPreference = "Stop"
$compose = Join-Path $Root "docker-compose.yml"
$terraform = Join-Path $Root "terraform"
$jar = Join-Path $Root "app\target\academic-app.jar"
$dump = Join-Path $PSScriptRoot "academic.sql"
$log = Join-Path $Root "evidences\cutover.txt"

# Every step is shown on screen and saved with its time in evidences/cutover.txt.
function Log($text) { Write-Host $text; Add-Content -Path $log -Value $text -Encoding utf8 }
function Step($text) { Log ("[{0}] {1}" -f (Get-Date -Format "HH:mm:ss"), $text) }
function Check($what) { if ($LASTEXITCODE -ne 0) { throw "$what failed (exit code $LASTEXITCODE)" } }

if (-not (Test-Path $jar)) { throw "Build the jar first: mvn -f app/pom.xml package" }
$bucket = terraform "-chdir=$terraform" output -raw bucket_name
Check "terraform output (run terraform apply first, step 3 of the readme)"

Set-Content -Path $log -Value "Cutover started $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -Encoding utf8
$start = Get-Date
Step "1/5 Freezing the origin: the application is stopped, no more writes"
docker compose -f $compose stop app
Check "docker compose stop"

Step "2/5 Exporting the database from the origin"
docker compose -f $compose exec -T db pg_dump -U app --clean --if-exists --no-owner --no-privileges -f /tmp/academic.sql academic
Check "pg_dump"
docker compose -f $compose cp db:/tmp/academic.sql $dump
Check "docker compose cp"

Step "3/5 Uploading the dump and the jar to S3 ($bucket)"
aws s3 cp $dump "s3://$bucket/academic.sql"
Check "upload of the dump"
aws s3 cp $jar "s3://$bucket/academic-app.jar"
Check "upload of the jar"

Step "4/5 Creating the target server (it installs the stack and restores the data when it boots)"
terraform "-chdir=$terraform" apply -auto-approve -var="migrate=true"
Check "terraform apply"
$url = terraform "-chdir=$terraform" output -raw app_url
Check "terraform output app_url"

Step "5/5 Waiting for the migrated application to answer at $url"
$answered = $false
for ($i = 0; $i -lt 120; $i++) {
    try {
        $r = Invoke-RestMethod "$url/health" -TimeoutSec 5
        if ($r.status -eq "ok") { $answered = $true; break }
    } catch { }
    Start-Sleep -Seconds 10
}
if (-not $answered) { throw "The target did not answer in 20 minutes. Check /var/log/migration.log on the instance." }

$elapsed = (Get-Date) - $start
Step "Target answering at $url"
Log ("Service interruption (origin stopped -> target answering): {0:N0} s ({1:mm\:ss})" -f $elapsed.TotalSeconds, $elapsed)
