package ca.utoronto.msrg.padres.demo.webclient.services;

import ca.utoronto.msrg.padres.demo.webclient.client.WebUIClient;
import simple.http.load.LoaderEngine;
import simple.http.load.Service;
import simple.http.serve.FileEngine;
import simple.http.serve.Context;
import simple.http.Response;
import simple.http.Request;

public class FileService extends Service {
	private FileEngine engine;

	public FileService(Context context) {
		super(context);
	}

	
	 public void prepare(LoaderEngine loader, Object data){
		 engine = new FileEngine(context);
	 }

	public void process(Request req, Response resp) throws Exception {
		if (engine == null)
			engine = new FileEngine(context);
		
		try {
			engine.resolve(redirectToWebDir(req.getURI())).handle(req, resp);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
   }

	// simple hack to redirect resource requests into the web directory
	// FIXME: figure out how to set the Context properly
	private String redirectToWebDir(String uri) {
		return System.getProperty("file.separator") + WebUIClient.getWebDir() + uri;
	}
}
