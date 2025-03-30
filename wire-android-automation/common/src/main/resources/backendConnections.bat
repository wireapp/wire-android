@echo off
rem This file generates a JSON file out of all entries in the Test Automation vault of 1Password that are in the category "Server"

set VAULT=Test Automation
set JSONFILE=backendConnections.json

rem Please install 1Password CLI manually on windows: winget install 1password-cli
set BINARY=op

echo [ > %JSONFILE%

rem For each entry in the server category of this vault
for /f "tokens=1" %%A in ('%BINARY% item list --vault "%VAULT%" --categories Server') do (
    echo %%A | findstr /b "ID" >nul
    if not errorlevel 1 (
        rem Skip this iteration if %%A starts with "ID"
    ) ELSE (
        if defined FIRSTTIME (
            echo , >> %JSONFILE%
        )
        rem Get the entry details in JSON format
        for /f "delims=" %%B in ('%BINARY% item get --vault "%VAULT%" %%A --reveal --format=json') do (
            echo %%B >> %JSONFILE%
        )
        set FIRSTTIME=true
    )
)

echo ] >> %JSONFILE%
exit /b 0