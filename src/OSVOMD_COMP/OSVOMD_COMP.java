package OSVOMD_COMP;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;

public class OSVOMD_COMP
{
	public static void main(String[] args)
	{
		Signature sig1 = new Signature();
		
		
	}

	
    /* zapisuje obecny podpis do pliku*/
    private void writeSigToFile(String filename, Signature signature)
    {
        byte[] sigBytes = signature.getSigBytes();
        
        FileOutputStream fos = null;

        File mainDir = getFilePath();
        String filePath = mainDir.getAbsolutePath() + "/" + filename;
        try
		{
			fos = new FileOutputStream(filePath);
	        fos.write(sigBytes);
	        fos.close();
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


    }

    private File getFilePath()
	{
		File f = new File("COMP");
		return f;
	}


    /* czyta podpis z pliku*/
    private Signature readSigFromFile(String filename)
    {
    	FileInputStream fis = null;
    	byte[] b = null;

    	File mainDir = getFilePath();
    	String filePath = mainDir.getAbsolutePath() + "/" + filename;
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
    private Signature loadSUSigFile(String filename)
    {
        Signature newSig = new Signature();

        FileInputStream is;
        BufferedReader br;
        final File file = new File(getFilePath() + "/SUSig/" + filename);

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
	            System.out.println("pdi.loadSUSig" + "plik " + filename + " " + "nie istnieje");
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
            System.out.print("pdi.loadSUSig" + "plik " + filename + " " + "niepoprawny");
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
                System.out.print("pdi.loadSUSig" +"plik " + filename + " " + "niepoprawny");
                return null;
            }
        }
        newSig.setID(filename);
        newSig.normalize();
        newSig.rePress();

        return newSig;
    }

    /* wczytuje podpis w formacie SVC*/
    private Signature loadSVCFile(String filename)
    {
        Signature newSig = new Signature();

        FileInputStream is;
        BufferedReader br;
        File mainDir = getFilePath();
        final File file = new File(mainDir.getAbsolutePath() + "/" + filename);

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
        		System.out.print("pdi.loadSVC" + "plik " + filename + " " + "nie istnieje");
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
            System.out.print("pdi.loadSVC" + "plik " + filename + " " + "niepoprawny");
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
                System.out.print("pdi.loadSVC" +  "plik " + filename + " " + "niepoprawny");
                return null;
            }
        }
        newSig.setID(filename);
        newSig.normalize();
        newSig.rePress();
        return newSig;
    }
}
