@echo off
%SystemRoot%\System32\wsl.exe bash -lc "systemctl --user start openclaw-gateway.service; systemctl --user start agentvoice-relay.service"
exit /b %ERRORLEVEL%
