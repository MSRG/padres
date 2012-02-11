package ca.utoronto.msrg.padres.broker.webmonitor.services;

import simple.http.serve.*;
import simple.http.*;

import java.io.*;

import ca.utoronto.msrg.padres.broker.webmonitor.monitor.PageWriter;

public class PageService extends simple.http.load.Service {
	// FIXME: Make the default page configurable
	private static final String DEFAULT_PAGE = "loadlog.html";
	
	public PageService(Context context) {
		super(context);
	}

	public void process(Request req, Response resp) throws Exception {
		PrintStream out = resp.getPrintStream();

		resp.set("Content-Type", "text/html");
		
		String page = req.getPath().getName();
		if (page == null)
			page = DEFAULT_PAGE;
		
		buildPage(out, page);
		out.close();
	}

	private void buildPage(PrintStream out, String page) {
		// currently, all pages are automatically fitted with the header (includes javascripts) and menus
		PageWriter.sendPage(out, "header.html");
		PageWriter.sendPage(out, "topmenu.html");
		PageWriter.sendPage(out, "sidemenu.html");

		// the actual page itself
		PageWriter.sendPage(out, page);
		
		PageWriter.sendPage(out, "footnote.html");
		PageWriter.sendPage(out, "footer.html");
	}
}
