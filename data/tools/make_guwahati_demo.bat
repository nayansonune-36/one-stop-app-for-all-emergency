@echo off
REM ==============================================================================
REM S.E.N.T.I.N.E.L. - make_guwahati_demo.bat
REM Extracts detailed offline basemap tiles for Guwahati (bbox 91.55,26.05,91.95,26.30, maxzoom 15)
REM ==============================================================================

setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
set "ROOT_DIR=%SCRIPT_DIR%..\.."
set "OUTPUT_DIR=%ROOT_DIR%\android\app\src\main\assets\maps"

if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

where pmtiles >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo ERROR: 'pmtiles' CLI not found in PATH.
    echo Please download pmtiles.exe from: https://github.com/protomaps/go-pmtiles/releases
    echo and place it in your PATH or in this folder.
    exit /b 1
)

if "%BUILD_DATE%"=="" set "BUILD_DATE=20261108"
set "SOURCE_URL=https://build.protomaps.com/%BUILD_DATE%.pmtiles"
set "GUWAHATI_BBOX=91.55,26.05,91.95,26.30"
set "MAX_ZOOM=15"

echo === Extracting Guwahati Demo PMTiles ===
echo Source: %SOURCE_URL%
echo BBox:   %GUWAHATI_BBOX%
echo Zoom:   0 to %MAX_ZOOM%
echo Target: %OUTPUT_DIR%\guwahati.pmtiles

pmtiles extract "%SOURCE_URL%" "%OUTPUT_DIR%\guwahati.pmtiles" --bbox="%GUWAHATI_BBOX%" --maxzoom="%MAX_ZOOM%"
if %ERRORLEVEL% neq 0 (
    echo Error during PMTiles extract.
    exit /b %ERRORLEVEL%
)

echo Converting PMTiles to MBTiles...
pmtiles convert "%OUTPUT_DIR%\guwahati.pmtiles" "%OUTPUT_DIR%\guwahati.mbtiles"

echo === Done! ===
dir "%OUTPUT_DIR%\guwahati.*"
