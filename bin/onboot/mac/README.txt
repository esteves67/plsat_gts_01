
----------------------------------------------------------------
MySQL:
  http://www.unumu.com/docs/oreilly/unix3/mac/ch02_02.htm

  Auto-starting on boot:
    % mkdir /Library/StartupItems/MySQLCOM
    % cp MySQL/MySQLCOM                 /Library/StartupItems/MySQLCOM/.
    % cp MySQL/StartupParameters.plist  /Library/StartupItems/MySQLCOM/.

  Manually Starting MySQL: (as root)
    /usr/local/mysql/support-files/mysql.server start
    ----
    /Library/StartupItems/MySQLCOM/MySQLCOM start
    /Library/StartupItems/MySQLCOM/MySQLCOM restart
    /Library/StartupItems/MySQLCOM/MySQLCOM stop
    
  Autostarting on boot:
    % mkdir /Library/StartupItems/MySQLCOM
    % cp MySQL/MySQLCOM                 /Library/StartupItems/MySQLCOM/.
    % cp MySQL/StartupParameters.plist  /Library/StartupItems/MySQLCOM/.
    % vi /etc/hostconfig
        MYSQLCOM=-YES-

----------------------------------------------------------------


