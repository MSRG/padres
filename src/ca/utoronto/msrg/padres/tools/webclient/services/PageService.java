package ca.utoronto.msrg.padres.tools.webclient.services;

import java.io.PrintStream;

import simple.http.Request;
import simple.http.Response;
import simple.http.load.Service;
import simple.http.serve.Context;
import ca.utoronto.msrg.padres.client.ClientConfig;
import ca.utoronto.msrg.padres.tools.webclient.WebClient;

public class PageService extends Service {

	private static String defaultPage = "index.html";

	private String webDir;

	public PageService(Context context) {
		super(context);
	}

	public void prepare(WebClient client) {
		webDir = client.getWebDir();
		if (!webDir.endsWith(ClientConfig.DIR_SLASH))
			webDir += ClientConfig.DIR_SLASH;
	}

	public static String getDefaultPage() {
		return defaultPage;
	}

	public static void setDefaultPage(String page) {
		defaultPage = page;
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

	/*
	 * Poor's man version of templates
	 * 
	 * TODO: Look into using real templates
	 */
	private void buildPage(PrintStream out, String page) {
		// Currently, all pages are automatically fitted with the header (includes global
		// javascripts) and menus
		PageWriter.sendPage(out, "html_header.html", webDir);
		PageWriter.sendPage(out, "page_header.html", webDir);

		// the actual page itself
		PageWriter.sendPage(out, page, webDir);

		// Also include a footer on every page
		PageWriter.sendPage(out, "page_footer.html", webDir);
		PageWriter.sendPage(out, "html_footer.html", webDir);
	}
}
