package OSVOMD_COMP;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static java.lang.Math.abs;
import static OSVOMD_COMP.Constants.SIGNATURE_TIME_LIMIT;
import static OSVOMD_COMP.Constants.SIGNATURE_TIME_WEIGHT;

public class Signature
{
    /* nazwa podpisu, zawieraj�ca ID oraz dok�adn� dat�*/
    String name;
    /* ID podpisu dla rozr�nienia wykonuj�cego*/
    private String ID = "defaultSigId";
    /*lista punkt�w podpisu (time, x, y, press)*/
    private List<Point> points;

    public Signature()
    {
        this.name = rename();
        this.points = new LinkedList<Point>();
    }

    public Signature (byte[] b)
    {
        this.name = rename();
        this.points = new LinkedList<Point>();

        getDataFromBytes(b);
    }

    /**konstruktor kopiuj�cy*/
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

    /**z listy podpis�w tworzy jeden wzorzec
     * IPPA algorithm
     * @param signatures enrollment signatures
     * @return templateSignature
     */
    static public Signature templateSignature(List<Signature> signatures, int maxInterations)
    {
    	if (signatures.isEmpty())
    	{
    		return null;
    	}
    	
        Signature template = new Signature();

        //kopia listy signatures
        List<Signature> hiddenSignatures = new LinkedList<Signature>();
        for (Signature s : signatures)
        {
            hiddenSignatures.add(new Signature(s));
        }

        //macierz sprawdzania warunku stopu, przechowuje informacje o poprzednich wynikach marszczenia
        double[][] prevScores = new double[signatures.size()][hiddenSignatures.size()];
        //w celu unikni�cia "oscylacji", sprawdzamy te� wyniki dwa kroki przed
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
                for (Signature sig : signatures) //dla ka�dej pary
                {
                    double[] score = new double[1];
                    inHiddenTime.add(sig.warpToTime(hid, score));

                    if (prevScores[sigIdx][hidIdx] != score[0] && prevPrevScores[sigIdx][hidIdx] != score[0]) stop = false; //s� r�ne, wi�c jeszcze nie koniec

                    prevPrevScores[sigIdx][hidIdx] = prevScores[sigIdx][hidIdx];
                    prevScores[sigIdx][hidIdx] = score[0];
                    ++sigIdx;
                }
                newHidden.add(averageSignature(inHiddenTime));
                ++hidIdx;
            }
            hiddenSignatures = newHidden;

            //nic si� ju� nie zmieni�o
            if (stop) break;
        }

        template = pickBestSignature(hiddenSignatures, signatures);
        return template;
    }

    /** z listy proponowanych wzorc�w, wybiera taki o najmniejszym najgorszym wyniku por�wnania ze zbiorem enrollemnt
     *
     * @param hiddenSignatures proponowane wzorce
     * @param entrollmentSignatures podpisy na podstawie kt�rych wybieramy
     * @return najlepszy podpis
     */
    private static Signature pickBestSignature(List<Signature> hiddenSignatures, List<Signature> entrollmentSignatures)
    {
        double[] worstScores = new double[hiddenSignatures.size()];

        int hidIdx = 0;
        for (Signature h : hiddenSignatures)
        {
            for (Signature e : entrollmentSignatures)
            {

                double newScore = new DTW<Point>(h.getPointArray(), e.getPointArray()).getWarpingDistance();
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
        //System.out.println("pdi.kkk." + "wybrano podpis o inx " + pickIdx + "kt�ry mia� najgorsz� warto�� jedynie " + bestScore);
        return hiddenSignatures.get(pickIdx);
    }

    /**z listy podpis�w ZMARSZCZONYCH DO TAKIEGO SAMEGO CZASU, wylicza �redni podpis
     *
     * @param inHiddenTime lista podpis�w zgodnie zmarszczonych
     * @return �redni podpis, null gdy r�ne czasy
     */
    private static Signature averageSignature(final LinkedList<Signature> inHiddenTime)
    {
        //spr czy r�wne d�ugosci podpis�w
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
        //dla ka�dego punktu
        for (int i=0; i<size; ++i)
        {
            long time = 0;
            double x = 0.0;
            double y = 0.0;
            double press = 0.0;

            //u�rednij ze wszystkich podpis�w
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

    /**przekszta�ca czas podpisu do czasu podanego w parametrze, zgodnie ze �cia�k� marszczenia
     *
     * @param timeSig podpis, do kt�rego czasu si�odnosimy
     * @param score zmienna, w kt�rej zapisywany jest wynik marszczenia
     * @return nowy zmarszczony podpis w czasie podpisu timeSig
     */
    private Signature warpToTime(final Signature timeSig, double[] score)
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

        //zapisz wynik �cie�ki do zmiennej w paramietrze
        score[0] = dtw.warpingDistance;

        //tw�rz list� punkt�w starego podpisu dla punkt�w czasowych nowego
        for (int[] i : dtw.warpingPath)
        {
            map.get(i[1]).add(i[0]);
        }

        //dla ka�dego nowego punktu czasowego oblicz (lub skopiuj) odpowiedni nowy (u�redniony) punkt
        for (int i=0; i<timeSig.points.size(); ++i)
        {
            long time = 0;
            double x = 0.0;
            double y = 0.0;
            double press = 0.0;

            //u�rednij wszystkie punkty
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

    /**por�wnuje dwa podpisy
     *
     * @param veryfied pierwszy podpis
     * @param template drugi podpis
     * @return comaprison value (more -> more different signatures)
     */
    static public double compare(Signature veryfied, Signature template)
    {
        DTW<Point> dtw = new DTW<>(veryfied.getPointArray(), template.getPointArray());

        double value = dtw.warpingDistance;

        long timeDif = abs(veryfied.getSignatureTime() - template.getSignatureTime()) / template.getSignatureTime();
        
        if(timeDif > SIGNATURE_TIME_LIMIT)
        {
            value += (timeDif-SIGNATURE_TIME_LIMIT)*SIGNATURE_TIME_WEIGHT;
        }
        
        //TODO ustalenie wyniku, warto�ci jakie wp�ywaj� na wynik por�wnania
        return value;
    }

    /**por�wnuje obecny podpis z podanym w parametrze
     *
     * @param other signature
     * @return omaprison value (more -> more different signatures)
     */
    public double compareTo(Signature other)
    {
        return Signature.compare(this, other);
    }

    /**dodaje punkt do podpisu*/
    public void addPoint(long time, double x, double y, double press)
    {
        points.add(new Point(time, x, y, press));
    }

    /*przekszta�ca surowy zbi�r punkt�w na znormalizowany*/
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

    /*usuwa punkty z zerowym naciskiem na pocz�tku i na ko�cu podpisu*/
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

    /*standaryzuje warto�ci X i Y od 0 do 1*/
    private void resize()
    {
        //znajd� min i max warto�ci punkt�w
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

    /*ustawia czas wzgl�dny, wzgl�dem pierwszego punktu*/
    private void reTime()
    {
        long firstPointTime = points.get(0).time;
        for (Point p : points)
        {
            p.time -= firstPointTime;
        }
    }

    /*standaryzuje warto�ci nacisku od 0 d 1
    * dla innych zestaw�w danych ni� przygotowane surowe na urz�dzeniu*/
    public void rePress()
    {
        //znajd� min i max warto�ci punkt�w
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

    /*zmienia nazw� podpisu (pole name) na zgodn� z aktualnym ID i dat�*/
    private String rename()
    {
        Date currentDate = Calendar.getInstance().getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd__HH_mm_ss");
        String stringDate = formatter.format(currentDate);
        stringDate = ID + stringDate;
        name = stringDate;
        return stringDate;
    }

    /*zeruje obecny podpis, uaktualnia dat� w nazwie*/
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

    /*zwraca podpis jako ci�g bajt�w*/
    public byte[] getSigBytes()
    {
        return this.getSigString().getBytes(StandardCharsets.UTF_8);
    }

    /*ustawia punkty podpisu na podstawie danych z tablicy bajt�w (zgodnie z getSigBytes*/
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

    /*wy�wietla w Log list� punkt�w podpisu*/
    public void print()
    {
        for (Point p : points)
        {
            System.out.println("pdi.signature." + p.toString());
        }
    }

    /**zwraca tablice Point[] punkt�w podpisu*/
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