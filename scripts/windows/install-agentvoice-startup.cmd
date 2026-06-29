@echo off
setlocal
set "START_SCRIPT=%~dp0start-agentvoice-relay.cmd"

reg add "HKCU\Software\Microsoft\Windows\CurrentVersion\Run" /v AgentVoiceRelay /t REG_SZ /d "\"%START_SCRIPT%\"" /f
if errorlevel 1 (
  echo Failed to install AgentVoice Relay startup entry.
  exit /b 1
)

call "%START_SCRIPT%"
echo AgentVoice Relay startup entry installed and service start requested.
exit /b 0
