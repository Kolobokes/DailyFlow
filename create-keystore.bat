@echo off
echo ========================================
echo Создание keystore.jks для подписи релиза
echo ========================================
echo.

if exist keystore.jks (
    echo ВНИМАНИЕ: Файл keystore.jks уже существует!
    echo Если вы хотите создать новый, сначала удалите существующий файл.
    pause
    exit /b 1
)

echo Выполняется команда для создания keystore...
echo.
echo Параметры:
echo   - Имя файла: keystore.jks
echo   - Псевдоним ключа: dailyflow
echo   - Алгоритм: RSA
echo   - Размер ключа: 2048 бит
echo   - Срок действия: 10000 дней (~27 лет)
echo.
echo ВАЖНО: Запомните пароли, которые вы введете!
echo.

keytool -genkey -v -keystore keystore.jks -alias dailyflow -keyalg RSA -keysize 2048 -validity 10000

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo Keystore успешно создан!
    echo ========================================
    echo.
    echo Следующие шаги:
    echo 1. Создайте файл keystore.properties на основе keystore.properties.example
    echo 2. Заполните пароли в keystore.properties
    echo 3. НЕ коммитьте keystore.jks и keystore.properties в репозиторий!
    echo.
) else (
    echo.
    echo ========================================
    echo Ошибка при создании keystore!
    echo ========================================
    echo.
)

pause

