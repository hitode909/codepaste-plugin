package foo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class WebObserver implements Runnable {

	@Override
	public void run() {
		while(true) {
		    String command = "/Users/fkd/co/gist/292611/eval_in_firefox 3";
		    Process process;
			try {
				process = Runtime.getRuntime().exec(command);
			    InputStream is = process.getInputStream();
			    BufferedReader br = new BufferedReader(new InputStreamReader(is));
			    String line;
			    while ((line = br.readLine()) != null) {
			    	System.out.println(line);
			    }
				process.waitFor();
				process = Runtime.getRuntime().exec("sleep 2");
				process.waitFor();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
