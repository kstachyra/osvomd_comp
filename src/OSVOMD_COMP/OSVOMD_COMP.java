package OSVOMD_COMP;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
		int noSigners = 4;
		
		ArrayList<LinkedList<Signature>> genuine1 = new ArrayList<>();
		ArrayList<LinkedList<Signature>> genuine2 = new ArrayList<>();
		ArrayList<LinkedList<Signature>> forgery = new ArrayList<>();
		ArrayList<LinkedList<Signature>> skilled = new ArrayList<>();
		
		loadAllSUSig(genuine1, genuine2, forgery, skilled, noSigners);
		
		ArrayList<Signature> template = new ArrayList<Signature>();
		ArrayList<LinkedList<Double>> templateScore = new ArrayList<>();
		//templateSusig(template, templateScore, genuine1, genuine2, 10,  10, 10, noSigners);
		
		/*for(int i=0; i<templateScore.size(); ++i)
		{
			System.out.print("PODPIS "+i+": ");
			if (templateScore.get(i)!=null)
			{
				for (Double s : templateScore.get(i))
				{
					System.out.print(s + " ");
				}
			}
			System.out.println("");
		}*/
		
		
		
		
		
		ERR(genuine1, genuine2, forgery, skilled, noSigners);
	}

	/**
	 * tworzy wzorce z listy podpisów i weryfikuje ich wyniki z pozosta³ymi
	 * @param template lista do której s¹ zapisywane wzorce
	 * @param templateScore lista do której s¹ zapisywane wyniki porównañ ze zbiorem weryfikacyjnym
	 * @param genuine1 zbiór podpisów1
	 * @param genuine2 zniór podpisów 2
	 * @param maxIterations maksymalna iteracja pry tworzeniu podpisu
	 * @param noTemplate liczba podpisów przeznaczona na zbiór tworz¹cy wzorzec
	 * @param noVerification liczba podpisów przeznaczona na zbiór weryfikacyjny
	 * @param noSigners liczba osób podpisuj¹cych siê, dla których wykonaywane s¹ obliczenia
	 */
	private static void templateSusig(ArrayList<Signature> template, ArrayList<LinkedList<Double>> templateScore,
			final ArrayList<LinkedList<Signature>> genuine1, final ArrayList<LinkedList<Signature>> genuine2, 
			int maxIterations, int noTemplate, int noVerification, int noSigners)
	{
		if (noTemplate + noVerification > 20)
		{
			System.out.println("templateSusig.zla liczba podpisów > wzorzec+weryfikacja!");
			return;
		}
		
		noTemplate /= 2;
		noVerification /= 2;
		
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
			Signature newTemplate = Signature.templateSignature(signatures, maxIterations);
			template.add(i, newTemplate);
			
			signatures.clear();
		}
		
		
		//wyniki dla zbioru weryfikacyjnego
		templateScore.clear();
		templateScore.add(null);
		
		for (int i = 1; i<=noSigners; ++i)
		{
			templateScore.add(i, new LinkedList<Double>());
			
			if (!genuine1.get(i).isEmpty() && !genuine2.isEmpty())
			{
				for (int j=noTemplate; j<noTemplate+noVerification; ++j)
				{
					Double newScore = Signature.compare(template.get(i), genuine1.get(i).get(j));
					templateScore.get(i).add(newScore);	
					newScore = Signature.compare(template.get(i), genuine2.get(i).get(j));
					templateScore.get(i).add(newScore);
				}
			}
		}		
	}

	
	private static void ERR(final ArrayList<LinkedList<Signature>> genuine1, final ArrayList<LinkedList<Signature>> genuine2,
			ArrayList<LinkedList<Signature>> forgery, ArrayList<LinkedList<Signature>> skilled,
			final int noSigners)
	{
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
		List<Double> skilledScores = new LinkedList<>();
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
							sameScores.add(Signature.compare(s1, s2));
						}
					}
					
					//forgery
					for (Signature s1 : genuine.get(i))
					{
						for (Signature forge : forgery.get(j))
						{
							forgeryScores.add(Signature.compare(s1, forge));
						}
					}
					//skilled
					for (Signature s1 : genuine.get(i))
					{
						for (Signature skill : skilled.get(j))
						{
							skilledScores.add(Signature.compare(s1, skill));
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
							otherScores.add(Signature.compare(s1, s2));
						}
					}
				}
			}
		}

		writeToFile("same_"+getDate(), sameScores.toString());
		System.out.println("EER.sameScores OK");
		writeToFile("other_"+getDate(), otherScores.toString());
		System.out.println("EER.otherScores OK");
		
		writeToFile("forgery_"+getDate(), forgeryScores.toString());
		System.out.println("EER.forgeryScores OK");
		writeToFile("skilled_"+getDate(), skilledScores.toString());
		System.out.println("EER.skilledScores OK");
		
		writeToFile("sameHist_"+getDate(), histogram(sameScores, 0.001).toString());
		writeToFile("otherHist_"+getDate(), histogram(otherScores, 0.001).toString());
		writeToFile("forgeryHist_"+getDate(), histogram(forgeryScores, 0.001).toString());
		writeToFile("skilledHist_"+getDate(), histogram(skilledScores, 0.001).toString());
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
		
		String result = histList.toString().replace("[", "");
		result = result.replace("]", "");
		result = result.replace(" ", "");
		return result;	
	}

	/** ³aduje okreœlon¹ liczbê podpisów do struktur*/
	private static void loadAllSUSig(ArrayList<LinkedList<Signature>> genuine1, ArrayList<LinkedList<Signature>> genuine2,
			ArrayList<LinkedList<Signature>> forgery, ArrayList<LinkedList<Signature>> skilled, int noSigners)
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
			for (int j=1; j<=5; ++j)
			{
				String filename = String.format("%03d", i) + "_f_" + j + ".sig";
				Signature s = loadSUSigFile(filename);
				if (s!= null) forgery.get(i).add(s);
			}
		}

		skilled.add(0, new LinkedList<Signature>());
		for (int i=1; i<=noSigners; ++i)
		{
			skilled.add(i, new LinkedList<Signature>());
			for (int j=6; j<=10; ++j)
			{
				String filename = String.format("%03d", i) + "_f_" + j + ".sig";
				Signature s = loadSUSigFile(filename);
				if (s!= null) skilled.get(i).add(s);
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
				System.out.println("pdi.loadSUSig." + "plik " + filename + " " + "nie istnieje");
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
