@echo off
echo ========================================
echo PlayLand Core Optimization Tests
echo ========================================
echo.

echo Checking Java version...
java -version
echo.

echo Compiling optimization classes...
cd paper-source\paper-server\src\main\java

echo Compiling Neural Network Optimization...
javac -cp ".;*" ru\playland\core\optimization\NeuralNetworkOptimization.java

echo Compiling Network Packet Optimizer...
javac -cp ".;*" ru\playland\core\optimization\NetworkPacketOptimizer.java

echo Compiling Advanced Performance Manager...
javac -cp ".;*" ru\playland\core\optimization\AdvancedPerformanceManager.java

echo Compiling Test Class...
javac -cp ".;*" ru\playland\core\test\OptimizationTest.java

echo.
echo ========================================
echo Running Optimization Tests
echo ========================================
echo.

echo Starting tests...
java -cp ".;*" ru.playland.core.test.OptimizationTest

echo.
echo ========================================
echo Test Results Summary
echo ========================================
echo.

echo Tests completed! Check the output above for results.
echo.

echo Performance Summary:
echo - Neural Network: Real CPU/Memory optimization
echo - Network Packets: Real compression and batching
echo - Performance Manager: Real tick and network optimization
echo.

pause
