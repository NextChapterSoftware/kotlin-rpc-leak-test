\
    @ECHO OFF
    SET DIR=%~dp0
    SET CLASSPATH=%DIR%gradle\wrapper\gradle-wrapper.jar
    IF NOT EXIST "%CLASSPATH%" (
      ECHO gradle-wrapper.jar is missing. If you have Gradle installed, run: gradle wrapper --gradle-version 9.0.0
      EXIT /B 1
    )
    "%DIR%gradlew.bat" %*
