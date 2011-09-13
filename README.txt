This project runs performance tests on the Neo4j database. It stores old results, and fails if performance degrades.

To run performance tests:

  mvn clean compile antrun:run
  
Test output and a chart comparing the test just run to older tests can be found in tarpit/[current date and time].zip

The file performance.history stores performance history, and is used to tell if performance has degraded.

Setup Windows Environment
        To enable the Windows-JVM running in server-mode you have to

        o Download JDK from http://java.sun.com
        o After installation delete java.exe, javacpl.cpl, javaw.exe, javaws.exe  in the C:\WINDOWS\SYSTEM32 folder
        o Add JDK_HOME/bin into PATH environment variable

        http://www.wowzamedia.com/forums/showthread.php?1029-Windows-tuning-running-the-quot-server-quot-Java-VM-(tuning)
