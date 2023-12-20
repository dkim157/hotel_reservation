# CSC365_Lab7

-- Team Members

Daniel Kim, Andreo Dela Cruz

-- Compilation/running instructions

1. Connect to cal poly servers
2. initialize shell variables (see shell init)
3. in command line, 
  a. javac InnReservations.java
  b. java InnReservations

-- Shell init:

export CLASSPATH=$CLASSPATH:mysql-connector-java-8.0.16.jar:.

export HP_JDBC_URL=jdbc:mysql://db.labthreesixfive.com/dkim157?autoReconnect=true\&useSSL=false

export HP_JDBC_USER=[calpoly_username]

export HP_JDBC_PW=wtr22_365-[emplID]

-- Known bugs/deficiencies
1. No constriaint in letting checkout come after check-in
2. Only alphanumeric characters allowed (so if a name/roomCode/etc. has a '-' or space it doesn't work)
