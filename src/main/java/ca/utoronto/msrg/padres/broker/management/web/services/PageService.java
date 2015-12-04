package ca.utoronto.msrg.padres.broker.management.web.services;

import simple.http.serve.*;
import simple.http.load.*;
import simple.http.*;

import java.io.*;

import ca.utoronto.msrg.padres.broker.management.web.ManagementServer;
import ca.utoronto.msrg.padres.broker.management.web.PageWriter;

public class PageService extends Service {

	private static String defaultPage = "shell.html";
	
	private String webDir;

	public PageService(Context context) {
		super(context);
	}
	
	public void prepare(ManagementServer ms) throws Exception {
		webDir = ms.getWebDir();
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
		// PageWriter.sendPage(out, "header.html");
		// PageWriter.sendPage(out, "topmenu.html");
		// PageWriter.sendPage(out, "sidemenu.html");

		// the actual page itself
		PageWriter.sendPage(out, page, webDir);

		// Also include a footer on every page
		// PageWriter.sendPage(out, "footnote.html");
		// PageWriter.sendPage(out, "footer.html");
	}
}
