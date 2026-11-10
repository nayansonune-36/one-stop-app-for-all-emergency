@echo off
REM ==============================================================================
REM S.E.N.T.I.N.E.L. - make_base_india.bat
REM Extracts coarse lightweight offline basemap for India (bbox 68,6,98,37, maxzoom 7)
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
set "INDIA_BBOX=68,6,98,37"
set "MAX_ZOOM=7"

echo === Extracting India Base PMTiles ===
echo Source: %SOURCE_URL%
echo BBox:   %INDIA_BBOX%
echo Zoom:   0 to %MAX_ZOOM%
echo Target: %OUTPUT_DIR%\india_base.pmtiles

pmtiles extract "%SOURCE_URL%" "%OUTPUT_DIR%\india_base.pmtiles" --bbox="%INDIA_BBOX%" --maxzoom="%MAX_ZOOM%"
if %ERRORLEVEL% neq 0 (
    echo Error during PMTiles extract.
    exit /b %ERRORLEVEL%
)

echo Converting PMTiles to MBTiles...
pmtiles convert "%OUTPUT_DIR%\india_base.pmtiles" "%OUTPUT_DIR%\india_base.mbtiles"

echo === Done! ===
dir "%OUTPUT_DIR%\india_base.*"
