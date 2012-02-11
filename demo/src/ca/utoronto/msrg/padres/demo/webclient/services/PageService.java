package ca.utoronto.msrg.padres.demo.webclient.services;

import java.io.PrintStream;

import simple.http.Request;
import simple.http.Response;
import simple.http.load.Service;
import simple.http.serve.Context;
import ca.utoronto.msrg.padres.demo.webclient.client.PageWriter;

public class PageService extends Service {

	private static String defaultPage = "status.html";

	public PageService(Context context) {
		super(context);
	}

	public void process(Request req, Response resp) throws Exception {
		PrintStream out = resp.getPrintStream();

		resp.set("Content-Type", "text/html");

		String page = req.getPath().getName();
		if (page == null)
			page = defaultPage;

		buildPage(out, page);
		out.close();
	}

	public static String getDefaultPage() {
		return defaultPage;
	}

	public static void setDefaultPage(String page) {
		defaultPage = page;
	}

	/*
	 * Poor's man version of templates
	 * 
	 * TODO: Look into using real templates
	 */
	private void buildPage(PrintStream out, String page) {
		// Currently, all pages are automatically fitted with the header
		// (includes global javascripts) and menus
		PageWriter.sendPage(out, "header.html");
		PageWriter.sendPage(out, "topmenu.html");
		PageWriter.sendPage(out, "sidemenu.html");

		// the actual page itself
		PageWriter.sendPage(out, page);

		// Also include a footer on every page
		PageWriter.sendPage(out, "footnote.html");
		PageWriter.sendPage(out, "footer.html");
	}
}
