package foo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;

public class WebObserver implements Runnable {

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
			return result;
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
		IProject project = root.getProject(name	);
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
		    String got = this.exec_with_retry("/Users/fkd/co/gist/292611/eval_in_firefox 3", 3);
		    System.out.println(got);
		    this.dispatch(got);
		    this.exec("sleep 1");
		}
	}

	private void dispatch(String got) {
		// TODO Auto-generated method stub
		if (got == "download") {
			
		} else if (got == "upload") {

		}
	}
}
