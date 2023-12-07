package SharedStructures;
public final class Define {

    //Cada Port recebe apenas um tipo de Protocolo
    //Um tipo de Protocolo pode ser recebido por mais de um Port

    public static int ripPort                       = 4500;

    public static int bootStraperPort               = 2100;

    public static int bootClientPort                = 2200;

    public static int simpPort                      = 2000;

    public static int shrimpPort                    = 2001;

    public static int linkPort                      = 3000;

    public static int serverPort                    = 4000;
    
    public static int serverConnectionManagerPort   = 4001;
    
    public static int streamingPort                 = 5000;

    public static int idlePort                      = 6000;

    public static int RPPort                        = 7000;

    public static int RPServerAdderPort             = 7001;

    public static int RPConectionManagerPort        = 7002;

    public static int nodeConnectionManagerPort     = 8000;
    
    public static int clientPort                    = 9000;

    //-------timeouts---------

    public static int streamingTimeout              = 2000;
    
    public static int bootClientTimeout             = 1000;

    public static int RPTimeout                     = 1000; 

    public static int RetransTimeout                = 2000; 

    public static int idleTimeout                   = 1000;

    public static int chooserThreadTimeOut          = 5000;

    //-------buffersize-------

    public static int streamBuffer                  = 15000;
    public static int infoBuffer                    = 1024;

    //-------Deltas-------

    public static double extraMetricsDelta           = 0.5;
    public static double mainDelta                   = 0.6;

    public static int maxLifePoints                  = 5;

}
