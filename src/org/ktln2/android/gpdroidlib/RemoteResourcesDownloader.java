package org.ktln2.android.gpdroidlib;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.net.HttpURLConnection;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.File;


/**
 * This class downloads a resource from remote server and then
 * notifies some registered observers.
 *
 * The resource is downloaded in the filesystem with the same name.
 *
 * EXAMPLE OF USAGE
 * <pre>
 * RemoteResourcesDownloader.getInstance().askForResource(this,
 * 	new URL("http://www.example.com/random/resource"),
 * 	new File("/data/com.example.android/cache/"));
 * </pre>
 *
 * TODO: make the request grouped by some parameter. (imagine that a bunch of
 * images need to be donwloaded in order to show sponsors on a map and that we want
 * to refresh the map only when all the images are retrieved).
 * TODO: POST requests handling.
 * TODO: Timeout of connection.
 * TODO: return something like an InputStream for situation with streaming data (imagine
 * the observer needs to parse a JSON without saving on the filesystem).
 */
public class RemoteResourcesDownloader {
	private static RemoteResourcesDownloader instance = null;

	private RemoteResourcesDownloader() {
	}

	public interface RemoteResourcesObserver {
		public void notifyEndDownload();
	}

	/**
	 * This variable contains the pair URL-observers.
	 */
	private HashMap<URL, List<RemoteResourcesObserver>> mObservers = new HashMap();

	/**
	 * This class only downloads the resource in a background thread.
	 */
	private class DownloadTask extends Thread {
		private URL mURL;
		private File mDestination;

		public DownloadTask(URL url, File destination) {
			mDestination = destination;
			mURL = url;
		}

		@Override
		public void run() {
			/**
			 * If some error happens what to do? is it possible to atomize the write of the file.
			 */
			try {
				HttpURLConnection conn = (HttpURLConnection)mURL.openConnection();

				InputStream is = conn.getInputStream();
				FileOutputStream f = new FileOutputStream(mDestination);

				byte[] buffer = new byte[1024];
				int len = 0;
				while ((len = is.read(buffer)) > 0) {
					f.write(buffer, 0, len);
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
			notifyObserver(mURL);
		}
	}

	private void notifyObserver(URL url) {
		List<RemoteResourcesObserver> observers = mObservers.get(url);

		for (RemoteResourcesObserver o : observers) {
			o.notifyEndDownload();
		}

		mObservers.remove(url);
	}

	synchronized public void askForResource(RemoteResourcesObserver object, URL url, File destination) {
		List<RemoteResourcesObserver> observers = new ArrayList();
		if (!mObservers.containsKey(url)) {
			new DownloadTask(url, destination).start();
		} else {
			observers = mObservers.get(url);
		}

		observers.add(object);
		mObservers.put(url, observers);
	}

	static public RemoteResourcesDownloader getInstance() {
		if (instance == null) {
			instance = new RemoteResourcesDownloader();
		}

		return instance;
	}
}
