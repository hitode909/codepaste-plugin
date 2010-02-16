package foo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;

public class WebObserver implements Runnable {

	
	private String command() {
		return "/Users/fkd/co/gist/292611/eval_in_firefox";
	}
	
	private String eval(String arg) {
		return this.exec(this.command() + " " + arg);
	}
	private String eval_with_retry(String arg, int retry) {
		return this.exec_with_retry(this.command() + " " + arg, retry);
	}

	private String exec(String command) {
		String result = "";
		try {
			Process process = Runtime.getRuntime().exec(command);
		    InputStream is = process.getInputStream();
		    BufferedReader br = new BufferedReader(new InputStreamReader(is));
		    String line;
		    while ((line = br.readLine()) != null) {
		    	result += line;
		    }
			process.waitFor();
			return result.replaceAll("\"", "");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	private String exec_with_retry(String command, int retry) {
		String result;
		for(int i=0; i<retry; i++) {
			result = this.exec(command);
			if (result != null && !result.isEmpty()) {
				return result;
			}
		}
		return "";
	};
	
	private boolean hasProject(String name) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject(name);
		return project.exists();
	}
	
	private IProject getOrCreateProject(String name) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject(name	);
		if (!project.exists()) {
		  try {
		    project.create(null);
		  } catch (CoreException e1) {
		    e1.printStackTrace();
		  }
		}
		return project;
	}
	
	@Override
	public void run() {
		while(true) {
			String arg = "var e=content.document.querySelector('meta[type=codepaste-command]'); e ? e.getAttribute('command') : false";
		    String got = this.eval_with_retry(arg, 3);
		    if (got.equals("download")) {
		    	System.out.println("download");
		    	this.download();
		    } else if (got.equals("upload")) {
		    	this.upload();
		    } else {
		    	System.out.println("not found -> "+ got);
		    }
		    this.exec("sleep 2");
		}
	}

	private void upload() {
		// TODO Auto-generated method stub
		
	}
	
	private String parseDisposition(String disposition) {
		Pattern p = Pattern.compile("attachment; ?filename=\"([^\"]+)\"");
		Matcher m = p.matcher(disposition);
		if (m.find()){
			return m.group(1);
		}
		return null;
	}
	
	private void download() {
		final String arg = "content.document.querySelector('link[rel=\"download\"]').href";
		String href = this.eval_with_retry(arg, 3);
		if (href.isEmpty()) return;
		this.eval_with_retry("content.wrappedJSObject.start()", 3);
		try {
			final URL url = new URL(href);
			final URLConnection connection = url.openConnection();
			final String fileName = this.parseDisposition(
					connection.getHeaderField("Content-Disposition"));
			final BufferedReader in = new BufferedReader(
                                 new InputStreamReader(
                                 url.openStream()));

			String line;
			while ((line = in.readLine()) != null) {
				System.out.println(line);
			}
	      
			in.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		this.eval_with_retry("content.wrappedJSObject.finish()", 3);
		return;
   }
}
