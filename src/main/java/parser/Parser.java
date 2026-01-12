package parser;

import java.awt.Desktop;
import java.awt.Font;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.graphper.api.FileType;
import org.graphper.api.Graphviz;
import org.graphper.draw.ExecuteException;

import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

import automaton.EDRTA;

public class Parser {

	public enum Export {
		PNG, SVG, UPPAAL, DOT
	}

	public static void exportTo(String route, EDRTA a, Export option) {

		switch (option) {
		case PNG:
			writeLayoutInFile(Paths.get(route.toString()), a.toDOTLayout(), FileType.PNG);
			break;
		case SVG:
			writeLayoutInFile(Paths.get(route.toString()), a.toDOTLayout(), FileType.SVG);
			break;
		case UPPAAL:
			new UPPAAL(route, a);
			break;
		default:
			throw new RuntimeException("Export option not defined");
		}

	}

	private static void writeLayoutInFile(Path path, Graphviz g, FileType ft) {
		try {
			if (path != null && Files.notExists(path.getParent())) {
				Files.createDirectories(path.getParent());
			}
			String filename = path.getFileName().toString() == null ? "automaton" : path.getFileName().toString();
			g.toFile(ft).save(path.getParent().toString(), filename);
		} catch (IOException | ExecuteException e) {
			e.printStackTrace(System.err);
		}
	}

	/**
	 * Method that displays the automata representation in a browser
	 * 
	 * @throws ExecuteException
	 */
	public static void show(EDRTA a) throws ExecuteException {
		var g = a.toDOTLayout();
		layoutInBrowser(g);
	}

	private static void layoutInBrowser(Graphviz g) {
		try {
			String svg = g.toSvgStr().replaceFirst("<\\?xml.*?\\?>", "");;

			// 2 Start local server
			HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

			// 3 Serve SVG
			server.createContext("/automaton.svg", exchange -> {
				byte[] bytes = svg.getBytes(StandardCharsets.UTF_8);
				exchange.getResponseHeaders().add("Content-Type", "image/svg+xml; charset=UTF-8");
				exchange.sendResponseHeaders(200, bytes.length);
				try (OutputStream os = exchange.getResponseBody()) {
					os.write(bytes);
				}
			});

			// 4 Serve HTML
			server.createContext("/", exchange -> {
/*				String html = """
						<!DOCTYPE html>
						<html>
						  <body style="margin:0; padding:0;">
						    <img src="/automaton.svg" style="max-width:100vw; height:auto;">
						  </body>
						</html>
						""";*/
				
	            String html = """
	                    <!DOCTYPE html>
	                    <html>
	                    <head>
	                      <style>
	                        body { margin:0; overflow:hidden; }
	                        svg { width:100vw; height:100vh; background:#fafafa; cursor:grab; }
	                        #controls {
	                          position:absolute;
	                          top:10px; left:10px;
	                          background:white;
	                          padding:5px;
	                          border-radius:5px;
	                          box-shadow:0 0 5px rgba(0,0,0,0.3);
	                          z-index:100;
	                        }
	                        #controls button { font-size:16px; margin:2px; }
	                      </style>
	                    </head>
	                    <body>
	                      <div id="controls">
	                        <button id="zoomIn">+</button>
	                        <button id="zoomOut">–</button>
	                      </div>
	                      %s
	                      <script>
	                        const svg = document.querySelector("svg");
	                        let viewBox = { x:0, y:0, w:1000, h:600 };
	                        const zoomFactor = 1.1;
	                        let isPanning=false;
	                        let start={};

	                        function updateViewBox() {
	                          svg.setAttribute("viewBox", `${viewBox.x} ${viewBox.y} ${viewBox.w} ${viewBox.h}`);
	                        }

	                        // Zoom buttons
	                        document.getElementById("zoomIn").addEventListener("click",()=>{ zoom(1/zoomFactor,0.5,0.5); });
	                        document.getElementById("zoomOut").addEventListener("click",()=>{ zoom(zoomFactor,0.5,0.5); });

	                        // Zoom
	                        svg.addEventListener("wheel", e=>{
	                          e.preventDefault();
	                          const dir = e.deltaY>0?1:-1;
	                          const factor = dir>0 ? zoomFactor : 1/zoomFactor;
	                          const mx = e.offsetX/svg.clientWidth;
	                          const my = e.offsetY/svg.clientHeight;
	                          zoom(factor,mx,my);
	                        },{ passive:false });

	                        function zoom(factor,mx,my){
	                          const newW=viewBox.w*factor;
	                          const newH=viewBox.h*factor;
	                          viewBox.x+=(viewBox.w-newW)*mx;
	                          viewBox.y+=(viewBox.h-newH)*my;
	                          viewBox.w=newW;
	                          viewBox.h=newH;
	                          updateViewBox();
	                        }

	                        // Pan
	                        svg.addEventListener("mousedown", e=>{ isPanning=true; start={x:e.clientX,y:e.clientY}; svg.style.cursor="grabbing"; });
	                        svg.addEventListener("mousemove", e=>{
	                          if(!isPanning) return;
	                          const dx=(e.clientX-start.x)*viewBox.w/svg.clientWidth;
	                          const dy=(e.clientY-start.y)*viewBox.h/svg.clientHeight;
	                          viewBox.x-=dx;
	                          viewBox.y-=dy;
	                          start={x:e.clientX,y:e.clientY};
	                          updateViewBox();
	                        });
	                        svg.addEventListener("mouseup",()=>{ isPanning=false; svg.style.cursor="grab"; });
	                        svg.addEventListener("mouseleave",()=>{ isPanning=false; svg.style.cursor="grab"; });

	                        // Relative resize
	                        window.addEventListener("resize",()=>{ updateViewBox(); });

	                        // Automatic server shutdown when closing the window
	                        window.addEventListener("beforeunload",()=>{ fetch("/shutdown"); });
	                      </script>
	                    </body>
	                    </html>
	                """.formatted(svg);


				byte[] bytes = html.getBytes();
				exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
				exchange.sendResponseHeaders(200, bytes.length);
				try (OutputStream os = exchange.getResponseBody()) {
					os.write(bytes);
				}
			});
			
			server.createContext("/shutdown", exchange -> {
	            String msg = "Server shutting down";
	            exchange.sendResponseHeaders(200, msg.length());
	            try (OutputStream os = exchange.getResponseBody()) {
	                os.write(msg.getBytes());
	            }
	            new Thread(() -> server.stop(0)).start();
	        });
			
			

			server.start();

			// 5 Start browser
			Desktop.getDesktop().browse(new URI("http://localhost:8080"));
			//////////////////////////////////////////////////////
			/*
			 * try { Files.write(file, html.getBytes());
			 * Desktop.getDesktop().browse(file.toUri()); } catch (IOException e) {
			 * e.printStackTrace(); }
			 */

		} catch (RuntimeException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecuteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
