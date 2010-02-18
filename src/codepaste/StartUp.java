package codepaste;

import org.eclipse.ui.IStartup;

public class StartUp implements IStartup {

	private WebObserver observer;
	private Thread thread;

	@Override
	public void earlyStartup() {
		if (this.observer == null) {
			this.observer = new WebObserver();
			this.thread = new Thread(this.observer);
			this.thread.start();
		}

	}

}
