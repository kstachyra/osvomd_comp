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
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class OSVOMD_COMP
{
	public static String ID;

	public static void main(String[] args)
	{
		Signature sig = new Signature();
		writeSigToFile(sig.name, sig);

		ERRsusig(20);
	}


	private static void ERRsusig(int noSigners)
	{
		ArrayList<LinkedList<Signature>> genuine = new ArrayList<>();
		ArrayList<LinkedList<Signature>> forgery = new ArrayList<>();
		ArrayList<LinkedList<Signature>> skilled = new ArrayList<>();

		genuine.add(0, new LinkedList<Signature>());
		for (int i=1; i<=noSigners; ++i)
		{
			genuine.add(i, new LinkedList<Signature>());
			for (int j=1; j<=10; ++j)
			{
				String filename = String.format("%03d", i) + "_1_" + j + ".sig";
				Signature s = loadSUSigFile(filename);
				if (s!= null) genuine.get(i).add(s);
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

		int ping = 0;
		List<Double> sameScores = new LinkedList<>();
		List<Double> otherScores = new LinkedList<>();
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
		writeToFile("other_"+getDate(), otherScores.toString());

		System.out.println("EER.sameScores " + sameScores.toString() );
		System.out.println("EER.otherScores " + otherScores.toString() );
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
