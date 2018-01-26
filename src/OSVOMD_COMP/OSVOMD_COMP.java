package OSVOMD_COMP;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class OSVOMD_COMP
{
	public static String ID;

	public static void main(String[] args)
	{
		//54 -> 35 userów!
		//25 -> 20 userów!
		int noSigners = 30;

		ArrayList<LinkedList<Signature>> genuine1 = new ArrayList<>();
		ArrayList<LinkedList<Signature>> genuine2 = new ArrayList<>();
		ArrayList<LinkedList<Signature>> forgery = new ArrayList<>();

		loadAllSUSig(genuine1, genuine2, forgery, noSigners);

		ArrayList<Signature> template = new ArrayList<Signature>();

		//usuwa te wykorzystane do tworzenia wzorca!!! wy³¹cznie liczby parzyste
		templateHidden(template, HMode.MEDIAN, genuine1, genuine2, 10,  10, noSigners);
		//templateAverage(template, genuine1, genuine2, 10, noSigners);
		//templateBest(template, genuine1, genuine2, 10, noSigners);

		writeToFile("EER_"+getDate(), ERR(genuine1, genuine2, forgery, template, noSigners));
		System.out.println("OK!");

		//generateScript();
	}



	private static void generateScript()
	{
		String turboString = "";
		for (Constants.X_W = 0.7; Constants.X_W <= 0.9; Constants.X_W += 0.1) //3
		{
			for (Constants.Y_W = 1.0; Constants.Y_W <= 2.0; Constants.Y_W += 0.2) //6
			{
				for (Constants.P_W = 0.3; Constants.P_W <= 0.6; Constants.P_W += 0.1) //4
				{
					turboString += "java -jar .\\OSVOMD_C.jar " + Constants.X_W + " " + Constants.Y_W + " " + Constants.P_W + " " + Constants.T_W + " && ";
				}
			}
		}
		turboString += "dir";

		writeToFile("SCRIPT", turboString);
		System.out.println(turboString);
	}

	/**
	 * tworzy wzorce z listy podpisów i weryfikuje ich wyniki z pozosta³ymi
	 * @param template lista do której s¹ zapisywane wzorce
	 * @param genuine1 zbiór podpisów1
	 * @param genuine2 zniór podpisów 2
	 * @param maxIterations maksymalna iteracja pry tworzeniu podpisu
	 * @param noTemplate liczba podpisów przeznaczona na zbiór tworz¹cy wzorzec
	 * @param noSigners liczba osób podpisuj¹cych siê, dla których wykonaywane s¹ obliczenia
	 */
	private static void templateHidden(ArrayList<Signature> template, HMode mode,
			final ArrayList<LinkedList<Signature>> genuine1, final ArrayList<LinkedList<Signature>> genuine2, 
			int maxIterations, int noTemplate, int noSigners)
	{
		if (noTemplate > 20)
		{
			System.out.println("templateSusig.zla liczba podpisów > wzorzec!");
			return;
		}

		noTemplate /= 2;

		template.clear();
		template.add(null);

		for (int i=1; i<=noSigners; ++i)
		{
			LinkedList<Signature> signatures = new LinkedList<>();

			if (!genuine1.get(i).isEmpty() && !genuine2.isEmpty())
			{
				for (int j=0; j<noTemplate; ++j)
				{
					signatures.add(genuine1.get(i).get(j));
					signatures.add(genuine2.get(i).get(j));

				}
			}

			//wyznaczanie pocz¹tkowego podpisu
			Signature firstTemplate = null;

			if (!signatures.isEmpty())
			{
				if (mode == HMode.ALL)
				{
					firstTemplate = null;
				}
				else if (mode == HMode.BEST)
				{
					firstTemplate = Signature.pickBestSignature(signatures, signatures);
				}
				else if (mode == HMode.AVERAGE)
				{
					firstTemplate = Signature.firstTemplateAverage(signatures);
				}
				else if (mode == HMode.MEDIAN)
				{
					firstTemplate = Signature.firstTemplateMedian(signatures);
				}
			}


			Signature newTemplate = Signature.templateSignature(signatures, firstTemplate, maxIterations);
			template.add(i, newTemplate);

			genuine1.get(i).removeAll(signatures);
			genuine2.get(i).removeAll(signatures);

			signatures.clear();

		}
	}

	private static void templateBest(ArrayList<Signature> template, final ArrayList<LinkedList<Signature>> genuine1,
			final ArrayList<LinkedList<Signature>> genuine2, int noTemplate, int noSigners)
	{
		if (noTemplate > 20)
		{
			System.out.println("templateSusig.zla liczba podpisów > wzorzec!");
			return;
		}

		noTemplate /= 2;

		template.clear();
		template.add(null);

		for (int i=1; i<=noSigners; ++i)
		{
			LinkedList<Signature> signatures = new LinkedList<>();

			Signature newTemplate = null;
			if (!genuine1.get(i).isEmpty() && !genuine2.isEmpty())
			{
				for (int j=0; j<noTemplate; ++j)
				{
					signatures.add(genuine1.get(i).get(j));
					signatures.add(genuine2.get(i).get(j));

				}

				newTemplate = Signature.pickBestSignature(signatures, signatures);
			}
			template.add(i, newTemplate);


			genuine1.get(i).removeAll(signatures);
			genuine2.get(i).removeAll(signatures);

			signatures.clear();
		}

	}

	private static void templateAverage(ArrayList<Signature> template, final ArrayList<LinkedList<Signature>> genuine1,
			final ArrayList<LinkedList<Signature>> genuine2, int noTemplate, int noSigners)
	{
		if (noTemplate > 20)
		{
			System.out.println("templateSusig.zla liczba podpisów > wzorzec!");
			return;
		}

		noTemplate /= 2;

		template.clear();
		template.add(null);

		for (int i=1; i<=noSigners; ++i)
		{
			LinkedList<Signature> signatures = new LinkedList<>();

			Signature newTemplate = null;
			if (!genuine1.get(i).isEmpty() && !genuine2.isEmpty())
			{
				for (int j=0; j<noTemplate; ++j)
				{
					signatures.add(genuine1.get(i).get(j));
					signatures.add(genuine2.get(i).get(j));

				}

				Signature bestTime = Signature.pickBestSignature(signatures, signatures);

				LinkedList<Signature> sameTimeList = new LinkedList<>();
				for (Signature s : signatures)
				{
					double[] scr = new double[1];
					sameTimeList.add(s.warpToTime(bestTime, scr));
				}

				newTemplate = Signature.averageSignature(sameTimeList);
			}
			template.add(i, newTemplate);

			genuine1.get(i).removeAll(signatures);
			genuine2.get(i).removeAll(signatures);

			signatures.clear();
		}	
	}

	private static String ERR(final ArrayList<LinkedList<Signature>> genuine1, final ArrayList<LinkedList<Signature>> genuine2,
			final ArrayList<LinkedList<Signature>> forgery,
			ArrayList<Signature> template, final int noSigners)
	{
		String toRet = "";
		ArrayList<LinkedList<Signature>> genuine = new ArrayList<>();

		genuine.addAll(genuine1);
		for (int i=1; i<=noSigners; ++i)
		{
			genuine.get(i).addAll(genuine2.get(i));
		}

		int ping = 0;
		List<Double> sameScores = new LinkedList<>();
		List<Double> otherScores = new LinkedList<>();
		List<Double> forgeryScores = new LinkedList<>();

		List<Double> templateSameScores = new LinkedList<>();
		List<Double> templateOtherScores = new LinkedList<>();
		List<Double> templateForgeryScores = new LinkedList<>();


		for (int i=1; i<=noSigners; ++i)
		{
			for (int j=1; j<=noSigners; ++j)
			{
				System.out.println("pdi.kkk." + "PING " + ++ping + "/" + noSigners*noSigners);
				if (i==j)
				{
					//ta sama klasa
					for (Signature s1 : genuine.get(i))
					{
						for (Signature s2 : genuine.get(j))
						{
							sameScores.add(Signature.compare(s1, s2, false));
						}
					}
					// + template
					for (Signature s : genuine.get(i))
					{
						if (template.get(j) != null)
						{
							templateSameScores.add(Signature.compare(s, template.get(j), false));
						}
					}

					//forgery
					for (Signature s1 : genuine.get(i))
					{
						for (Signature forge : forgery.get(j))
						{
							forgeryScores.add(Signature.compare(forge, s1, false));
						}
					}
					// + template
					for (Signature f : forgery.get(i))
					{
						if (template.get(j) != null)
						{
							templateForgeryScores.add(Signature.compare(f, template.get(j), false));
						}
					}
				}
				else
				{
					//inna klasa
					for (Signature s1 : genuine.get(i))
					{
						for (Signature s2 : genuine.get(j))
						{
							otherScores.add(Signature.compare(s1, s2, false));
						}
					}
					//+ template
					for (Signature s : genuine.get(i))
					{
						if (template.get(j) != null)
						{
							templateOtherScores.add(Signature.compare(s, template.get(j), false));
						}
					}
				}
			}
		}

		writeToFile("same_"+getDate(), sameScores.toString().replace(", ", "\n").replace("[", "").replace("]", ""));
		writeToFile("other_"+getDate(), otherScores.toString().replace(", ", "\n").replace("[", "").replace("]", ""));
		writeToFile("forgery_"+getDate(), forgeryScores.toString().replace(", ", "\n").replace("[", "").replace("]", ""));

		writeToFile("templateForgery_"+getDate(), templateForgeryScores.toString().replace(", ", "\n").replace("[", "").replace("]", ""));
		writeToFile("templateSame_"+getDate(), templateSameScores.toString().replace(", ", "\n").replace("[", "").replace("]", ""));
		writeToFile("templateOther_"+getDate(), templateOtherScores.toString().replace(", ", "\n").replace("[", "").replace("]", ""));


		writeToFile("sameHist_"+getDate(), histogram(sameScores, 0.01).toString());
		writeToFile("otherHist_"+getDate(), histogram(otherScores, 0.01).toString());
		writeToFile("forgeryHist_"+getDate(), histogram(forgeryScores, 0.01).toString());

		writeToFile("templateForgeryHist_"+getDate(), histogram(templateForgeryScores, 0.01).toString());
		writeToFile("templateSameHist_"+getDate(), histogram(templateSameScores, 0.01).toString());
		writeToFile("templateOtherHist_"+getDate(), histogram(templateOtherScores, 0.01).toString());

		toRet += ("GEN ");
		toRet += (stringEER(sameScores, otherScores)+" ");
		toRet += ("TEMPLATE ");
		toRet += (stringEER(templateSameScores, templateOtherScores)+" ");
		toRet += ("TEMPLATE_FORGE ");
		toRet += (stringEER(templateSameScores, templateForgeryScores)+" ");

		return toRet;
	}

	private static String stringEER(List<Double> sameScores, List<Double> otherScores)
	{
		String toRet = "";

		double FRR, FAR;
		double prevD = 0;
		double nextD = 0;
		for (double d = Collections.min(otherScores); d < Collections.max(sameScores); d += 0.0001)
		{
			FRR = 0;
			FAR = 0;
			for (Double ss : sameScores)
			{
				if (ss >= d) FRR += 1;
			}
			for (Double os : otherScores)
			{
				if (os < d) FAR += 1;
			}
			FRR /= sameScores.size();
			FAR /= otherScores.size();

			if (FAR-FRR >= 0)
			{
				nextD = d;
				break;
			}

			prevD = d;

		}

		toRet += ("nextD " + String.valueOf(nextD) +" ");	
		toRet +=("prevD " + String.valueOf(prevD) +" ");

		FRR = 0;
		FAR = 0;
		for (Double ss : sameScores)
		{
			if (ss >= prevD) FRR += 1;
		}
		for (Double os : otherScores)
		{
			if (os < prevD) FAR += 1;
		}
		FRR /= sameScores.size();
		FAR /= otherScores.size();
		toRet +=("prevD_FRR " + String.valueOf(FRR*100) +" ");
		toRet +=("prevD_FAR " + String.valueOf(FAR*100) +" ");


		FRR = 0;
		FAR = 0;
		for (Double ss : sameScores)
		{
			if (ss >= nextD) FRR += 1;
		}
		for (Double os : otherScores)
		{
			if (os < nextD) FAR += 1;
		}
		FRR /= sameScores.size();
		FAR /= otherScores.size();
		toRet +=("nextD_FRR " + String.valueOf(FRR*100) +" ");
		toRet +=("nextD_FAR " + String.valueOf(FAR*100) +" ");

		return toRet;
	}

	/** tworzy histogram z listy wartoœci*/
	private static String histogram(final List<Double> scores, double step)
	{
		Double maxVal = Collections.max(scores);
		Double temp = (maxVal/step);
		Integer noBuckets = temp.intValue() +1;

		int[] hist = new int[noBuckets];

		for (Double s : scores)
		{
			Double d = s/step;
			hist[d.intValue()]++;
		}


		List<Integer> histList = new LinkedList<>();
		for (int i=0; i<noBuckets; ++i)
		{
			histList.add(hist[i]);
		}

		String result = histList.toString().replace("[", "").replace("]", "").replace(" ", "");
		return result;	
	}

	/** ³aduje okreœlon¹ liczbê podpisów do struktur*/
	private static void loadAllSUSig(ArrayList<LinkedList<Signature>> genuine1, ArrayList<LinkedList<Signature>> genuine2,
			ArrayList<LinkedList<Signature>> forgery, int noSigners)
	{
		genuine1.add(0, new LinkedList<Signature>());
		for (int i=1; i<=noSigners; ++i)
		{
			genuine1.add(i, new LinkedList<Signature>());
			for (int j=1; j<=10; ++j)
			{
				String filename = String.format("%03d", i) + "_1_" + j + ".sig";
				Signature s = loadSUSigFile(filename);
				if (s!= null) genuine1.get(i).add(s);
			}
		}

		genuine2.add(0, new LinkedList<Signature>());
		for (int i=1; i<=noSigners; ++i)
		{
			genuine2.add(i, new LinkedList<Signature>());
			for (int j=1; j<=10; ++j)
			{
				String filename = String.format("%03d", i) + "_2_" + j + ".sig";
				Signature s = loadSUSigFile(filename);
				if (s!= null) genuine2.get(i).add(s);
			}
		}

		forgery.add(0, new LinkedList<Signature>());
		for (int i=1; i<=noSigners; ++i)
		{
			forgery.add(i, new LinkedList<Signature>());
			for (int j=1; j<=10; ++j)
			{
				String filename = String.format("%03d", i) + "_f_" + j + ".sig";
				Signature s = loadSUSigFile(filename);
				if (s!= null) forgery.get(i).add(s);
			}
		}
	}


	/* zapisuje obecny podpis do pliku*/
	private static void writeSigToFile(String filename, Signature signature)
	{
		byte[] sigBytes = signature.getSigBytes();

		FileOutputStream fos = null;

		File mainDir = getFilePath();
		String filePath = mainDir.getAbsolutePath() + File.separator + filename;

		System.out.println(filePath);

		try
		{
			fos = new FileOutputStream(filePath);
			fos.write(sigBytes);
			fos.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}


	}

	private static File getFilePath()
	{
		File f = new File("STORAGE");
		return f;
	}


	/* czyta podpis z pliku*/
	private static Signature readSigFromFile(String filename)
	{
		FileInputStream fis = null;
		byte[] b = null;

		File mainDir = getFilePath();
		String filePath = mainDir.getAbsolutePath() + File.separator + filename;
		File file = new File(filePath);

		b = new byte[(int) file.length()];

		Signature newSig = new Signature();
		try
		{
			fis = new FileInputStream(file);
			fis.read(b);

			newSig = new Signature(b);

			fis.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}

		return newSig;
	}

	/* wczytuje podpis w formacie SUSig*/
	private static Signature loadSUSigFile(String filename)
	{
		Signature newSig = new Signature();

		FileInputStream is;
		BufferedReader br;
		File mainDir = getFilePath();
		final File file = new File(mainDir.getAbsolutePath()+ File.separator + "SUSig" + File.separator + filename);

		//wszystkie linie pliku
		LinkedList<String> lines = new LinkedList<String>();

		try
		{
			if (file.exists())
			{
				is = new FileInputStream(file);
				br = new BufferedReader(new InputStreamReader(is));
				String line = br.readLine();

				while(line != null)
				{
					lines.add(line);
					line = br.readLine();
				}
				br.close();
			}
			else
			{
				//System.out.println("pdi.loadSUSig." + "plik " + filename + " " + "nie istnieje");
				return null;
			}
		} catch (IOException e)
		{
			e.printStackTrace();
		}


		//usuwa dwie pierwsze linie
		if (lines.size()>2)
		{
			lines.remove(0);
			lines.remove(0);
		}
		else
		{
			System.out.println("pdi.loadSUSig." + "plik " + filename + " " + "niepoprawny");
			return null;
		}

		for (String line : lines)
		{
			String[] values = line.split(" ");
			if (values.length == 5)
			{
				newSig.addPoint(Long.parseLong(values[2]), Double.parseDouble(values[0]), Double.parseDouble(values[1]), Double.parseDouble(values[3]));
			}
			else
			{
				System.out.println("pdi.loadSUSig." +"plik " + filename + " " + "niepoprawny");
				return null;
			}
		}
		newSig.setID(filename);
		newSig.normalize();
		newSig.rePress();

		return newSig;
	}

	/* wczytuje podpis w formacie SVC*/
	private static Signature loadSVCFile(String filename)
	{
		Signature newSig = new Signature();

		FileInputStream is;
		BufferedReader br;
		File mainDir = getFilePath();
		final File file = new File(mainDir.getAbsolutePath() + File.separator + filename);

		//wszystkie linie pliku
		LinkedList<String> lines = new LinkedList<String>();

		try
		{
			if (file.exists())
			{
				is = new FileInputStream(file);
				br = new BufferedReader(new InputStreamReader(is));
				String line = br.readLine();

				while(line != null)
				{
					lines.add(line);
					line = br.readLine();
				}
				br.close();
			}
			else
			{
				System.out.println("pdi.loadSVC." + "plik " + filename + " " + "nie istnieje");
				return null;
			}
		} catch (IOException e)
		{
			e.printStackTrace();
		}


		//usuwa dwie pierwsze linie
		if (lines.size()>1)
		{
			lines.remove(0);
		}
		else
		{
			System.out.println("pdi.loadSVC." + "plik " + filename + " " + "niepoprawny");
			return null;
		}

		for (String line : lines)
		{
			String[] values = line.split(" ");
			if (values.length == 7)
			{
				newSig.addPoint(Long.parseLong(values[2]), Double.parseDouble(values[0]), Double.parseDouble(values[1]), Double.parseDouble(values[6]));
			}
			else
			{
				System.out.println("pdi.loadSVC." +  "plik " + filename + " " + "niepoprawny");
				return null;
			}
		}
		newSig.setID(filename);
		newSig.normalize();
		newSig.rePress();
		return newSig;
	}

	/* zapisuje String do pliku o podanej nazwie*/
	private static void writeToFile(String filename, String s)
	{
		try
		{
			byte[] bytes = s.getBytes();
			File mainDir = getFilePath();
			String filePath = mainDir.getAbsolutePath() + File.separator + filename;
			FileOutputStream fos = new FileOutputStream(filePath);

			fos.write(bytes);
			fos.close();
		} catch (Exception e)
		{
			e.printStackTrace();
		}



	}

	/* zwraca string z aktualn¹ dat¹*/
	private static String getDate()
	{
		Date currentDate = Calendar.getInstance().getTime();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd__HH_mm_ss");
		String stringDate = formatter.format(currentDate);
		return stringDate;
	}
}
