public final class Define {

    //Cada Port recebe apenas um tipo de Protocolo
    //Um tipo de Protocolo pode ser recebido por mais de um Port

    static int ripPort                     = 100;

    static int bootStraperPort             = 1000;

    static int bootClientPort              = 1001;

    static int simpPort                    = 2000;

    static int shrimpPort                  = 3000;

    static int serverPort                  = 4000;
    
    static int serverConnectionManagerPort = 4001;
    
    static int streamingPort               = 5000;

    static int idlePort                    = 6000;

    static int RPPort                      = 7000;

    static int RPServerAdderPort           = 7001;

    static int RPConectionManagerPort      = 7002;

    static int nodeConnectionManagerPort   = 8000;
    
    static int clientPort                  = 9000;

    //-------timeouts---------

    static int streamingTimeout  = 1000;
    
    static int bootClientTimeout = 1000;

    static int RPTimeout         = 1000; 

    static int RetransTimeout    = 1000; 

    static int idleTimeout       = 1000;

    //-------buffersize-------

    static int streamBuffer = 15000;
    static int infoBuffer   = 1024;

}
