package OSVOMD_COMP;

public class Constants
{
    /*cz�� okna przeznaczona na podpis*/
    public static final double SIZE = 0.8f;

    /*nazwa publicznego folderu aplikacji*/
    public final static String EX_PUB_DIR_PATH = "/OSVOMD";

    /*stopie� kompresji obraz�w PNG podpisu 0 (MAX compression) - 100 (MIN compression)*/
    public final static int COMPRESS = 0;

    /*Crypto*/
    public final static int CRYPTO_ITERATIONS = 1000;
    public final static int CRYPTO_KEY_LENGTH = 256;

    public final static String APK_CONSTANT = "asdkl;";

    /*pattern*/
    public final static int MAX_TEMPLATE_ITERATIONS = 10;

    /*compare*/
    public static double X_W = 0.8;
    public static double Y_W = 2.0;
    public static double P_W = 0.5;
    public static double T_W = 0.0000001;

    /*procentowa ignorowana r�znica czasu*/
    public static double SIGNATURE_TIME_LIMIT = 0.27;
    
    public static double SIGNATURE_TIME_WEIGHT = 0.5;
}