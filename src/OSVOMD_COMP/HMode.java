package OSVOMD_COMP;

/**
 * tryb pracy dla tworzenia wzorca hidden
 * ALL - tworzone wszystkie i wybierany najlepszy
 * AVERAGE - reparametryzowany podpis o �rednim czasie trwania ze wszystkich
 * MEDIAN - podpis, kt�rego czas jest median� wszystkich przeznaczonych do stworzenia wzorca
 *
 */
public enum HMode
{
	ALL, BEST, AVERAGE, MEDIAN;
}
