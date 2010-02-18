package foo;

import java.io.BufferedReader;
//import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
//import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageTwo;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;

public class WebObserver implements Runnable {

	
	private String command() {
		return "/Users/fkd/co/gist/292611/eval_in_firefox";
	}
	
	private String eval_with_retry(String arg, int retry) {
		System.out.println(arg);
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
		} catch (Exception e) {
			return null;
		}
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
	
	private IFile getFileInProject(String projectName, String fileName) {
		try {
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			IProject project = root.getProject(projectName);
			if (project.exists()) {
				project.open(null);
				IFile file = project.getFile(fileName);
				if (file.exists()) {
					return file;
				}
			}
		} catch(Exception e) {
		}
		return null;
	}
	
	private IJavaProject getOrCreateProject(String name) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject(name);
		IJavaProject javaProject = null;
		if (!project.exists()) {
		    try {
				project.create(null);
				project.open(null);
				IProjectDescription description = project.getDescription();
				String[] natures = description.getNatureIds();
				String[] newNatures = new String[natures.length + 1];
				System.arraycopy(natures, 0, newNatures, 0, natures.length);
				newNatures[natures.length] = JavaCore.NATURE_ID;
				description.setNatureIds(newNatures);
				project.setDescription(description, null);

				javaProject = JavaCore.create(project);

				Set<IClasspathEntry> entries = new HashSet<IClasspathEntry>();
				entries.addAll(Arrays.asList(javaProject.getRawClasspath()));

				String srcFolderName = "src";
				//ソースフォルダを作成
				IPath sourcePath = javaProject.getPath().append(srcFolderName);
				IFolder sourceDir = project.getFolder(new Path(srcFolderName));
				if (!sourceDir.exists()) {
					sourceDir.create(false, true, null);
				}
				String binFolderName = "bin";
				// 出力先フォルダを作成
				IPath outputPath = javaProject.getPath().append(binFolderName);
				IFolder outputDir = project.getFolder(new Path(binFolderName));
				if (!outputDir.exists()) {
					outputDir.create(false, true, null);
				}
				// ソースフォルダ、出力フォルダを設定
				IClasspathEntry srcEntry = JavaCore.newSourceEntry(sourcePath,
						new IPath[] {javaProject.getPath()}, outputPath);
				entries.add(srcEntry);

				entries.add(JavaRuntime.getDefaultJREContainerEntry());
				javaProject.setRawClasspath(entries.toArray(new IClasspathEntry[entries.size()]), null);
				
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
		}
		return javaProject;
	}


	@Override
	public void run() {
		while(true) {
		    String got = this.eval_with_retry("(content.document.querySelector('meta[name=codepaste-command]') || {content: false}).content", 3);
		    if (got.equals("download")) {
		    	System.out.println("download");
		    	this.download();
		    } else if (got.equals("upload")) {
		    	this.upload();
		    } else {
		    	System.out.println("not found -> "+ got);
		    }
		    this.exec("sleep 10");
		}
	}

	private void upload() {
		// TODO Auto-generated method stub
		this.eval_with_retry("content.wrappedJSObject.start()", 3);
		final String projectName = this.eval_with_retry("(content.document.querySelector('meta[name=\"project\"]') || {}).content", 3);
		final String fileName = this.eval_with_retry("(content.document.querySelector('meta[name=\"file\"]') || {}).content", 3);
		try {
			if (projectName.isEmpty() || fileName.isEmpty()) throw new Exception("Page seems broken.");
			IFile file = this.getFileInProject(projectName, fileName);
			if (file == null) throw new RuntimeException(projectName + "/" + fileName + " not found.");
			BufferedReader reader = new BufferedReader(new InputStreamReader(file.getContents()));
			StringBuffer buffer = new StringBuffer();
			String line;
			while ((line = reader.readLine()) != null) {
				buffer.append(line + "\n");
			}
			reader.close();
			String content = buffer.toString();
			String encoded_content = URLEncoder.encode(content, "UTF-8");
			this.eval_with_retry("content.wrappedJSObject.setBody('" + encoded_content + "')", 3);
			this.eval_with_retry("content.wrappedJSObject.succeed()", 3);
		} catch (Exception e) {
			this.reportError(e);
		}
		return;
		
	}
	
	private void reportError(Exception e) {
		String message;
		try {
			message = URLEncoder.encode(e.getMessage(), "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			message = e.getClass().toString();
		}
		this.eval_with_retry("content.wrappedJSObject.failed('" + message + "')", 3);
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
		this.eval_with_retry("content.wrappedJSObject.start()", 3);
		String href = this.eval_with_retry(
				"(content.document.querySelector('link[rel=\"download\"]') || {}).href", 3);
		final String projectName = this.eval_with_retry("(content.document.querySelector('meta[name=\"project\"]') || {}).content", 3);
		final Boolean allowOverwrite = (this.eval_with_retry("(content.document.querySelector('input#overwrite') || {}).checked", 3)).equals("true");
		try {
			if (href.isEmpty() || projectName.isEmpty()) throw new Exception("Page seems broken.");
			final URL url = new URL(href);
			final URLConnection connection = url.openConnection();
			final String fileName = this.parseDisposition(
					connection.getHeaderField("Content-Disposition"));
			IJavaProject jproject = this.getOrCreateProject(projectName);
			if (jproject == null) throw new RuntimeException("Failed to get project.");
			jproject.open(null);
			IFile file = jproject.getProject().getFile(fileName);
			if (file.exists() && allowOverwrite) {
				file.setContents(url.openStream(),IResource.FORCE, null);
			} else {
				file.create(url.openStream(),true, null);
			}
			this.eval_with_retry("content.wrappedJSObject.succeed('" + jproject.getProject().getName() + "')", 3);
		} catch (Exception e) {
			this.reportError(e);
		}
		return;
   }

}
