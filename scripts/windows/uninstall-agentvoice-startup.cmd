@echo off
reg delete "HKCU\Software\Microsoft\Windows\CurrentVersion\Run" /v AgentVoiceRelay /f
exit /b %ERRORLEVEL%
