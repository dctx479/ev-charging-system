$port = 8080
try {
    $conn = Get-NetTCPConnection -LocalPort $port -ErrorAction Stop
    $pid = $conn.OwningProcess
    Write-Host "Found process $pid on port $port"
    Stop-Process -Id $pid -Force
    Start-Sleep -Seconds 3
    Write-Host "Process $pid stopped successfully"
} catch {
    Write-Host "No process found on port $port"
}
