package OSVOMD_COMP;

/**
 * tryb pracy dla tworzenia wzorca hidden
 * ALL - tworzone wszystkie i wybierany najlepszy
 * AVERAGE - reparametryzowany podpis o œrednim czasie trwania ze wszystkich
 * MEDIAN - podpis, którego czas jest median¹ wszystkich przeznaczonych do stworzenia wzorca
 *
 */
public enum HMode
{
	ALL, BEST, AVERAGE, MEDIAN;
}
