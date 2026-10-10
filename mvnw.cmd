@echo off
setlocal

set "CACHED_MVN=C:\Users\nares\.m2\wrapper\dists\apache-maven-3.10.0\2aaba761688b1a6ea5a46e7e5f4d5406ed7a75ee8da95e5e3079863a23461c9b\bin\mvn.cmd"

if exist "%CACHED_MVN%" (
    call "%CACHED_MVN%" %*
) else (
    mvn %*
)
