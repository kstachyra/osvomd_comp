package OSVOMD_COMP;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javafx.util.Pair;

import static java.lang.Math.abs;
import static OSVOMD_COMP.Constants.SIGNATURE_TIME_LIMIT;
import static OSVOMD_COMP.Constants.SIGNATURE_TIME_WEIGHT;

public class Signature
{
    /* nazwa podpisu, zawieraj¹ca ID oraz dok³adn¹ datê*/
    String name;
    /* ID podpisu dla rozró¿nienia wykonuj¹cego*/
    private String ID = "defaultSigId";
    /*lista punktów podpisu (time, x, y, press)*/
    private List<Point> points;

    public Signature()
    {
        this.name = rename();
        this.points = new ArrayList<Point>();
    }

    public Signature (byte[] b)
    {
        this.name = rename();
        this.points = new ArrayList<Point>();

        getDataFromBytes(b);
    }

    /**konstruktor kopiuj¹cy*/
    public Signature (Signature other)
    {
        this.name = other.name;
        this.ID = other.ID;
        this.points = new LinkedList<Point>();

        for (Point p : other.points)
        {
            this.points.add(new Point(p.time, p.x, p.y, p.press));
        }
    }

    /**z listy podpisów tworzy jeden wzorzec
     * IPPA algorithm
     * @param signatures enrollment signatures
     * @param firstTemaplte first sginature for calculating
     * @return templateSignature
     */
    static public Signature templateSignature(List<Signature> signatures, Signature firstTemplate, int maxInterations)
    {
    	if (signatures.isEmpty())
    	{
    		return null;
    	}

    	Signature template = new Signature();

		List<Signature> hiddenSignatures = new LinkedList<Signature>();
    	if (firstTemplate == null)
    	{
    		//kopia listy signatures
    		for (Signature s : signatures)
    		{
    			hiddenSignatures.add(new Signature(s));
    		}
    	}
    	else //istnieje jeden podpis pocz¹tkowy
    	{
    		hiddenSignatures.add(firstTemplate);
    	}

    	//macierz sprawdzania warunku stopu, przechowuje informacje o poprzednich wynikach marszczenia
    	double[][] prevScores = new double[signatures.size()][hiddenSignatures.size()];
    	//w celu unikniêcia "oscylacji", sprawdzamy te¿ wyniki dwa kroki przed
    	double[][] prevPrevScores = new double[signatures.size()][hiddenSignatures.size()];
    	//warunek stopu
    	boolean stop = false;
        for (int i=0; i<maxInterations; ++i)
        {
            stop = true;
            LinkedList<Signature> newHidden = new LinkedList<Signature>();
            int hidIdx = 0;
            for (Signature hid : hiddenSignatures)
            {
                int sigIdx = 0;
                LinkedList<Signature> inHiddenTime = new LinkedList<Signature>();
                for (Signature sig : signatures) //dla ka¿dej pary
                {
                    double[] score = new double[1];
                    inHiddenTime.add(sig.warpToTime(hid, score));

                    if (prevScores[sigIdx][hidIdx] != score[0] && prevPrevScores[sigIdx][hidIdx] != score[0]) stop = false; //s¹ ró¿ne, wiêc jeszcze nie koniec

                    prevPrevScores[sigIdx][hidIdx] = prevScores[sigIdx][hidIdx];
                    prevScores[sigIdx][hidIdx] = score[0];
                    ++sigIdx;
                }
                newHidden.add(averageSignature(inHiddenTime));
                ++hidIdx;
            }
            hiddenSignatures = newHidden; //mo¿e byæ jeden tylko, w odpowiednich trybach

            //nic siê ju¿ nie zmieni³o
            if (stop) break;
        }

        template = pickBestSignature(hiddenSignatures, signatures);
        return template;
    }

    /** z listy proponowanych wzorców, wybiera taki o najmniejszym najgorszym wyniku porównania ze zbiorem enrollemnt
     *
     * @param hiddenSignatures proponowane wzorce
     * @param entrollmentSignatures podpisy na podstawie których wybieramy
     * @return najlepszy podpis
     */
    public static Signature pickBestSignature(List<Signature> hiddenSignatures, List<Signature> entrollmentSignatures)
    {
    	//jeden to on jest najlepszy
    	if (hiddenSignatures.size() == 1) return hiddenSignatures.get(0);
    	
        double[] worstScores = new double[hiddenSignatures.size()];

        int hidIdx = 0;
        for (Signature h : hiddenSignatures)
        {
            for (Signature e : entrollmentSignatures)
            {

                double newScore = Signature.compare(e, h, false);
                if (newScore > worstScores[hidIdx]) worstScores[hidIdx] = newScore;
            }
            ++hidIdx;
        }

        int pickIdx = 0;
        double bestScore = Double.MAX_VALUE;
        for (int i=0; i<worstScores.length; ++i)
        {
            if (worstScores[i] < bestScore)
            {
                bestScore = worstScores[i];
                pickIdx = i;
            }
        }
        System.out.println("pdi.kkk." + "wybrano podpis o inx " + pickIdx + "który mia³ najgorsz¹ wartoœæ jedynie " + bestScore);
        return hiddenSignatures.get(pickIdx);
    }

    /**z listy podpisów ZMARSZCZONYCH DO TAKIEGO SAMEGO CZASU, wylicza œredni podpis
     *
     * @param inHiddenTime lista podpisów zgodnie zmarszczonych
     * @return œredni podpis, null gdy ró¿ne czasy
     */
    public static Signature averageSignature(final LinkedList<Signature> inHiddenTime)
    {
        //spr czy równe d³ugosci podpisów
        int size = inHiddenTime.getFirst().points.size();
        for (Signature s : inHiddenTime)
        {
            if (s.points.size() != size)
            {
            	System.out.println("pdi.signature.err" + "different signatures size!");
                return null;
            }
        }

        Signature newSig = new Signature();
        //dla ka¿dego punktu
        for (int i=0; i<size; ++i)
        {
            long time = 0;
            double x = 0.0;
            double y = 0.0;
            double press = 0.0;

            //uœrednij ze wszystkich podpisów
            for (Signature s : inHiddenTime)
            {
                time += s.points.get(i).time;
                x += s.points.get(i).x;
                y += s.points.get(i).y;
                press += s.points.get(i).press;
            }
            time = time / inHiddenTime.size();
            x = x / inHiddenTime.size();
            y = y / inHiddenTime.size();
            press = press / inHiddenTime.size();

            //dodaj obliczony punkt
            newSig.addPoint(time, x, y, press);
        }
        return newSig;
    }

    /**przekszta³ca czas podpisu do czasu podanego w parametrze, zgodnie ze œcia¿k¹ marszczenia
     *
     * @param timeSig podpis, do którego czasu siêodnosimy
     * @param score zmienna, w której zapisywany jest wynik marszczenia
     * @return nowy zmarszczony podpis w czasie podpisu timeSig
     */
    public Signature warpToTime(final Signature timeSig, double[] score)
    {
        Signature newSig = new Signature();

        //pusta lista
        ArrayList<LinkedList<Integer>> map = new ArrayList<>();
        for (int i=0; i<timeSig.points.size(); ++i)
        {
            map.add(new LinkedList<Integer>());
        }

        //oblicz DTW warping path
        DTW<Point> dtw = new DTW<>(this.getPointArray(), timeSig.getPointArray());

        //zapisz wynik œcie¿ki do zmiennej w paramietrze
        score[0] = dtw.warpingDistance;

        //twórz listê punktów starego podpisu dla punktów czasowych nowego
        for (int[] i : dtw.warpingPath)
        {
            map.get(i[1]).add(i[0]);
        }

        //dla ka¿dego nowego punktu czasowego oblicz (lub skopiuj) odpowiedni nowy (uœredniony) punkt
        for (int i=0; i<timeSig.points.size(); ++i)
        {
            long time = 0;
            double x = 0.0;
            double y = 0.0;
            double press = 0.0;

            //uœrednij wszystkie punkty
            for (int j=0; j<map.get(i).size(); ++j)
            {
                int index = map.get(i).get(j); //indeks marszczonego punktu

                time += this.points.get(index).time;
                x += this.points.get(index).x;
                y += this.points.get(index).y;
                press += this.points.get(index).press;
            }
            time = time / map.get(i).size();
            x = x / map.get(i).size();
            y = y / map.get(i).size();
            press = press / map.get(i).size();

            //dodaj obliczony punkt
            newSig.addPoint(time, x, y, press);
        }
        return newSig;
    }
    
    /** tworzy jeden pocz¹tkowy podpis, bêd¹cy œrednim podpisem, o œrednim czasie trwania*/
	public static Signature firstTemplateAverage(final LinkedList<Signature> signatures)
	{
		//wylicza œredni czas i reparametryzuje podpisy do tego czasu, potem uœrednia
		int averagePoints = 0;
		int noSigs = 0;
		for (Signature s : signatures)
		{
			averagePoints += s.points.size();
			++noSigs;
		}
		averagePoints = averagePoints / noSigs;
				
		//nowa lista zmienionych w czasie
		LinkedList<Signature> sameTimeSigs = new LinkedList<>();
		for (Signature s : signatures)
		{
			sameTimeSigs.add(s.changeNoPoints(averagePoints));
		}
		
		Signature firstTemplateAverage = Signature.averageSignature(sameTimeSigs);

		return firstTemplateAverage;
	}

	private Signature changeNoPoints(long targetNoPoints)
	{
		System.out.println("PRZED REPARAMETRYZACJA");
		this.print();
		
		//reparametryzuje kopiê podpisu do okreœlonej liczby punktów
		Signature newSig = new Signature();
		
		//indeksy punktów oryginalnego
		int prev = 0;
		int next = 0;
		
		long timeStep = this.getSignatureTime() / (targetNoPoints - 1);
		long newPointTime = 0;
		
		for (int i=0; i<targetNoPoints; ++i)
		{
			while (this.points.get(next).time <= newPointTime && next!=this.points.size()-1)
			{
				++next;
			}
			if (next != 0) prev = next -1;
			
			double prevDist = newPointTime - this.points.get(prev).time;
			double nextDist = this.points.get(next).time - newPointTime;
			double prop = prevDist / (prevDist + nextDist);
			
			Point prevP = this.points.get(prev);
			Point nextP = this.points.get(next);
			
			newSig.addPoint(newPointTime, prevP.x + prop*(nextP.x - prevP.x) , prevP.y + prop*(nextP.y - prevP.y),
					prevP.press + prop*(nextP.press - prevP.press));
		
			newPointTime += timeStep;
		}
		
		System.out.println("PO REPARAMETRYZACJA ORYGINALNY");
		this.print();
		
		System.out.println("PO REPARAMETRYZACJA NOWY");
		newSig.print();
		
		// TODO Auto-generated method stub
		return newSig;
	}

	/** tworzy jeden pocz¹tkowy podpis, bêd¹cy podpisem z puli o czasie trwania, który jest median¹ czaasów*/
	public static Signature firstTemplateMedian(LinkedList<Signature> signatures)
	{
		//dla wszystkich podpisów, wybiera ten o czasie bêd¹cym median¹ wszystkich czasów
		LinkedList<Pair<Integer, Integer>> pointsToIndex = new LinkedList<>();
		for (int i=0; i<signatures.size(); ++i)
		{
			Signature s = signatures.get(i);
			if (s != null)
			{
				pointsToIndex.add(new Pair<Integer, Integer>(s.points.size(), i));
			}
		}
		
		Collections.sort(pointsToIndex, new Comparator<Pair<Integer, Integer>>()
		{
			@Override
			public int compare(Pair<Integer, Integer> arg0, Pair<Integer, Integer> arg1)
			{
				if (arg0.getKey() > arg1.getKey())
				{
					return 1;
				}
				else if (arg0.getKey() < arg1.getKey())
				{
					return -1;
				}
				
				return 0;
			}
		});
		
		//do parzystych list dodajemy na koñcu cokolwiek
		if (pointsToIndex.size() %2 == 0)
		{
			pointsToIndex.add(new Pair<Integer, Integer>(0, -1));
		}
		
		//index podpisu mediany czasu
		Integer pickedIndex = pointsToIndex.get((pointsToIndex.size() - 1) / 2).getValue() - 1;
				
		Signature firstTemplateMedian = null;
		if (pickedIndex != -1)
		{
			firstTemplateMedian = new Signature(signatures.get(pickedIndex));
		}
		else
		{
			System.out.println("ERROR firstTemplateMedian!");
		}
		
		return firstTemplateMedian;
	}

    /**porównuje dwa podpisy
     *
     * @param veryfied pierwszy podpis
     * @param template drugi podpis
     * @return comaprison value (more -> more different signatures)
     */
    static public double compare(final Signature veryfied, final Signature template, boolean otherAproach)
    {
    	if (!otherAproach)
    	{
    		DTW<Point> dtw = new DTW<>(veryfied.getPointArray(), template.getPointArray());

    		double value = dtw.warpingDistance;

    		double timeDif = abs((double)veryfied.getSignatureTime() - (double)template.getSignatureTime()) / (double)template.getSignatureTime();

    		if(timeDif > SIGNATURE_TIME_LIMIT)
    		{
    			value += timeDif*SIGNATURE_TIME_WEIGHT;
    		}
    		return value;
    	}
    	else
    	{
    		double value = Double.MAX_VALUE;
    		
    		//TODO other Aproach
    		
    		return value;
    	}
    }

    /**porównuje obecny podpis z podanym w parametrze klasyczn¹ metod¹
     *
     * @param other signature
     * @return omaprison value (more -> more different signatures)
     */
    public double compareTo(Signature other)
    {
        return Signature.compare(this, other, false);
    }

    /**dodaje punkt do podpisu*/
    public void addPoint(long time, double x, double y, double press)
    {
        points.add(new Point(time, x, y, press));
    }
    
    /**dodaje punkt do podpisu*/
    public void addPoint(Point p)
    {
        points.add(p);
    }

    /*przekszta³ca surowy zbiór punktów na znormalizowany*/
    public void normalize()
    {
        try
        {
            if (this.points.size() > 0)
            {
                this.clearBeginEnd();
                this.resize();
                this.reTime();
                //System.out.println("pdi.signature" + "signature normalized");
            } else
            {
            	System.out.println("pdi.signature" + "can't normalize, !points.size > 0");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /*zwraca podpis jako pojedynczy string*/
    private String getSigString()
    {
        String sigString = "";
        for (Point p : points)
        {
            sigString = sigString.concat(p.toString()).concat(System.lineSeparator());
        }
        return sigString;
    }

    /*usuwa punkty z zerowym naciskiem na pocz¹tku i na koñcu podpisu*/
    private void clearBeginEnd()
    {
        while (points.size()>0 && points.get(0).press == 0.0)
        {
            points.remove(0);
            /*Iterator <Point> iter = points.listIterator();
            if (iter.hasNext()) iter.next();
            iter.remove();*/
        }
        while (points.size()>0 && points.get(points.size()-1).press == 0.0)
        {
            points.remove(points.size()-1);
        }
    }

    /*standaryzuje wartoœci X i Y od 0 do 1*/
    private void resize()
    {
        //znajdŸ min i max wartoœci punktów
        double minX = Double.MAX_VALUE;
        double maxX = 0;
        double minY = Double.MAX_VALUE;
        double maxY = 0;
        for (Point p : points)
        {
            if(p.x < minX) minX = p.x;
            if(p.x > maxX) maxX = p.x;
            if(p.y < minY) minY = p.y;
            if(p.y > maxY) maxY = p.y;
        }

        //znormalizuj od 0 do 1
        double rangeX = maxX - minX;
        double rangeY = maxY - minY;
        for (Point p : points)
        {
            p.x = (p.x - minX)/rangeX;
            p.y = (p.y - minY)/rangeY;
        }
    }

    /*ustawia czas wzglêdny, wzglêdem pierwszego punktu*/
    private void reTime()
    {
        long firstPointTime = points.get(0).time;
        for (Point p : points)
        {
            p.time -= firstPointTime;
        }
    }

    /*standaryzuje wartoœci nacisku od 0 d 1
    * dla innych zestawów danych ni¿ przygotowane surowe na urz¹dzeniu*/
    public void rePress()
    {
        //znajdŸ min i max wartoœci punktów
        double min = Double.MAX_VALUE;
        double max = 0;

        for (Point p : points)
        {
            if(p.press < min) min = p.press;
            if(p.press > max) max = p.press;
        }

        //znormalizuj od 0 do 1
        double range = max - min;
        for (Point p : points)
        {
            p.press = (p.press - min)/range;
        }
    }

    /*zmienia nazwê podpisu (pole name) na zgodn¹ z aktualnym ID i dat¹*/
    private String rename()
    {
        Date currentDate = Calendar.getInstance().getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd__HH_mm_ss");
        String stringDate = formatter.format(currentDate);
        stringDate = ID + stringDate;
        name = stringDate;
        return stringDate;
    }

    /*zeruje obecny podpis, uaktualnia datê w nazwie*/
    public void clear()
    {
        try
        {
            rename();
            points.clear();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /*zwraca podpis jako ci¹g bajtów*/
    public byte[] getSigBytes()
    {
        return this.getSigString().getBytes(StandardCharsets.UTF_8);
    }

    /*ustawia punkty podpisu na podstawie danych z tablicy bajtów (zgodnie z getSigBytes*/
    private void getDataFromBytes(byte[] b)
    {
        String sigString = new String(b, StandardCharsets.UTF_8);
        String[] lines = sigString.split(System.lineSeparator());
        for (String line : lines)
        {
            String[] values = line.split("\t");
            if (values.length == 4)
            {
                this.addPoint(Long.parseLong(values[0]), Double.parseDouble(values[1]), Double.parseDouble(values[2]), Double.parseDouble(values[3]));
            }
        }
    }

    /*wyœwietla w Log listê punktów podpisu*/
    public void print()
    {
        for (Point p : points)
        {
            System.out.println("pdi.signature.\t" + p.toString());
        }
    }

    /**zwraca tablice Point[] punktów podpisu*/
    public Point[] getPointArray()
    {
        return points.toArray(new Point[0]);
    }

    /**zwraca czas trwania podpisu*/
    public long getSignatureTime()
    {
        return points.get(points.size()-1).time;
    }

    public void setID(String id)
    {
        this.ID = id;
        rename();
    }
}