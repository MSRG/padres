package ca.utoronto.msrg.padres.tools.webclient.services;

import simple.http.Request;
import simple.http.Response;
import simple.http.load.LoaderEngine;
import simple.http.load.Service;
import simple.http.serve.Context;
import simple.http.serve.FileEngine;

public class FileService extends Service {

	private FileEngine engine;

	public FileService(Context context) {
		super(context);
	}

	public void prepare(LoaderEngine loader, Object data) {
		engine = new FileEngine(context);
	}

	public void process(Request req, Response resp) throws Exception {
		if (engine == null)
			engine = new FileEngine(context);

		try {
			engine.resolve(req.getURI()).handle(req, resp);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
	}

}
